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
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Công việc trong dự án.
 *  - n - 1 Project  (phía sở hữu của quan hệ Project.tasks)
 *  - n - 1 Employee (người thực hiện - phía sở hữu của Employee.assignedTasks)
 *  - n - 1 Employee (người giao việc)
 */
@Entity
@Table(name = "project_tasks")
@NamedQueries({
        @NamedQuery(name = "ProjectTask.findAll",
                query = "select t from ProjectTask t join fetch t.project join fetch t.assignee left join fetch t.assigner "
                        + "order by t.project.id desc, t.id desc"),
        @NamedQuery(name = "ProjectTask.findByAssignee",
                query = "select t from ProjectTask t join fetch t.project join fetch t.assignee left join fetch t.assigner "
                        + "where t.assignee.id = :employeeId order by t.deadline asc nulls last, t.id desc"),
        @NamedQuery(name = "ProjectTask.findByManager",
                query = "select t from ProjectTask t join fetch t.project p join fetch t.assignee left join fetch t.assigner "
                        + "where p.manager.id = :managerId order by p.id desc, t.id desc"),
        @NamedQuery(name = "ProjectTask.findOpenByAssignee",
                query = "select t from ProjectTask t join fetch t.project where t.assignee.id = :employeeId "
                        + "and t.status in :openStatuses"),
        @NamedQuery(name = "ProjectTask.countByAssignee",
                query = "select count(t) from ProjectTask t where t.assignee.id = :employeeId"),
        @NamedQuery(name = "ProjectTask.countByAssigneeAndStatuses",
                query = "select count(t) from ProjectTask t where t.assignee.id = :employeeId and t.status in :statuses"),
        @NamedQuery(name = "ProjectTask.countOpenByAssigneeInProject",
                query = "select count(t) from ProjectTask t where t.assignee.id = :employeeId "
                        + "and t.project.id = :projectId and t.status in :openStatuses"),
        @NamedQuery(name = "ProjectTask.clearAssigner",
                query = "update ProjectTask t set t.assigner = null where t.assigner.id = :employeeId")
})
public class ProjectTask implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String note;

    @Column(length = 2000)
    private String resultText;

    @Column(length = 500)
    private String resultLink;

    private LocalDate startDate;
    private LocalDate deadline;
    private LocalDateTime completedAt;
    private int progress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30)")
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "varchar(20)")
    private TaskPriority priority = TaskPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private Employee assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigner_id")
    private Employee assigner;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public String getResultLink() {
        return resultLink;
    }

    public void setResultLink(String resultLink) {
        this.resultLink = resultLink;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority == null ? TaskPriority.MEDIUM : priority;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Employee getAssignee() {
        return assignee;
    }

    public void setAssignee(Employee assignee) {
        this.assignee = assignee;
    }

    public Employee getAssigner() {
        return assigner;
    }

    public void setAssigner(Employee assigner) {
        this.assigner = assigner;
    }
}
