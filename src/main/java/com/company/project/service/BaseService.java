package com.company.project.service;

import com.company.project.entity.AccountStatus;
import com.company.project.entity.Employee;
import com.company.project.entity.Project;
import com.company.project.entity.ProjectTask;
import com.company.project.entity.Role;
import com.company.project.util.EntityManagerFactoryUtil;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * Lớp cha của các service: quản lý transaction và kiểm tra phân quyền.
 * Mọi kiểm tra quyền đều thực hiện ở server, client chỉ ẩn/hiện nút.
 */
public abstract class BaseService {
    protected final SessionManager sessions;

    protected BaseService(SessionManager sessions) {
        this.sessions = sessions;
    }

    /** Thực thi thao tác chỉ đọc. */
    protected <T> T read(Function<DataContext, T> work) {
        EntityManager em = EntityManagerFactoryUtil.getEntityManager();
        try {
            return work.apply(DataContext.of(em));
        } finally {
            em.close();
        }
    }

    /** Thực thi thao tác ghi trong một transaction; lỗi thì rollback. */
    protected <T> T write(Function<DataContext, T> work) {
        EntityManager em = EntityManagerFactoryUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            T result = work.apply(DataContext.of(em));
            em.getTransaction().commit();
            return result;
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    // ---------------------------------------------------------------- phân quyền

    protected Employee currentUser(DataContext ctx, String token) {
        Long id = sessions.employeeId(token);
        if (id == null) {
            throw new SessionExpiredException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }
        Employee employee = ctx.employees().findById(id).orElse(null);
        if (employee == null || employee.getStatus() != AccountStatus.ACTIVE) {
            sessions.remove(token);
            throw new SessionExpiredException(employee == null
                    ? "Tài khoản không còn tồn tại."
                    : statusMessage(employee));
        }
        return employee;
    }

    protected Employee requireAdmin(DataContext ctx, String token) {
        Employee actor = currentUser(ctx, token);
        if (!actor.isAdmin()) {
            throw new SecurityException("Chức năng chỉ dành cho quản trị viên (Admin).");
        }
        return actor;
    }

    protected Employee requireManagerOrAdmin(DataContext ctx, String token) {
        Employee actor = currentUser(ctx, token);
        if (actor.isEmployee()) {
            throw new SecurityException("Chức năng chỉ dành cho quản lý.");
        }
        return actor;
    }

    protected Employee requireEmployee(DataContext ctx, String token) {
        Employee actor = currentUser(ctx, token);
        if (!actor.isEmployee()) {
            throw new SecurityException("Chức năng chỉ dành cho nhân viên.");
        }
        return actor;
    }

    /**
     * Quy tắc quản lý tài khoản:
     *  - Không ai thao tác lên chính mình.
     *  - Admin quản lý được Quản lý và Nhân viên (không đụng tới Admin).
     *  - Quản lý chỉ quản lý được Nhân viên.
     */
    protected static boolean canManageAccount(Employee actor, Employee target) {
        if (actor.getId().equals(target.getId())) {
            return false;
        }
        if (actor.isAdmin()) {
            return !target.isAdmin();
        }
        return actor.isManager() && target.isEmployee();
    }

    protected static void requireCanManage(Employee actor, Employee target) {
        if (actor.getId().equals(target.getId())) {
            throw new SecurityException("Không thể thực hiện thao tác này trên chính tài khoản của bạn.");
        }
        if (!canManageAccount(actor, target)) {
            throw new SecurityException(actor.isManager()
                    ? "Quản lý chỉ được thao tác trên tài khoản nhân viên."
                    : "Bạn không có quyền thao tác trên tài khoản này.");
        }
    }

    /** Admin được thao tác mọi dự án; quản lý chỉ thao tác dự án mình phụ trách. */
    protected static void requireProjectAccess(Employee actor, Project project) {
        if (actor.isAdmin()) {
            return;
        }
        boolean owner = actor.isManager() && project.getManager() != null
                && project.getManager().getId().equals(actor.getId());
        if (!owner) {
            throw new SecurityException("Bạn không phụ trách dự án \"" + project.getName() + "\".");
        }
    }

    protected static boolean hasProjectAccess(Employee actor, Project project) {
        return actor.isAdmin() || (actor.isManager() && project.getManager() != null
                && project.getManager().getId().equals(actor.getId()));
    }

    protected static void requireProjectOpen(Project project) {
        if (project.getStatus().isClosed()) {
            throw new IllegalArgumentException("Dự án \"" + project.getName() + "\" ở trạng thái \""
                    + project.getStatus().label() + "\", công việc chỉ được xem.");
        }
    }

    // ---------------------------------------------------------------- tiện ích

    protected static Employee findEmployee(DataContext ctx, long id) {
        return ctx.employees().findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản #" + id + "."));
    }

    protected static Project findProject(DataContext ctx, long id) {
        return ctx.projects().findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dự án #" + id + "."));
    }

    protected static ProjectTask findTask(DataContext ctx, long id) {
        return ctx.tasks().findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công việc #" + id + "."));
    }

    /** Nhân viên được giao việc phải đang hoạt động và có vai trò nhân viên. */
    protected static void requireAssignable(Employee employee) {
        if (!employee.isEmployee()) {
            throw new IllegalArgumentException("Chỉ giao công việc cho tài khoản nhân viên.");
        }
        if (employee.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Nhân viên " + employee.getFullName() + " đang ở trạng thái \""
                    + employee.getStatus().label() + "\", không thể nhận công việc.");
        }
    }

    protected static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        return value.trim();
    }

    protected static void requireDateOrder(LocalDate start, LocalDate end, String message) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException(message);
        }
    }

    protected static void requirePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
    }

    /** Thông báo tương ứng từng trạng thái tài khoản khi không đăng nhập được. */
    protected static String statusMessage(Employee employee) {
        return switch (employee.getStatus()) {
            case PENDING -> "Tài khoản đang chờ quản lý duyệt.";
            case ON_LEAVE -> "Tài khoản đang tạm dừng"
                    + (employee.getStatusReason() == null ? "" : " (" + employee.getStatusReason() + ")")
                    + (employee.getStatusUntil() == null ? "." : " đến ngày " + employee.getStatusUntil() + ".");
            case LOCKED -> "Tài khoản đã bị khóa"
                    + (employee.getStatusReason() == null || employee.getStatusReason().isBlank()
                    ? "" : ": " + employee.getStatusReason())
                    + ". Vui lòng liên hệ quản lý.";
            case RESIGNED -> "Tài khoản không còn hoạt động (" + nullToEmpty(employee.getStatusReason()) + ").";
            case ACTIVE -> "Tài khoản đang hoạt động.";
        };
    }

    protected static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    protected static String roleText(Role role) {
        return role == null ? "" : role.label();
    }
}
