package com.company.project.service;

import com.company.project.repository.EmployeeRepository;
import com.company.project.repository.EmployeeRepositoryImpl;
import com.company.project.repository.ProjectRepository;
import com.company.project.repository.ProjectRepositoryImpl;
import com.company.project.repository.TaskRepository;
import com.company.project.repository.TaskRepositoryImpl;
import jakarta.persistence.EntityManager;

/** Gom các repository dùng chung một EntityManager trong một lần xử lý yêu cầu. */
public record DataContext(
        EntityManager em,
        EmployeeRepository employees,
        ProjectRepository projects,
        TaskRepository tasks
) {
    public static DataContext of(EntityManager em) {
        return new DataContext(em,
                new EmployeeRepositoryImpl(em),
                new ProjectRepositoryImpl(em),
                new TaskRepositoryImpl(em));
    }
}
