package com.company.project.repository;

import com.company.project.entity.AccountStatus;
import com.company.project.entity.AccountStatusHistory;
import com.company.project.entity.Employee;
import com.company.project.entity.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Tầng truy cập dữ liệu cho tài khoản (Employee) và lịch sử trạng thái. */
public interface EmployeeRepository {
    Optional<Employee> findById(long id);

    Optional<Employee> findByUsername(String username);

    List<Employee> findAll();

    List<Employee> findByRoles(Collection<Role> roles);

    List<Employee> findByStatus(AccountStatus status);

    List<Employee> findByRoleAndStatus(Role role, AccountStatus status);

    long count();

    void save(Employee employee);

    void delete(Employee employee);

    List<AccountStatusHistory> findStatusHistory(long employeeId);

    /** Gỡ tham chiếu "người thực hiện" trong lịch sử trước khi xóa hẳn một tài khoản. */
    int clearHistoryActor(long employeeId);
}
