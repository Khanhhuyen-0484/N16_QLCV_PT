package com.company.project.repository;

import com.company.project.entity.ProjectTask;
import com.company.project.entity.TaskStatus;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class TaskRepositoryImpl implements TaskRepository {
    static final List<TaskStatus> OPEN_STATUSES = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.ON_HOLD);

    private final EntityManager em;

    public TaskRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<ProjectTask> findById(long id) {
        return Optional.ofNullable(em.find(ProjectTask.class, id));
    }

    @Override
    public List<ProjectTask> findAll() {
        return em.createNamedQuery("ProjectTask.findAll", ProjectTask.class).getResultList();
    }

    @Override
    public List<ProjectTask> findByAssignee(long employeeId) {
        return em.createNamedQuery("ProjectTask.findByAssignee", ProjectTask.class)
                .setParameter("employeeId", employeeId)
                .getResultList();
    }

    @Override
    public List<ProjectTask> findByManager(long managerId) {
        return em.createNamedQuery("ProjectTask.findByManager", ProjectTask.class)
                .setParameter("managerId", managerId)
                .getResultList();
    }

    @Override
    public List<ProjectTask> findOpenByAssignee(long employeeId) {
        return em.createNamedQuery("ProjectTask.findOpenByAssignee", ProjectTask.class)
                .setParameter("employeeId", employeeId)
                .setParameter("openStatuses", OPEN_STATUSES)
                .getResultList();
    }

    @Override
    public long countByAssignee(long employeeId) {
        return em.createNamedQuery("ProjectTask.countByAssignee", Long.class)
                .setParameter("employeeId", employeeId)
                .getSingleResult();
    }

    @Override
    public long countOpenByAssignee(long employeeId) {
        return em.createNamedQuery("ProjectTask.countByAssigneeAndStatuses", Long.class)
                .setParameter("employeeId", employeeId)
                .setParameter("statuses", OPEN_STATUSES)
                .getSingleResult();
    }

    @Override
    public long countCompletedByAssignee(long employeeId) {
        return em.createNamedQuery("ProjectTask.countByAssigneeAndStatuses", Long.class)
                .setParameter("employeeId", employeeId)
                .setParameter("statuses", List.of(TaskStatus.COMPLETED))
                .getSingleResult();
    }

    @Override
    public long countOpenByAssigneeInProject(long employeeId, long projectId) {
        return em.createNamedQuery("ProjectTask.countOpenByAssigneeInProject", Long.class)
                .setParameter("employeeId", employeeId)
                .setParameter("projectId", projectId)
                .setParameter("openStatuses", OPEN_STATUSES)
                .getSingleResult();
    }

    @Override
    public void save(ProjectTask task) {
        if (task.getId() == null) {
            em.persist(task);
        } else {
            em.merge(task);
        }
    }

    @Override
    public void delete(ProjectTask task) {
        task.getProject().getTasks().remove(task);
        task.getAssignee().getAssignedTasks().remove(task);
        em.remove(task);
    }

    @Override
    public int clearAssigner(long employeeId) {
        return em.createNamedQuery("ProjectTask.clearAssigner")
                .setParameter("employeeId", employeeId)
                .executeUpdate();
    }
}
