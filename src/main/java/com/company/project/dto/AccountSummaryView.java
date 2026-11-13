package com.company.project.dto;

import java.io.Serializable;

/** Một dòng trong danh sách tài khoản: thông tin tài khoản + thống kê công việc. */
public record AccountSummaryView(
        EmployeeView employee,
        long openTasks,
        long completedTasks,
        long totalTasks,
        long managedProjects
) implements Serializable {
}
