package com.company.project.dto;

import java.io.Serializable;

public record EmployeeDirectoryView(
        EmployeeView employee,
        long inProgressTasks,
        long completedTasks
) implements Serializable {
}
