package com.company.project.service;

import com.company.project.dto.AccountSummaryView;
import com.company.project.dto.EmployeeView;
import com.company.project.dto.StatusHistoryView;
import com.company.project.entity.AccountStatus;
import com.company.project.entity.Employee;
import com.company.project.entity.LeaveReason;
import com.company.project.entity.Project;
import com.company.project.entity.ProjectTask;
import com.company.project.entity.RemovalReason;
import com.company.project.entity.Role;
import com.company.project.entity.TaskStatus;
import com.company.project.util.PasswordUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý tài khoản: tạo, sửa, duyệt/từ chối đăng ký, khóa/mở khóa, tạm dừng/kích hoạt lại,
 * xóa (mềm hoặc hẳn) và chuyển giao công việc.
 */
public class AccountService extends BaseService {

    public AccountService(SessionManager sessions) {
        super(sessions);
    }

    // ---------------------------------------------------------------- danh sách

    /** Admin thấy mọi tài khoản; quản lý chỉ thấy tài khoản nhân viên. */
    public List<AccountSummaryView> listAccounts(String token) {
        return read(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            List<Employee> accounts = actor.isAdmin()
                    ? ctx.employees().findAll()
                    : ctx.employees().findByRoles(List.of(Role.EMPLOYEE));
            List<AccountSummaryView> result = new ArrayList<>();
            for (Employee employee : accounts) {
                result.add(summary(ctx, employee));
            }
            return result;
        });
    }

    public List<EmployeeView> listPending(String token) {
        return read(ctx -> {
            requireManagerOrAdmin(ctx, token);
            return new ArrayList<>(ctx.employees().findByStatus(AccountStatus.PENDING).stream()
                    .map(EmployeeView::from).toList());
        });
    }

    /** Danh sách quản lý (và admin) đang hoạt động, dùng để chọn quản lý phụ trách dự án. */
    public List<EmployeeView> listManagers(String token) {
        return read(ctx -> {
            requireManagerOrAdmin(ctx, token);
            return new ArrayList<>(ctx.employees().findByRoles(List.of(Role.ADMIN, Role.MANAGER)).stream()
                    .filter(e -> e.getStatus() == AccountStatus.ACTIVE)
                    .map(EmployeeView::from).toList());
        });
    }

    /** Nhân viên đang hoạt động, có thể nhận việc / thêm vào dự án. */
    public List<EmployeeView> listAssignableEmployees(String token) {
        return read(ctx -> {
            requireManagerOrAdmin(ctx, token);
            return new ArrayList<>(ctx.employees().findByRoleAndStatus(Role.EMPLOYEE, AccountStatus.ACTIVE).stream()
                    .map(EmployeeView::from).toList());
        });
    }

    public List<StatusHistoryView> statusHistory(String token, long employeeId) {
        return read(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee target = findEmployee(ctx, employeeId);
            if (actor.isManager() && !target.isEmployee() && !target.getId().equals(actor.getId())) {
                throw new SecurityException("Quản lý chỉ xem được lịch sử của tài khoản nhân viên.");
            }
            return new ArrayList<>(ctx.employees().findStatusHistory(employeeId).stream()
                    .map(StatusHistoryView::from).toList());
        });
    }

    // ---------------------------------------------------------------- tạo / sửa

    /** Admin tạo được Quản lý và Nhân viên; Quản lý chỉ tạo được Nhân viên. Tài khoản hoạt động ngay. */
    public EmployeeView createAccount(String token, String username, String password, String fullName,
                                      String email, String phone, Role role) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Role newRole = role == null ? Role.EMPLOYEE : role;
            if (newRole == Role.ADMIN) {
                throw new SecurityException("Hệ thống chỉ có một tài khoản Admin, không thể tạo thêm.");
            }
            if (actor.isManager() && newRole != Role.EMPLOYEE) {
                throw new SecurityException("Quản lý chỉ được tạo tài khoản nhân viên.");
            }
            String cleanUsername = requireText(username, "Tên đăng nhập");
            requirePassword(password);
            if (ctx.employees().findByUsername(cleanUsername).isPresent()) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
            }
            Employee employee = new Employee();
            employee.setUsername(cleanUsername);
            employee.setPasswordHash(PasswordUtil.hash(password));
            employee.setFullName(requireText(fullName, "Họ tên"));
            employee.setEmail(email);
            employee.setPhone(phone);
            employee.setRole(newRole);
            employee.initStatus(AccountStatus.ACTIVE, "Tạo bởi " + actor.getRole().label().toLowerCase()
                    + " " + actor.getFullName(), actor);
            ctx.employees().save(employee);
            return EmployeeView.from(employee);
        });
    }

    /** Sửa thông tin; chỉ Admin được đổi vai trò (Nhân viên <-> Quản lý). */
    public EmployeeView updateAccount(String token, long employeeId, String fullName, String email,
                                      String phone, Role role) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee target = findEmployee(ctx, employeeId);
            requireCanManage(actor, target);
            if (target.getStatus() == AccountStatus.RESIGNED) {
                throw new IllegalArgumentException("Tài khoản đã nghỉ việc, không thể chỉnh sửa.");
            }
            target.setFullName(requireText(fullName, "Họ tên"));
            target.setEmail(email);
            target.setPhone(phone);
            if (role != null && role != target.getRole()) {
                changeRole(ctx, actor, target, role);
            }
            return EmployeeView.from(target);
        });
    }

    private void changeRole(DataContext ctx, Employee actor, Employee target, Role newRole) {
        if (!actor.isAdmin()) {
            throw new SecurityException("Chỉ Admin được thay đổi vai trò tài khoản.");
        }
        if (newRole == Role.ADMIN) {
            throw new SecurityException("Không thể nâng tài khoản lên Admin.");
        }
        if (newRole == Role.MANAGER) {
            long open = ctx.tasks().countOpenByAssignee(target.getId());
            if (open > 0) {
                throw new IllegalArgumentException("Nhân viên còn " + open
                        + " công việc chưa hoàn thành. Hãy chuyển giao trước khi nâng lên quản lý.");
            }
            // Quản lý không còn là thành viên thực hiện công việc trong dự án
            for (Project project : new ArrayList<>(target.getProjects())) {
                project.removeMember(target);
            }
        } else {
            int managed = target.getManagedProjects().size();
            if (managed > 0) {
                throw new IllegalArgumentException("Quản lý đang phụ trách " + managed
                        + " dự án. Hãy chuyển các dự án cho quản lý khác trước.");
            }
        }
        target.setRole(newRole);
    }

    // ---------------------------------------------------------------- duyệt / từ chối

    public EmployeeView approve(String token, long employeeId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee target = findEmployee(ctx, employeeId);
            requirePending(target);
            requireCanManage(actor, target);
            target.changeStatus(AccountStatus.ACTIVE, "Đăng ký được duyệt", null, LocalDate.now(), null, actor);
            return EmployeeView.from(target);
        });
    }

    /** Từ chối đăng ký: bản đăng ký bị xóa khỏi hệ thống. */
    public String reject(String token, long employeeId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee target = findEmployee(ctx, employeeId);
            requirePending(target);
            requireCanManage(actor, target);
            String name = target.getFullName();
            for (Project project : new ArrayList<>(target.getProjects())) {
                project.removeMember(target);
            }
            ctx.employees().delete(target);
            return name;
        });
    }

    private static void requirePending(Employee target) {
        if (target.getStatus() != AccountStatus.PENDING) {
            throw new IllegalArgumentException("Tài khoản không ở trạng thái chờ duyệt.");
        }
    }

    // ---------------------------------------------------------------- khóa / mở khóa

    public EmployeeView lock(String token, long employeeId, String reason) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee target = findEmployee(ctx, employeeId);
            requireCanManage(actor, target);
            if (target.getStatus() != AccountStatus.ACTIVE && target.getStatus() != AccountStatus.ON_LEAVE) {
                throw new IllegalArgumentException("Chỉ khóa được tài khoản đang hoạt động hoặc đang tạm dừng.");
            }
            target.changeStatus(AccountStatus.LOCKED, requireText(reason, "Lý do khóa"), null,
                    LocalDate.now(), null, actor);
            sessions.removeEmployee(target.getId());
            return EmployeeView.from(target);
        });
    }

    public EmployeeView unlock(String token, long employeeId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee target = findEmployee(ctx, employeeId);
            requireCanManage(actor, target);
            if (target.getStatus() != AccountStatus.LOCKED) {
                throw new IllegalArgumentException("Tài khoản không ở trạng thái bị khóa.");
            }
            target.changeStatus(AccountStatus.ACTIVE, "Mở khóa tài khoản", null, LocalDate.now(), null, actor);
            return EmployeeView.from(target);
        });
    }

    // ---------------------------------------------------------------- tạm dừng / kích hoạt lại

    /**
     * Tạm dừng tài khoản khi nghỉ dài ngày.
     * Công việc đang mở: chuyển giao cho nhân viên khác (transferToId) hoặc giữ lại và tạm hoãn.
     */
    public EmployeeView suspend(String token, long employeeId, LeaveReason reason, LocalDate from,
                                LocalDate until, String note, boolean transferTasks, Long transferToId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee target = findEmployee(ctx, employeeId);
            requireCanManage(actor, target);
            if (target.getStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Chỉ tạm dừng được tài khoản đang hoạt động.");
            }
            if (reason == null) {
                throw new IllegalArgumentException("Hãy chọn lý do tạm dừng.");
            }
            LocalDate start = from == null ? LocalDate.now() : from;
            if (until == null) {
                throw new IllegalArgumentException("Hãy nhập ngày dự kiến quay lại.");
            }
            requireDateOrder(start, until, "Ngày quay lại phải sau ngày bắt đầu nghỉ.");
            if (reason == LeaveReason.OTHER && (note == null || note.isBlank())) {
                throw new IllegalArgumentException("Lý do khác: hãy ghi rõ lý do trong phần ghi chú.");
            }

            List<ProjectTask> openTasks = ctx.tasks().findOpenByAssignee(target.getId());
            if (transferTasks) {
                if (transferToId == null) {
                    throw new IllegalArgumentException("Hãy chọn nhân viên nhận chuyển giao công việc.");
                }
                Employee receiver = findEmployee(ctx, transferToId);
                transferAll(actor, target, receiver, openTasks);
            } else {
                for (ProjectTask task : openTasks) {
                    if (task.getStatus() != TaskStatus.ON_HOLD) {
                        task.setStatus(TaskStatus.ON_HOLD);
                    }
                }
            }
            target.changeStatus(AccountStatus.ON_LEAVE, reason.label(), note, start, until, actor);
            sessions.removeEmployee(target.getId());
            return EmployeeView.from(target);
        });
    }

    /** Kích hoạt lại sau khi nghỉ: công việc đang tạm hoãn được tiếp tục. */
    public EmployeeView reactivate(String token, long employeeId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee target = findEmployee(ctx, employeeId);
            requireCanManage(actor, target);
            if (target.getStatus() != AccountStatus.ON_LEAVE) {
                throw new IllegalArgumentException("Tài khoản không ở trạng thái tạm dừng.");
            }
            for (ProjectTask task : ctx.tasks().findOpenByAssignee(target.getId())) {
                if (task.getStatus() == TaskStatus.ON_HOLD) {
                    task.setStatus(task.getProgress() > 0 ? TaskStatus.IN_PROGRESS : TaskStatus.TODO);
                }
            }
            target.changeStatus(AccountStatus.ACTIVE, "Quay lại làm việc", null, LocalDate.now(), null, actor);
            return EmployeeView.from(target);
        });
    }

    // ---------------------------------------------------------------- chuyển giao công việc

    /** Chuyển toàn bộ công việc đang mở của một nhân viên sang nhân viên khác. Trả về số việc đã chuyển. */
    public Integer transferOpenTasks(String token, long fromEmployeeId, long toEmployeeId) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee from = findEmployee(ctx, fromEmployeeId);
            requireCanManage(actor, from);
            Employee to = findEmployee(ctx, toEmployeeId);
            List<ProjectTask> openTasks = ctx.tasks().findOpenByAssignee(from.getId());
            if (openTasks.isEmpty()) {
                throw new IllegalArgumentException(from.getFullName() + " không còn công việc nào chưa hoàn thành.");
            }
            return transferAll(actor, from, to, openTasks);
        });
    }

    private int transferAll(Employee actor, Employee from, Employee to, List<ProjectTask> tasks) {
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Nhân viên nhận chuyển giao phải khác nhân viên hiện tại.");
        }
        requireAssignable(to);
        List<ProjectTask> blocked = tasks.stream().filter(t -> !hasProjectAccess(actor, t.getProject())).toList();
        if (!blocked.isEmpty()) {
            throw new SecurityException("Có " + blocked.size() + " công việc thuộc dự án bạn không phụ trách "
                    + "(vd: \"" + blocked.getFirst().getProject().getName() + "\"). Hãy nhờ quản lý dự án đó "
                    + "hoặc Admin chuyển giao.");
        }
        for (ProjectTask task : tasks) {
            moveTask(task, to);
        }
        return tasks.size();
    }

    /** Chuyển một công việc sang người khác, đồng bộ 2 phía quan hệ và thêm người nhận vào dự án. */
    static void moveTask(ProjectTask task, Employee to) {
        task.getAssignee().getAssignedTasks().remove(task);
        task.setAssignee(to);
        to.getAssignedTasks().add(task);
        if (!task.getProject().hasMember(to)) {
            task.getProject().addMember(to);
        }
        if (task.getStatus() == TaskStatus.ON_HOLD) {
            task.setStatus(task.getProgress() > 0 ? TaskStatus.IN_PROGRESS : TaskStatus.TODO);
        }
    }

    // ---------------------------------------------------------------- xóa

    /**
     * Xóa tài khoản theo lý do:
     *  - Nghỉ việc / hết hợp đồng / sa thải: xóa mềm (RESIGNED), phải không còn công việc đang mở.
     *  - Tạo nhầm / trùng: xóa hẳn, chỉ khi tài khoản chưa từng có công việc.
     */
    public String delete(String token, long employeeId, RemovalReason reason, String note) {
        return write(ctx -> {
            Employee actor = requireManagerOrAdmin(ctx, token);
            Employee target = findEmployee(ctx, employeeId);
            requireCanManage(actor, target);
            if (reason == null) {
                throw new IllegalArgumentException("Hãy chọn lý do xóa tài khoản.");
            }
            if (target.getStatus() == AccountStatus.RESIGNED) {
                throw new IllegalArgumentException("Tài khoản này đã được xóa (đã nghỉ việc) trước đó.");
            }
            if (target.getStatus() == AccountStatus.PENDING) {
                throw new IllegalArgumentException("Tài khoản đang chờ duyệt: hãy dùng chức năng \"Từ chối\".");
            }
            int managed = target.getManagedProjects().size();
            if (managed > 0) {
                throw new IllegalArgumentException("Tài khoản đang phụ trách " + managed
                        + " dự án. Hãy chuyển dự án cho quản lý khác trước khi xóa.");
            }
            String name = target.getFullName();
            if (reason.isSoftDelete()) {
                long open = ctx.tasks().countOpenByAssignee(target.getId());
                if (open > 0) {
                    throw new IllegalArgumentException("Còn " + open
                            + " công việc chưa hoàn thành. Hãy chuyển giao công việc trước khi xóa.");
                }
                for (Project project : new ArrayList<>(target.getProjects())) {
                    project.removeMember(target);
                }
                target.changeStatus(AccountStatus.RESIGNED, reason.label(), note, LocalDate.now(), null, actor);
                sessions.removeEmployee(target.getId());
                return name + " đã được chuyển sang trạng thái \"Đã nghỉ việc\" (" + reason.label() + ").";
            }
            long total = ctx.tasks().countByAssignee(target.getId());
            if (total > 0) {
                throw new IllegalArgumentException("Tài khoản đã có " + total + " công việc nên không thể xóa hẳn. "
                        + "Hãy chọn lý do Nghỉ việc / Hết hợp đồng / Bị sa thải để giữ lại lịch sử.");
            }
            for (Project project : new ArrayList<>(target.getProjects())) {
                project.removeMember(target);
            }
            ctx.tasks().clearAssigner(target.getId());
            ctx.employees().clearHistoryActor(target.getId());
            ctx.employees().delete(target);
            sessions.removeEmployee(employeeId);
            return "Đã xóa hẳn tài khoản " + name + " khỏi hệ thống.";
        });
    }

    // ---------------------------------------------------------------- tiện ích

    private static AccountSummaryView summary(DataContext ctx, Employee employee) {
        long open = ctx.tasks().countOpenByAssignee(employee.getId());
        long completed = ctx.tasks().countCompletedByAssignee(employee.getId());
        long total = ctx.tasks().countByAssignee(employee.getId());
        return new AccountSummaryView(EmployeeView.from(employee), open, completed, total,
                employee.getManagedProjects().size());
    }
}
