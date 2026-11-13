package com.company.project.repository;

import com.company.project.entity.Project;

import java.util.List;
import java.util.Optional;

/** Tầng truy cập dữ liệu cho dự án. */
public interface ProjectRepository {
    Optional<Project> findById(long id);

    List<Project> findAll();

    List<Project> findByManager(long managerId);

    List<Project> findByMember(long employeeId);

    void save(Project project);

    /** Xóa dự án: công việc bị xóa theo (cascade REMOVE), dòng thành viên bị xóa theo (phía sở hữu n - n). */
    void delete(Project project);
}
