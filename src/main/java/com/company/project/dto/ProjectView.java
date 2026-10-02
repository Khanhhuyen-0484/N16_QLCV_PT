package com.company.project.dto;

import com.company.project.entity.Project;
import com.company.project.entity.ProjectStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record ProjectView(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate deadline,
        ProjectStatus status,
        List<EmployeeView> members
) implements Serializable {
    public static ProjectView from(Project project) {
        return new ProjectView(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStartDate(),
                project.getDeadline(),
                project.getStatus(),
                new ArrayList<>(project.getMembers().stream().map(EmployeeView::from).toList())
        );
    }
}
