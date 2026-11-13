package com.company.project.dto;

import com.company.project.entity.ProjectTask;
import com.company.project.entity.TaskPriority;
import com.company.project.entity.TaskStatus;

import java.io.Serializable;
import java.time.LocalDate;

public record TaskView(
        Long id,
        String title,
        String description,
        String note,
        String resultText,
        String resultLink,
        LocalDate startDate,
        LocalDate deadline,
        int progress,
        TaskStatus status,
        TaskPriority priority,
        boolean overdue,
        Long projectId,
        String projectName,
        boolean projectClosed,
        Long assigneeId,
        String assigneeName,
        Long assignerId,
        String assignerName
) implements Serializable {
    public static TaskView from(ProjectTask task) {
        boolean overdue = task.getDeadline() != null
                && task.getDeadline().isBefore(LocalDate.now())
                && task.getStatus().isOpen();
        return new TaskView(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getNote(),
                task.getResultText(),
                task.getResultLink(),
                task.getStartDate(),
                task.getDeadline(),
                task.getProgress(),
                task.getStatus(),
                task.getPriority() == null ? TaskPriority.MEDIUM : task.getPriority(),
                overdue,
                task.getProject().getId(),
                task.getProject().getName(),
                task.getProject().getStatus().isClosed(),
                task.getAssignee().getId(),
                task.getAssignee().getFullName(),
                task.getAssigner() == null ? null : task.getAssigner().getId(),
                task.getAssigner() == null ? "" : task.getAssigner().getFullName()
        );
    }
}
