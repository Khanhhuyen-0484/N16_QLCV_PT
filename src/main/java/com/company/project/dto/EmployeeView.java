package com.company.project.dto;

import com.company.project.entity.AccountStatus;
import com.company.project.entity.Employee;
import com.company.project.entity.Role;

import java.io.Serializable;

public record EmployeeView(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        Role role,
        AccountStatus status
) implements Serializable {
    public static EmployeeView from(Employee employee) {
        return new EmployeeView(
                employee.getId(),
                employee.getUsername(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getRole(),
                employee.getStatus()
        );
    }
}
