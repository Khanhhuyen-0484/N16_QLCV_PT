package com.company.project.server;

import com.company.project.entity.LeaveReason;
import com.company.project.entity.ProjectStatus;
import com.company.project.entity.RemovalReason;
import com.company.project.entity.Role;
import com.company.project.entity.TaskPriority;
import com.company.project.network.Action;
import com.company.project.network.Request;
import com.company.project.network.Response;
import com.company.project.service.AccountService;
import com.company.project.service.AuthService;
import com.company.project.service.ProjectService;
import com.company.project.service.SessionExpiredException;
import com.company.project.service.TaskService;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Nhận Request từ ClientWorker, gọi service tương ứng và đóng gói kết quả thành Response. */
public class RequestDispatcher {
    private final AuthService auth;
    private final AccountService accounts;
    private final ProjectService projects;
    private final TaskService tasks;

    public RequestDispatcher(AuthService auth, AccountService accounts, ProjectService projects, TaskService tasks) {
        this.auth = auth;
        this.accounts = accounts;
        this.projects = projects;
        this.tasks = tasks;
    }

    public Response dispatch(Request request) {
        try {
            return handle(request.getAction(), request.getToken(), request.getData());
        } catch (SessionExpiredException e) {
            return Response.sessionExpired(e.getMessage());
        } catch (SecurityException | IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (DateTimeParseException e) {
            return Response.error("Ngày không hợp lệ, hãy nhập theo dạng yyyy-MM-dd.");
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private Response handle(Action action, String token, Map<String, Serializable> d) {
        return switch (action) {
            // ------------------------------------------------ tài khoản của chính mình
            case LOGIN -> Response.ok("Đăng nhập thành công.", auth.login(str(d, "username"), raw(d, "password")));
            case REGISTER -> Response.ok("Đăng ký thành công. Vui lòng chờ quản lý duyệt tài khoản.",
                    auth.register(str(d, "username"), raw(d, "password"), str(d, "fullName"),
                            str(d, "email"), str(d, "phone")));
            case LOGOUT -> {
                auth.logout(token);
                yield Response.ok("Đã đăng xuất.");
            }
            case ME -> Response.ok("Thông tin tài khoản.", auth.me(token));
            case UPDATE_PROFILE -> Response.ok("Cập nhật thông tin thành công.",
                    auth.updateProfile(token, str(d, "fullName"), str(d, "email"), str(d, "phone")));
            case CHANGE_PASSWORD -> {
                auth.changePassword(token, raw(d, "oldPassword"), raw(d, "newPassword"));
                yield Response.ok("Đổi mật khẩu thành công.");
            }
            case DASHBOARD -> Response.ok("Thống kê tổng quan.", tasks.dashboard(token));

            // ------------------------------------------------ quản lý tài khoản
            case LIST_ACCOUNTS -> Response.ok("Danh sách tài khoản.", (Serializable) accounts.listAccounts(token));
            case LIST_PENDING_ACCOUNTS -> Response.ok("Danh sách chờ duyệt.", (Serializable) accounts.listPending(token));
            case LIST_MANAGERS -> Response.ok("Danh sách quản lý.", (Serializable) accounts.listManagers(token));
            case LIST_ASSIGNABLE_EMPLOYEES -> Response.ok("Danh sách nhân viên.",
                    (Serializable) accounts.listAssignableEmployees(token));
            case LIST_STATUS_HISTORY -> Response.ok("Lịch sử trạng thái.",
                    (Serializable) accounts.statusHistory(token, lng(d, "employeeId")));
            case CREATE_ACCOUNT -> Response.ok("Đã tạo tài khoản.",
                    accounts.createAccount(token, str(d, "username"), raw(d, "password"), str(d, "fullName"),
                            str(d, "email"), str(d, "phone"), enumValue(d, "role", Role.class)));
            case UPDATE_ACCOUNT -> Response.ok("Đã cập nhật tài khoản.",
                    accounts.updateAccount(token, lng(d, "employeeId"), str(d, "fullName"), str(d, "email"),
                            str(d, "phone"), enumValue(d, "role", Role.class)));
            case APPROVE_ACCOUNT -> Response.ok("Đã duyệt tài khoản.", accounts.approve(token, lng(d, "employeeId")));
            case REJECT_ACCOUNT -> Response.ok("Đã từ chối đăng ký của "
                    + accounts.reject(token, lng(d, "employeeId")) + ".");
            case LOCK_ACCOUNT -> Response.ok("Đã khóa tài khoản.",
                    accounts.lock(token, lng(d, "employeeId"), str(d, "reason")));
            case UNLOCK_ACCOUNT -> Response.ok("Đã mở khóa tài khoản.", accounts.unlock(token, lng(d, "employeeId")));
            case SUSPEND_ACCOUNT -> Response.ok("Đã tạm dừng tài khoản.",
                    accounts.suspend(token, lng(d, "employeeId"), enumValue(d, "reason", LeaveReason.class),
                            date(d, "from"), date(d, "until"), str(d, "note"), bool(d, "transferTasks"),
                            optionalLong(d, "transferToId")));
            case REACTIVATE_ACCOUNT -> Response.ok("Đã kích hoạt lại tài khoản.",
                    accounts.reactivate(token, lng(d, "employeeId")));
            case DELETE_ACCOUNT -> Response.ok(accounts.delete(token, lng(d, "employeeId"),
                    enumValue(d, "reason", RemovalReason.class), str(d, "note")));
            case TRANSFER_OPEN_TASKS -> Response.ok("Đã chuyển giao "
                    + accounts.transferOpenTasks(token, lng(d, "fromEmployeeId"), lng(d, "toEmployeeId"))
                    + " công việc.");

            // ------------------------------------------------ dự án
            case LIST_PROJECTS -> Response.ok("Danh sách dự án.", (Serializable) projects.list(token));
            case CREATE_PROJECT -> Response.ok("Đã tạo dự án.",
                    projects.create(token, str(d, "name"), str(d, "description"), date(d, "startDate"),
                            date(d, "deadline"), optionalLong(d, "managerId")));
            case UPDATE_PROJECT -> Response.ok("Đã cập nhật dự án.",
                    projects.update(token, lng(d, "projectId"), str(d, "name"), str(d, "description"),
                            date(d, "startDate"), date(d, "deadline"), enumValue(d, "status", ProjectStatus.class),
                            optionalLong(d, "managerId"), bool(d, "confirmIncomplete")));
            case DELETE_PROJECT -> Response.ok("Đã xóa dự án \"" + projects.delete(token, lng(d, "projectId"))
                    + "\" cùng các công việc của dự án.");
            case ADD_PROJECT_MEMBER -> Response.ok("Đã thêm thành viên vào dự án.",
                    projects.addMember(token, lng(d, "projectId"), lng(d, "employeeId")));
            case REMOVE_PROJECT_MEMBER -> Response.ok("Đã gỡ thành viên khỏi dự án.",
                    projects.removeMember(token, lng(d, "projectId"), lng(d, "employeeId")));

            // ------------------------------------------------ công việc
            case LIST_TASKS -> Response.ok("Danh sách công việc.", (Serializable) tasks.list(token));
            case MY_TASKS -> Response.ok("Công việc của tôi.", (Serializable) tasks.myTasks(token));
            case CREATE_TASK -> Response.ok("Đã giao công việc.",
                    tasks.create(token, lng(d, "projectId"), lng(d, "assigneeId"), str(d, "title"),
                            str(d, "description"), date(d, "startDate"), date(d, "deadline"), priority(d)));
            case UPDATE_TASK -> Response.ok("Đã cập nhật công việc.",
                    tasks.update(token, lng(d, "taskId"), str(d, "title"), str(d, "description"),
                            date(d, "startDate"), date(d, "deadline"), priority(d)));
            case DELETE_TASK -> Response.ok("Đã xóa công việc \"" + tasks.delete(token, lng(d, "taskId")) + "\".");
            case TRANSFER_TASK -> Response.ok("Đã chuyển giao công việc.",
                    tasks.transfer(token, lng(d, "taskId"), lng(d, "assigneeId")));
            case REOPEN_TASK -> Response.ok("Đã mở lại công việc.", tasks.reopen(token, lng(d, "taskId")));
            case UPDATE_TASK_PROGRESS -> Response.ok("Đã cập nhật tiến độ.",
                    tasks.updateProgress(token, lng(d, "taskId"), integer(d, "progress")));
            case COMPLETE_TASK -> Response.ok("Đã đánh dấu hoàn thành.", tasks.complete(token, lng(d, "taskId")));
            case ADD_TASK_NOTE -> Response.ok("Đã lưu ghi chú.",
                    tasks.addNote(token, lng(d, "taskId"), str(d, "note")));
            case SUBMIT_TASK_RESULT -> Response.ok("Đã nộp kết quả công việc.",
                    tasks.submitResult(token, lng(d, "taskId"), str(d, "resultText"), str(d, "resultLink"),
                            str(d, "note")));
        };
    }

    // ---------------------------------------------------------------- đọc tham số

    private static String str(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Giữ nguyên giá trị (không trim) - dùng cho mật khẩu. */
    private static String raw(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static long lng(Map<String, Serializable> data, String key) {
        Long value = optionalLong(data, key);
        if (value == null) {
            throw new IllegalArgumentException("Thiếu tham số " + key + ".");
        }
        return value;
    }

    private static Long optionalLong(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá trị " + key + " không hợp lệ.");
        }
    }

    private static int integer(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá trị " + key + " phải là số nguyên.");
        }
    }

    private static boolean bool(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    private static LocalDate date(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(String.valueOf(value).trim());
    }

    private static <E extends Enum<E>> E enumValue(Map<String, Serializable> data, String key, Class<E> type) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return Enum.valueOf(type, String.valueOf(value).trim());
    }

    private static TaskPriority priority(Map<String, Serializable> data) {
        TaskPriority priority = enumValue(data, "priority", TaskPriority.class);
        return priority == null ? TaskPriority.MEDIUM : priority;
    }
}
