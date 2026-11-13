package com.company.project.service;

import com.company.project.entity.AccountStatus;
import com.company.project.entity.Employee;
import com.company.project.entity.LeaveReason;
import com.company.project.entity.Project;
import com.company.project.entity.ProjectStatus;
import com.company.project.entity.ProjectTask;
import com.company.project.entity.Role;
import com.company.project.entity.TaskPriority;
import com.company.project.entity.TaskStatus;
import com.company.project.util.EntityManagerFactoryUtil;
import com.company.project.util.PasswordUtil;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chạy khi server khởi động:
 *  1. Chuyển dữ liệu từ phiên bản cũ (cột enum của MariaDB -> varchar, DISABLED -> LOCKED, DONE -> COMPLETED,
 *     tài khoản admin -> vai trò ADMIN, dự án chưa có quản lý phụ trách).
 *  2. Tạo dữ liệu mẫu nếu CSDL còn trống.
 */
public class DataInitializer {
    private static final String DEFAULT_PASSWORD = "123456";

    public void initialize() {
        migrateLegacySchema();
        seed();
    }

    // ---------------------------------------------------------------- chuyển dữ liệu cũ

    private void migrateLegacySchema() {
        String[] statements = {
                "ALTER TABLE employees MODIFY role VARCHAR(20) NOT NULL",
                "ALTER TABLE employees MODIFY status VARCHAR(20) NOT NULL",
                "ALTER TABLE projects MODIFY status VARCHAR(20) NOT NULL",
                "ALTER TABLE project_tasks MODIFY status VARCHAR(30) NOT NULL",
                "ALTER TABLE project_tasks MODIFY priority VARCHAR(20)",
                "UPDATE employees SET status = 'LOCKED' WHERE status = 'DISABLED'",
                "UPDATE project_tasks SET status = 'COMPLETED' WHERE status = 'DONE'",
                "UPDATE employees SET role = 'ADMIN' WHERE username = 'admin' AND role <> 'ADMIN'",
                // Thành viên dự án chỉ là nhân viên đã được duyệt
                "DELETE pm FROM project_members pm JOIN employees e ON e.id = pm.employee_id "
                        + "WHERE e.role <> 'EMPLOYEE' OR e.status = 'PENDING'"
        };
        EntityManager em = EntityManagerFactoryUtil.getEntityManager();
        try {
            for (String sql : statements) {
                try {
                    em.getTransaction().begin();
                    em.createNativeQuery(sql).executeUpdate();
                    em.getTransaction().commit();
                } catch (RuntimeException e) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    System.out.println("[Migration] Bỏ qua: " + sql + " (" + e.getMessage() + ")");
                }
            }
        } finally {
            em.close();
        }
    }

    // ---------------------------------------------------------------- dữ liệu mẫu

    private void seed() {
        EntityManager em = EntityManagerFactoryUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            DataContext ctx = DataContext.of(em);
            if (ctx.employees().count() == 0) {
                seedDemoData(em);
            } else {
                ensureAdminAndManager(em, ctx);
            }
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /** CSDL cũ: đảm bảo có Admin, có ít nhất một quản lý và mọi dự án đều có quản lý phụ trách. */
    private void ensureAdminAndManager(EntityManager em, DataContext ctx) {
        Employee admin = ctx.employees().findByRoles(List.of(Role.ADMIN)).stream().findFirst().orElse(null);
        if (admin == null) {
            admin = account(em, "admin", "admin123", "Quản trị hệ thống", "admin@company.local", "0900000000",
                    Role.ADMIN, AccountStatus.ACTIVE);
        }
        Employee manager = ctx.employees().findByRoles(List.of(Role.MANAGER)).stream()
                .filter(e -> e.getStatus() == AccountStatus.ACTIVE).findFirst().orElse(null);
        if (manager == null && ctx.employees().findByUsername("quanly01").isEmpty()) {
            manager = account(em, "quanly01", DEFAULT_PASSWORD, "Trần Thị Lan", "lan.tran@company.local",
                    "0903000001", Role.MANAGER, AccountStatus.ACTIVE);
        }
        Employee owner = manager == null ? admin : manager;
        for (Project project : ctx.projects().findAll()) {
            if (project.getManager() == null) {
                project.setManager(owner);
            }
        }
    }

    private void seedDemoData(EntityManager em) {
        Employee admin = account(em, "admin", "admin123", "Quản trị hệ thống", "admin@company.local",
                "0900000000", Role.ADMIN, AccountStatus.ACTIVE);
        Employee lan = account(em, "quanly01", DEFAULT_PASSWORD, "Trần Thị Lan", "lan.tran@company.local",
                "0903000001", Role.MANAGER, AccountStatus.ACTIVE);
        Employee nam = account(em, "quanly02", DEFAULT_PASSWORD, "Lê Hoàng Nam", "nam.le@company.local",
                "0903000002", Role.MANAGER, AccountStatus.ACTIVE);
        Employee an = account(em, "nhanvien01", DEFAULT_PASSWORD, "Nguyễn Minh An", "an@company.local",
                "0901000001", Role.EMPLOYEE, AccountStatus.ACTIVE);
        Employee minh = account(em, "minhdev", DEFAULT_PASSWORD, "Nguyễn Văn Minh", "minh@company.local",
                "0912345678", Role.EMPLOYEE, AccountStatus.ACTIVE);
        Employee ha = account(em, "thuha", DEFAULT_PASSWORD, "Phạm Thu Hà", "ha.pham@company.local",
                "0934000003", Role.EMPLOYEE, AccountStatus.ACTIVE);
        account(em, "nhanvien_moi", DEFAULT_PASSWORD, "Nhân Viên Mới", "new.employee@company.local",
                "0987654321", Role.EMPLOYEE, AccountStatus.PENDING);

        Project internal = project(em, "Hệ thống quản lý công việc nội bộ",
                "Xây dựng ứng dụng client/server quản lý phân công công việc cho công ty.",
                LocalDate.now().minusDays(20), LocalDate.now().plusMonths(2), ProjectStatus.IN_PROGRESS, lan);
        internal.addMember(an);
        internal.addMember(minh);
        internal.addMember(ha);

        Project website = project(em, "Website giới thiệu công ty",
                "Thiết kế lại website giới thiệu sản phẩm và tuyển dụng.",
                LocalDate.now().plusDays(7), LocalDate.now().plusMonths(3), ProjectStatus.PLANNING, nam);
        website.addMember(minh);

        task(em, internal, lan, an, "Thiết kế giao diện Dashboard",
                "Hoàn thiện giao diện tổng quan dự án, thống kê và danh sách công việc gần deadline.",
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(3), 65,
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, "Đang hoàn thiện bố cục và màu sắc.");
        task(em, internal, lan, minh, "Xây dựng chức năng phân công công việc",
                "Tạo form giao việc, chọn nhân viên, deadline và mức độ ưu tiên.",
                LocalDate.now().minusDays(4), LocalDate.now().plusDays(7), 35,
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, "Đã nối socket client/server.");
        task(em, internal, lan, an, "Kiểm thử đăng nhập và phân quyền",
                "Kiểm tra luồng đăng nhập, đổi mật khẩu, duyệt tài khoản và khóa tài khoản.",
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), 100,
                TaskStatus.COMPLETED, TaskPriority.MEDIUM, "Đã hoàn thành kiểm thử chính.");
        task(em, internal, lan, minh, "Viết báo cáo tiến độ dự án",
                "Tổng hợp số lượng công việc, tiến độ và trạng thái quá hạn.",
                LocalDate.now().minusDays(7), LocalDate.now().minusDays(2), 20,
                TaskStatus.IN_PROGRESS, TaskPriority.URGENT, "Cần cập nhật phần công việc quá hạn.");
        task(em, internal, lan, an, "Chuẩn bị dữ liệu demo",
                "Tạo dữ liệu mẫu để máy khác clone project có thể chạy thử ngay.",
                LocalDate.now(), LocalDate.now().plusDays(10), 0,
                TaskStatus.TODO, TaskPriority.LOW, "");
        task(em, internal, lan, ha, "Viết tài liệu hướng dẫn sử dụng",
                "Soạn tài liệu hướng dẫn cho quản lý và nhân viên.",
                LocalDate.now().minusDays(12), LocalDate.now().plusDays(20), 40,
                TaskStatus.ON_HOLD, TaskPriority.MEDIUM, "Tạm hoãn trong thời gian nghỉ thai sản.");
        task(em, website, nam, minh, "Khảo sát yêu cầu website",
                "Thu thập yêu cầu từ phòng kinh doanh và nhân sự.",
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(14), 0,
                TaskStatus.TODO, TaskPriority.MEDIUM, "");

        // Nhân viên đang nghỉ thai sản: minh họa trạng thái Tạm dừng
        ha.changeStatus(AccountStatus.ON_LEAVE, LeaveReason.MATERNITY_LEAVE.label(), "Nghỉ thai sản 6 tháng",
                LocalDate.now().minusDays(10), LocalDate.now().plusMonths(6).minusDays(10), lan);
        System.out.println("[Seed] Đã tạo dữ liệu mẫu (admin: " + admin.getUsername() + ").");
    }

    private Employee account(EntityManager em, String username, String password, String fullName, String email,
                             String phone, Role role, AccountStatus status) {
        Employee employee = new Employee();
        employee.setUsername(username);
        employee.setPasswordHash(PasswordUtil.hash(password));
        employee.setFullName(fullName);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setRole(role);
        employee.initStatus(status, status == AccountStatus.PENDING ? "Nhân viên tự đăng ký, chờ duyệt"
                : "Dữ liệu mẫu", null);
        em.persist(employee);
        return employee;
    }

    private Project project(EntityManager em, String name, String description, LocalDate start, LocalDate deadline,
                            ProjectStatus status, Employee manager) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setStartDate(start);
        project.setDeadline(deadline);
        project.setStatus(status);
        project.setManager(manager);
        manager.getManagedProjects().add(project);
        em.persist(project);
        return project;
    }

    private void task(EntityManager em, Project project, Employee assigner, Employee assignee, String title,
                      String description, LocalDate start, LocalDate deadline, int progress, TaskStatus status,
                      TaskPriority priority, String note) {
        ProjectTask task = new ProjectTask();
        task.setProject(project);
        task.setAssigner(assigner);
        task.setAssignee(assignee);
        task.setTitle(title);
        task.setDescription(description);
        task.setStartDate(start);
        task.setDeadline(deadline);
        task.setProgress(progress);
        task.setStatus(status);
        task.setPriority(priority);
        task.setNote(note);
        if (status == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now().minusDays(1));
        }
        project.getTasks().add(task);
        assignee.getAssignedTasks().add(task);
        em.persist(task);
    }
}
