package com.company.project.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record DashboardStats(
        long totalProjects,
        long totalEmployees,
        long totalTasks,
        long todoTasks,
        long inProgressTasks,
        long onHoldTasks,
        long completedTasks,
        long overdueTasks,
        int progress,
        List<TaskView> nearDeadlineTasks,
        List<ProjectView> projects
) implements Serializable {
    public DashboardStats {
        nearDeadlineTasks = nearDeadlineTasks == null ? new ArrayList<>() : new ArrayList<>(nearDeadlineTasks);
        projects = projects == null ? new ArrayList<>() : new ArrayList<>(projects);
    }

    public static DashboardStats empty() {
        return new DashboardStats(0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of());
    }
}
