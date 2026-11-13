package com.company.project.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Dự án.
 *  - n - 1 Employee  : quản lý phụ trách (manager)
 *  - n - n Employee  : thành viên (bảng trung gian project_members) - phía chủ sở hữu quan hệ
 *  - 1 - n ProjectTask : danh sách công việc, cascade REMOVE nên xóa dự án sẽ xóa luôn công việc
 */
@Entity
@Table(name = "projects")
@NamedQueries({
        @NamedQuery(name = "Project.findAll",
                query = "select p from Project p left join fetch p.manager order by p.id desc"),
        @NamedQuery(name = "Project.findByManager",
                query = "select p from Project p left join fetch p.manager where p.manager.id = :managerId order by p.id desc"),
        @NamedQuery(name = "Project.findByMember",
                query = "select distinct p from Project p left join fetch p.manager join p.members m "
                        + "where m.id = :employeeId order by p.id desc")
})
public class Project implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    private LocalDate startDate;
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private ProjectStatus status = ProjectStatus.PLANNING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_members",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    private Set<Employee> members = new LinkedHashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ProjectTask> tasks = new ArrayList<>();

    public Project() {
    }

    /** Thêm thành viên, cập nhật cả 2 phía của quan hệ n - n. */
    public void addMember(Employee employee) {
        members.add(employee);
        employee.getProjects().add(this);
    }

    /** Gỡ thành viên, cập nhật cả 2 phía của quan hệ n - n. */
    public void removeMember(Employee employee) {
        members.remove(employee);
        employee.getProjects().remove(this);
    }

    public boolean hasMember(Employee employee) {
        return members.stream().anyMatch(member -> member.getId().equals(employee.getId()));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    public Set<Employee> getMembers() {
        return members;
    }

    public List<ProjectTask> getTasks() {
        return tasks;
    }
}
