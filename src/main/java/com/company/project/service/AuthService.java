package com.company.project.service;

import com.company.project.dto.EmployeeView;
import com.company.project.dto.LoginResult;
import com.company.project.entity.AccountStatus;
import com.company.project.entity.Employee;
import com.company.project.entity.Role;
import com.company.project.util.PasswordUtil;

/** Đăng nhập, đăng ký và các thao tác trên tài khoản của chính người dùng. */
public class AuthService extends BaseService {

    public AuthService(SessionManager sessions) {
        super(sessions);
    }

    public LoginResult login(String username, String password) {
        return read(ctx -> {
            Employee employee = ctx.employees().findByUsername(username == null ? "" : username.trim()).orElse(null);
            if (employee == null || password == null || !PasswordUtil.matches(password, employee.getPasswordHash())) {
                throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu.");
            }
            if (employee.getStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException(statusMessage(employee));
            }
            return new LoginResult(sessions.create(employee.getId()), EmployeeView.from(employee));
        });
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    /** Nhân viên mới tự đăng ký: tài khoản ở trạng thái chờ duyệt cho tới khi quản lý duyệt. */
    public EmployeeView register(String username, String password, String fullName, String email, String phone) {
        return write(ctx -> {
            String cleanUsername = requireText(username, "Tên đăng nhập");
            requirePassword(password);
            String cleanName = requireText(fullName, "Họ tên");
            if (ctx.employees().findByUsername(cleanUsername).isPresent()) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
            }
            Employee employee = new Employee();
            employee.setUsername(cleanUsername);
            employee.setPasswordHash(PasswordUtil.hash(password));
            employee.setFullName(cleanName);
            employee.setEmail(email);
            employee.setPhone(phone);
            employee.setRole(Role.EMPLOYEE);
            employee.initStatus(AccountStatus.PENDING, "Nhân viên tự đăng ký, chờ duyệt", null);
            ctx.employees().save(employee);
            return EmployeeView.from(employee);
        });
    }

    public EmployeeView me(String token) {
        return read(ctx -> EmployeeView.from(currentUser(ctx, token)));
    }

    public EmployeeView updateProfile(String token, String fullName, String email, String phone) {
        return write(ctx -> {
            Employee employee = currentUser(ctx, token);
            employee.setFullName(requireText(fullName, "Họ tên"));
            employee.setEmail(email);
            employee.setPhone(phone);
            return EmployeeView.from(employee);
        });
    }

    public void changePassword(String token, String oldPassword, String newPassword) {
        write(ctx -> {
            Employee employee = currentUser(ctx, token);
            if (oldPassword == null || !PasswordUtil.matches(oldPassword, employee.getPasswordHash())) {
                throw new IllegalArgumentException("Mật khẩu cũ không đúng.");
            }
            requirePassword(newPassword);
            employee.setPasswordHash(PasswordUtil.hash(newPassword));
            return null;
        });
    }
}
