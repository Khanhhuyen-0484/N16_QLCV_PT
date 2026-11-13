package com.company.project.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tài khoản người dùng (Admin / Quản lý / Nhân viên).
 * Quan hệ 2 chiều:
 *  - Employee 1 - n ProjectTask (công việc được giao)        mappedBy = "assignee"
 *  - Employee n - n Project     (dự án tham gia)              mappedBy = "members"
 *  - Employee 1 - n Project     (dự án phụ trách - quản lý)   mappedBy = "manager"
 *  - Employee 1 - n AccountStatusHistory (lịch sử trạng thái) mappedBy = "employee"
 */
@Entity
@Table(name = "employees", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@NamedQueries({
        @NamedQuery(name = "Employee.findAll",
                query = "select e from Employee e order by e.role, e.fullName, e.id"),
        @NamedQuery(name = "Employee.findByUsername",
                query = "select e from Employee e where e.username = :username"),
        @NamedQuery(name = "Employee.findByRoles",
                query = "select e from Employee e where e.role in :roles order by e.fullName, e.id"),
        @NamedQuery(name = "Employee.findByStatus",
                query = "select e from Employee e where e.status = :status order by e.id"),
        @NamedQuery(name = "Employee.findByRoleAndStatus",
                query = "select e from Employee e where e.role = :role and e.status = :status order by e.fullName, e.id")
})
public class Employee implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String username;

    @Column(nullable = false, length = 128)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(length = 120)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Role role = Role.EMPLOYEE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private AccountStatus status = AccountStatus.PENDING;

    /** Lý do của trạng thái hiện tại (tạm dừng / khóa / nghỉ việc). */
    @Column(length = 255)
    private String statusReason;

    /** Ngày bắt đầu của trạng thái hiện tại (vd: ngày bắt đầu nghỉ). */
    private LocalDate statusFrom;

    /** Ngày dự kiến kết thúc (vd: ngày dự kiến quay lại làm việc). */
    private LocalDate statusUntil;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "assignee")
    private List<ProjectTask> assignedTasks = new ArrayList<>();

    @ManyToMany(mappedBy = "members")
    private Set<Project> projects = new LinkedHashSet<>();

    @OneToMany(mappedBy = "manager")
    private List<Project> managedProjects = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt DESC")
    private List<AccountStatusHistory> statusHistory = new ArrayList<>();

    public Employee() {
    }

    /** Đặt trạng thái ban đầu khi tạo tài khoản và ghi lịch sử (không có trạng thái trước đó). */
    public void initStatus(AccountStatus initialStatus, String reason, Employee createdBy) {
        this.status = initialStatus;
        statusHistory.add(new AccountStatusHistory(this, null, initialStatus, reason, null,
                LocalDate.now(), null, createdBy));
    }

    /** Đổi trạng thái và ghi lại lịch sử (bản ghi lịch sử được lưu nhờ cascade). */
    public AccountStatusHistory changeStatus(AccountStatus newStatus, String reason, String note,
                                             LocalDate from, LocalDate until, Employee changedBy) {
        AccountStatusHistory history = new AccountStatusHistory(this, status, newStatus, reason, note,
                from, until, changedBy);
        statusHistory.add(history);
        this.status = newStatus;
        this.statusReason = reason;
        this.statusFrom = from;
        this.statusUntil = until;
        return history;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isManager() {
        return role == Role.MANAGER;
    }

    public boolean isEmployee() {
        return role == Role.EMPLOYEE;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public LocalDate getStatusFrom() {
        return statusFrom;
    }

    public LocalDate getStatusUntil() {
        return statusUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ProjectTask> getAssignedTasks() {
        return assignedTasks;
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public List<Project> getManagedProjects() {
        return managedProjects;
    }

    public List<AccountStatusHistory> getStatusHistory() {
        return statusHistory;
    }
}
