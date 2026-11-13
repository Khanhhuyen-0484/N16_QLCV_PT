package com.company.project.service;

import com.company.project.dto.ProjectView;
import com.company.project.entity.AccountStatus;
import com.company.project.entity.Employee;
import com.company.project.entity.Project;
import com.company.project.entity.ProjectStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý dự án. Admin thao tác mọi dự án; quản lý chỉ thao tác dự án mình phụ trách;
 * nhân viên chỉ xem các dự án mình là thành viên.
 */
public class ProjectService extends BaseService {

    public ProjectService(SessionManager sessions) {
        super(sessions);
    }

    public List<ProjectView> list(String token) {
        return read(ctx -> {
            Employee actor = currentUser(ctx, token);
            List<Project> projects;
            if (actor.isAdmin()) {
                projects = ctx.projects().findAll();
            } else if (actor.isManager()) {
                projects = ctx.projects().findByManager(actor.getId());
            } else {
                projects = ctx.projects().findByMember(actor.getId());
            }
            return new ArrayList<>(projects.stream().map(ProjectView::from).toList());
        });
    }

    public ProjectView create(String token, String name, String description, LocalDate startDate,
                              LocalDate deadline, Long managerId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            requireDateOrder(startDate, deadline, "Deadline dự án phải sau hoặc bằng ngày bắt đầu.");
            Project project = new Project();
            project.setName(requireText(name, "Tên dự án"));
            project.setDescription(description);
            project.setStartDate(startDate);
            project.setDeadline(deadline);
            project.setStatus(ProjectStatus.PLANNING);
            project.setManager(resolveManager(ctx, actor, managerId));
            project.getManager().getManagedProjects().add(project);
            ctx.projects().save(project);
            return ProjectView.from(project);
        });
    }

    /**
     * Sửa dự án. Chuyển sang "Đã hoàn thành" khi còn việc chưa xong phải có xác nhận (confirmIncomplete).
     * Chỉ Admin được đổi quản lý phụ trách.
     */
    public ProjectView update(String token, long projectId, String name, String description, LocalDate startDate,
                              LocalDate deadline, ProjectStatus status, Long managerId, boolean confirmIncomplete) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Project project = findProject(ctx, projectId);
            requireProjectAccess(actor, project);
            requireDateOrder(startDate, deadline, "Deadline dự án phải sau hoặc bằng ngày bắt đầu.");
            project.setName(requireText(name, "Tên dự án"));
            project.setDescription(description);
            project.setStartDate(startDate);
            project.setDeadline(deadline);
            if (status != null && status != project.getStatus()) {
                long open = project.getTasks().stream().filter(t -> t.getStatus().isOpen()).count();
                if (status == ProjectStatus.COMPLETED && open > 0 && !confirmIncomplete) {
                    throw new IllegalArgumentException("Dự án còn " + open
                            + " công việc chưa hoàn thành. Hãy xác nhận nếu vẫn muốn đánh dấu hoàn thành.");
                }
                project.setStatus(status);
            }
            if (managerId != null && (project.getManager() == null || !managerId.equals(project.getManager().getId()))) {
                if (!actor.isAdmin()) {
                    throw new SecurityException("Chỉ Admin được đổi quản lý phụ trách dự án.");
                }
                Employee newManager = resolveManager(ctx, actor, managerId);
                if (project.getManager() != null) {
                    project.getManager().getManagedProjects().remove(project);
                }
                project.setManager(newManager);
                newManager.getManagedProjects().add(project);
            }
            return ProjectView.from(project);
        });
    }

    /** Xóa dự án ở mọi trạng thái: công việc của dự án bị xóa theo (cascade). Trả về tên dự án. */
    public String delete(String token, long projectId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Project project = findProject(ctx, projectId);
            requireProjectAccess(actor, project);
            String name = project.getName();
            if (project.getManager() != null) {
                project.getManager().getManagedProjects().remove(project);
            }
            ctx.projects().delete(project);
            return name;
        });
    }

    public ProjectView addMember(String token, long projectId, long employeeId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Project project = findProject(ctx, projectId);
            requireProjectAccess(actor, project);
            Employee employee = findEmployee(ctx, employeeId);
            requireAssignable(employee);
            if (project.hasMember(employee)) {
                throw new IllegalArgumentException(employee.getFullName() + " đã là thành viên của dự án.");
            }
            project.addMember(employee);
            return ProjectView.from(project);
        });
    }

    /** Gỡ thành viên: chỉ khi người đó không còn công việc chưa hoàn thành trong dự án. */
    public ProjectView removeMember(String token, long projectId, long employeeId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Project project = findProject(ctx, projectId);
            requireProjectAccess(actor, project);
            Employee employee = findEmployee(ctx, employeeId);
            if (!project.hasMember(employee)) {
                throw new IllegalArgumentException(employee.getFullName() + " không phải thành viên của dự án.");
            }
            long open = ctx.tasks().countOpenByAssigneeInProject(employeeId, projectId);
            if (open > 0) {
                throw new IllegalArgumentException(employee.getFullName() + " còn " + open
                        + " công việc chưa hoàn thành trong dự án. Hãy chuyển giao trước khi gỡ.");
            }
            project.removeMember(employee);
            return ProjectView.from(project);
        });
    }

    private Employee resolveManager(DataContext ctx, Employee actor, Long managerId) {
        if (managerId == null || managerId.equals(actor.getId())) {
            return actor;
        }
        if (!actor.isAdmin()) {
            throw new SecurityException("Chỉ Admin được chọn quản lý phụ trách khác.");
        }
        Employee manager = findEmployee(ctx, managerId);
        if (manager.isEmployee()) {
            throw new IllegalArgumentException("Quản lý phụ trách phải là tài khoản quản lý.");
        }
        if (manager.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Quản lý " + manager.getFullName() + " không ở trạng thái hoạt động.");
        }
        return manager;
    }
}
