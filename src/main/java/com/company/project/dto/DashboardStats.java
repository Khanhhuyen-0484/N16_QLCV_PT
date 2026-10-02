package com.company.project.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record DashboardStats(
        long totalEmployees,
        long totalTasks,
        long todoTasks,
        long inProgressTasks,
        long completedTasks,
        long overdueTasks,
        int projectProgress,
        List<TaskView> nearDeadlineTasks
) implements Serializable {
    public DashboardStats {
        nearDeadlineTasks = nearDeadlineTasks == null ? new ArrayList<>() : new ArrayList<>(nearDeadlineTasks);
    }
}
