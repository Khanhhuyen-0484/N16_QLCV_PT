package com.company.project.repository;

import com.company.project.entity.ProjectTask;

import java.util.List;
import java.util.Optional;

/** Tầng truy cập dữ liệu cho công việc. */
public interface TaskRepository {
    Optional<ProjectTask> findById(long id);

    List<ProjectTask> findAll();

    List<ProjectTask> findByAssignee(long employeeId);

    List<ProjectTask> findByManager(long managerId);

    List<ProjectTask> findOpenByAssignee(long employeeId);

    long countByAssignee(long employeeId);

    long countOpenByAssignee(long employeeId);

    long countCompletedByAssignee(long employeeId);

    long countOpenByAssigneeInProject(long employeeId, long projectId);

    void save(ProjectTask task);

    void delete(ProjectTask task);

    /** Gỡ tham chiếu "người giao việc" trước khi xóa hẳn một tài khoản. */
    int clearAssigner(long employeeId);
}
