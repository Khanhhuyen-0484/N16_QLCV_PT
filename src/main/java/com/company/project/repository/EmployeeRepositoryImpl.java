package com.company.project.repository;

import com.company.project.entity.AccountStatus;
import com.company.project.entity.AccountStatusHistory;
import com.company.project.entity.Employee;
import com.company.project.entity.Role;
import jakarta.persistence.EntityManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class EmployeeRepositoryImpl implements EmployeeRepository {
    private final EntityManager em;

    public EmployeeRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<Employee> findById(long id) {
        return Optional.ofNullable(em.find(Employee.class, id));
    }

    @Override
    public Optional<Employee> findByUsername(String username) {
        return em.createNamedQuery("Employee.findByUsername", Employee.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Employee> findAll() {
        return em.createNamedQuery("Employee.findAll", Employee.class).getResultList();
    }

    @Override
    public List<Employee> findByRoles(Collection<Role> roles) {
        return em.createNamedQuery("Employee.findByRoles", Employee.class)
                .setParameter("roles", roles)
                .getResultList();
    }

    @Override
    public List<Employee> findByStatus(AccountStatus status) {
        return em.createNamedQuery("Employee.findByStatus", Employee.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Employee> findByRoleAndStatus(Role role, AccountStatus status) {
        return em.createNamedQuery("Employee.findByRoleAndStatus", Employee.class)
                .setParameter("role", role)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public long count() {
        return em.createQuery("select count(e) from Employee e", Long.class).getSingleResult();
    }

    @Override
    public void save(Employee employee) {
        if (employee.getId() == null) {
            em.persist(employee);
        } else {
            em.merge(employee);
        }
    }

    @Override
    public void delete(Employee employee) {
        em.remove(employee);
    }

    @Override
    public List<AccountStatusHistory> findStatusHistory(long employeeId) {
        return em.createNamedQuery("AccountStatusHistory.findByEmployee", AccountStatusHistory.class)
                .setParameter("employeeId", employeeId)
                .getResultList();
    }

    @Override
    public int clearHistoryActor(long employeeId) {
        return em.createQuery("update AccountStatusHistory h set h.changedBy = null where h.changedBy.id = :id")
                .setParameter("id", employeeId)
                .executeUpdate();
    }
}
