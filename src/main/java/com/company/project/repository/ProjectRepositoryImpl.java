package com.company.project.repository;

import com.company.project.entity.Project;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class ProjectRepositoryImpl implements ProjectRepository {
    private final EntityManager em;

    public ProjectRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<Project> findById(long id) {
        return Optional.ofNullable(em.find(Project.class, id));
    }

    @Override
    public List<Project> findAll() {
        return em.createNamedQuery("Project.findAll", Project.class).getResultList();
    }

    @Override
    public List<Project> findByManager(long managerId) {
        return em.createNamedQuery("Project.findByManager", Project.class)
                .setParameter("managerId", managerId)
                .getResultList();
    }

    @Override
    public List<Project> findByMember(long employeeId) {
        return em.createNamedQuery("Project.findByMember", Project.class)
                .setParameter("employeeId", employeeId)
                .getResultList();
    }

    @Override
    public void save(Project project) {
        if (project.getId() == null) {
            em.persist(project);
        } else {
            em.merge(project);
        }
    }

    @Override
    public void delete(Project project) {
        // Cập nhật phía nghịch đảo của quan hệ n - n trước khi xóa
        project.getMembers().forEach(member -> member.getProjects().remove(project));
        em.remove(project);
    }
}
