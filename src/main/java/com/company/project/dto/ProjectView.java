package com.company.project.dto;

import com.company.project.entity.Project;
import com.company.project.entity.ProjectStatus;
import com.company.project.entity.ProjectTask;
import com.company.project.entity.TaskStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record ProjectView(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate deadline,
        ProjectStatus status,
        Long managerId,
        String managerName,
        List<EmployeeView> members,
        long totalTasks,
        long completedTasks,
        long openTasks,
        int progress
) implements Serializable {
    /** Dùng quan hệ 2 chiều Project.tasks / Project.members để tính thống kê của dự án. */
    public static ProjectView from(Project project) {
        List<ProjectTask> tasks = project.getTasks();
        long total = tasks.stream().filter(t -> t.getStatus() != TaskStatus.CANCELLED).count();
        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long open = tasks.stream().filter(t -> t.getStatus().isOpen()).count();
        int progress = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
        List<EmployeeView> members = new ArrayList<>(project.getMembers().stream()
                .sorted(Comparator.comparing(e -> e.getFullName() == null ? "" : e.getFullName()))
                .map(EmployeeView::from)
                .toList());
        return new ProjectView(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStartDate(),
                project.getDeadline(),
                project.getStatus(),
                project.getManager() == null ? null : project.getManager().getId(),
                project.getManager() == null ? "" : project.getManager().getFullName(),
                members,
                total,
                completed,
                open,
                progress
        );
    }
}
