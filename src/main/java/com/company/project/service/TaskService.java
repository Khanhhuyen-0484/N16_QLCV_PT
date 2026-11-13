package com.company.project.service;

import com.company.project.dto.DashboardStats;
import com.company.project.dto.ProjectView;
import com.company.project.dto.TaskView;
import com.company.project.entity.AccountStatus;
import com.company.project.entity.Employee;
import com.company.project.entity.Project;
import com.company.project.entity.ProjectTask;
import com.company.project.entity.Role;
import com.company.project.entity.TaskPriority;
import com.company.project.entity.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Giao việc, cập nhật công việc và thống kê tổng quan. */
public class TaskService extends BaseService {

    public TaskService(SessionManager sessions) {
        super(sessions);
    }

    // ---------------------------------------------------------------- danh sách

    /** Admin: mọi công việc; quản lý: công việc trong dự án phụ trách; nhân viên: việc của mình. */
    public List<TaskView> list(String token) {
        return read(ctx -> toViews(scopeTasks(ctx, currentUser(ctx, token))));
    }

    public List<TaskView> myTasks(String token) {
        return read(ctx -> toViews(ctx.tasks().findByAssignee(currentUser(ctx, token).getId())));
    }

    // ---------------------------------------------------------------- quản lý thao tác

    public TaskView create(String token, long projectId, long assigneeId, String title, String description,
                           LocalDate startDate, LocalDate deadline, TaskPriority priority) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Project project = findProject(ctx, projectId);
            requireProjectAccess(actor, project);
            requireProjectOpen(project);
            Employee assignee = findEmployee(ctx, assigneeId);
            requireAssignable(assignee);
            if (!project.hasMember(assignee)) {
                throw new IllegalArgumentException(assignee.getFullName()
                        + " chưa là thành viên dự án. Hãy thêm vào dự án trước khi giao việc.");
            }
            requireDateOrder(startDate, deadline, "Deadline phải sau hoặc bằng ngày bắt đầu.");
            ProjectTask task = new ProjectTask();
            task.setTitle(requireText(title, "Tên công việc"));
            task.setDescription(description);
            task.setStartDate(startDate);
            task.setDeadline(deadline);
            task.setPriority(priority);
            task.setProgress(0);
            task.setStatus(TaskStatus.TODO);
            task.setProject(project);
            task.setAssignee(assignee);
            task.setAssigner(actor);
            project.getTasks().add(task);
            assignee.getAssignedTasks().add(task);
            ctx.tasks().save(task);
            return TaskView.from(task);
        });
    }

    public TaskView update(String token, long taskId, String title, String description, LocalDate startDate,
                           LocalDate deadline, TaskPriority priority) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            ProjectTask task = findTask(ctx, taskId);
            requireProjectAccess(actor, task.getProject());
            requireProjectOpen(task.getProject());
            requireDateOrder(startDate, deadline, "Deadline phải sau hoặc bằng ngày bắt đầu.");
            task.setTitle(requireText(title, "Tên công việc"));
            task.setDescription(description);
            task.setStartDate(startDate);
            task.setDeadline(deadline);
            task.setPriority(priority);
            return TaskView.from(task);
        });
    }

    public String delete(String token, long taskId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            ProjectTask task = findTask(ctx, taskId);
            requireProjectAccess(actor, task.getProject());
            String title = task.getTitle();
            ctx.tasks().delete(task);
            return title;
        });
    }

    /** Chuyển một công việc sang nhân viên khác (người nhận được thêm vào dự án nếu chưa có). */
    public TaskView transfer(String token, long taskId, long newAssigneeId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            ProjectTask task = findTask(ctx, taskId);
            requireProjectAccess(actor, task.getProject());
            requireProjectOpen(task.getProject());
            if (!task.getStatus().isOpen()) {
                throw new IllegalArgumentException("Chỉ chuyển giao được công việc chưa hoàn thành.");
            }
            Employee receiver = findEmployee(ctx, newAssigneeId);
            if (receiver.getId().equals(task.getAssignee().getId())) {
                throw new IllegalArgumentException("Công việc đang được giao cho chính nhân viên này.");
            }
            requireAssignable(receiver);
            AccountService.moveTask(task, receiver);
            return TaskView.from(task);
        });
    }

    /** Mở lại công việc đã hoàn thành (vd: kết quả chưa đạt). */
    public TaskView reopen(String token, long taskId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            ProjectTask task = findTask(ctx, taskId);
            requireProjectAccess(actor, task.getProject());
            requireProjectOpen(task.getProject());
            if (task.getStatus() != TaskStatus.COMPLETED) {
                throw new IllegalArgumentException("Chỉ mở lại được công việc đã hoàn thành.");
            }
            task.setStatus(task.getAssignee().getStatus() == AccountStatus.ON_LEAVE
                    ? TaskStatus.ON_HOLD : TaskStatus.IN_PROGRESS);
            task.setProgress(Math.min(task.getProgress(), 90));
            task.setCompletedAt(null);
            return TaskView.from(task);
        });
    }

    // ---------------------------------------------------------------- nhân viên thao tác

    public TaskView updateProgress(String token, long taskId, int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Tiến độ phải nằm trong khoảng 0 - 100.");
        }
        return write(ctx -> {
            ProjectTask task = ownTask(ctx, token, taskId);
            task.setProgress(progress);
            if (progress >= 100) {
                task.setStatus(TaskStatus.COMPLETED);
                task.setCompletedAt(LocalDateTime.now());
            } else {
                task.setStatus(progress == 0 && task.getStatus() == TaskStatus.TODO ? TaskStatus.TODO : TaskStatus.IN_PROGRESS);
                task.setCompletedAt(null);
            }
            return TaskView.from(task);
        });
    }

    public TaskView complete(String token, long taskId) {
        return updateProgress(token, taskId, 100);
    }

    public TaskView addNote(String token, long taskId, String note) {
        return write(ctx -> {
            ProjectTask task = ownTask(ctx, token, taskId);
            task.setNote(note);
            if (task.getStatus() == TaskStatus.TODO) {
                task.setStatus(TaskStatus.IN_PROGRESS);
            }
            return TaskView.from(task);
        });
    }

    public TaskView submitResult(String token, long taskId, String resultText, String resultLink, String note) {
        return write(ctx -> {
            ProjectTask task = ownTask(ctx, token, taskId);
            task.setResultText(requireText(resultText, "Nội dung kết quả"));
            task.setResultLink(resultLink);
            if (note != null && !note.isBlank()) {
                task.setNote(note);
            }
            task.setProgress(100);
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            return TaskView.from(task);
        });
    }

    /** Công việc của chính nhân viên, dự án chưa đóng và công việc không bị tạm hoãn/hủy. */
    private ProjectTask ownTask(DataContext ctx, String token, long taskId) {
        Employee actor = requireEmployee(ctx, token);
        ProjectTask task = findTask(ctx, taskId);
        if (!task.getAssignee().getId().equals(actor.getId())) {
            throw new SecurityException("Công việc này không được giao cho bạn.");
        }
        requireProjectOpen(task.getProject());
        if (task.getStatus() == TaskStatus.ON_HOLD) {
            throw new IllegalArgumentException("Công việc đang tạm hoãn, chưa thể cập nhật.");
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Công việc đã bị hủy.");
        }
        return task;
    }

    // ---------------------------------------------------------------- tổng quan

    public DashboardStats dashboard(String token) {
        return read(ctx -> {
            Employee actor = currentUser(ctx, token);
            List<ProjectTask> tasks = scopeTasks(ctx, actor);
            List<Project> projects = actor.isAdmin() ? ctx.projects().findAll()
                    : actor.isManager() ? ctx.projects().findByManager(actor.getId())
                    : ctx.projects().findByMember(actor.getId());

            long employees;
            if (actor.isAdmin()) {
                employees = ctx.employees().findByRoleAndStatus(Role.EMPLOYEE, AccountStatus.ACTIVE).size();
            } else {
                Set<Long> memberIds = new LinkedHashSet<>();
                projects.forEach(p -> p.getMembers().forEach(m -> memberIds.add(m.getId())));
                employees = memberIds.size();
            }
            long total = tasks.stream().filter(t -> t.getStatus() != TaskStatus.CANCELLED).count();
            long todo = count(tasks, TaskStatus.TODO);
            long inProgress = count(tasks, TaskStatus.IN_PROGRESS);
            long onHold = count(tasks, TaskStatus.ON_HOLD);
            long completed = count(tasks, TaskStatus.COMPLETED);
            long overdue = tasks.stream().map(TaskView::from).filter(TaskView::overdue).count();
            int progress = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
            List<TaskView> near = tasks.stream()
                    .filter(t -> t.getStatus().isOpen() && t.getDeadline() != null)
                    .sorted(Comparator.comparing(ProjectTask::getDeadline))
                    .limit(5)
                    .map(TaskView::from)
                    .toList();
            List<ProjectView> projectViews = projects.stream().map(ProjectView::from).toList();
            return new DashboardStats(projects.size(), employees, total, todo, inProgress, onHold, completed,
                    overdue, progress, near, projectViews);
        });
    }

    private static long count(List<ProjectTask> tasks, TaskStatus status) {
        return tasks.stream().filter(t -> t.getStatus() == status).count();
    }

    private static List<ProjectTask> scopeTasks(DataContext ctx, Employee actor) {
        if (actor.isAdmin()) {
            return ctx.tasks().findAll();
        }
        if (actor.isManager()) {
            return ctx.tasks().findByManager(actor.getId());
        }
        return ctx.tasks().findByAssignee(actor.getId());
    }

    private static List<TaskView> toViews(List<ProjectTask> tasks) {
        return new ArrayList<>(tasks.stream().map(TaskView::from).toList());
    }
}
