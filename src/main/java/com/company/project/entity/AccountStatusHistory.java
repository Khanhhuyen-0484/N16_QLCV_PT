package com.company.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lịch sử thay đổi trạng thái tài khoản: ai đổi, lúc nào, từ trạng thái gì sang gì, lý do.
 * Quan hệ n - 1 tới nhân viên bị đổi trạng thái và tới người thực hiện.
 */
@Entity
@Table(name = "account_status_history")
@NamedQuery(name = "AccountStatusHistory.findByEmployee",
        query = "select h from AccountStatusHistory h left join fetch h.changedBy "
                + "where h.employee.id = :employeeId order by h.changedAt desc, h.id desc")
public class AccountStatusHistory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "varchar(20)")
    private AccountStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private AccountStatus toStatus;

    @Column(length = 255)
    private String reason;

    @Column(length = 1000)
    private String note;

    private LocalDate fromDate;
    private LocalDate toDate;

    @Column(nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private Employee changedBy;

    /** Lưu lại tên người thực hiện để vẫn hiển thị được nếu tài khoản đó bị xóa hẳn. */
    @Column(length = 120)
    private String changedByName;

    public AccountStatusHistory() {
    }

    public AccountStatusHistory(Employee employee, AccountStatus fromStatus, AccountStatus toStatus, String reason,
                                String note, LocalDate fromDate, LocalDate toDate, Employee changedBy) {
        this.employee = employee;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.note = note;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.changedBy = changedBy;
        this.changedByName = changedBy == null ? "Hệ thống" : changedBy.getFullName();
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public AccountStatus getFromStatus() {
        return fromStatus;
    }

    public AccountStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public Employee getChangedBy() {
        return changedBy;
    }

    public String getChangedByName() {
        return changedByName;
    }
}
