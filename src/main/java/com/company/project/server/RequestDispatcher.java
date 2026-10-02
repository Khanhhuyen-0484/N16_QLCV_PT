package com.company.project.server;

import com.company.project.entity.ProjectStatus;
import com.company.project.entity.Role;
import com.company.project.entity.TaskPriority;
import com.company.project.network.Action;
import com.company.project.network.Request;
import com.company.project.network.Response;
import com.company.project.service.AppService;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

public class RequestDispatcher {
    private final AppService service;

    public RequestDispatcher(AppService service) {
        this.service = service;
    }

    public Response dispatch(Request request) {
        try {
            Action action = request.getAction();
            Map<String, Serializable> data = request.getData();
            return switch (action) {
                case LOGIN -> Response.ok("Đăng nhập thành công.",
                        service.login(str(data, "username"), str(data, "password")));
                case REGISTER -> Response.ok("Đăng ký thành công. Chờ quản lý phê duyệt.",
                        service.register(str(data, "username"), str(data, "password"),
                                str(data, "fullName"), str(data, "email"), str(data, "phone")));
                case ME -> Response.ok("Thông tin tài khoản.", service.me(request.getToken()));
                case CHANGE_PASSWORD -> {
                    service.changePassword(request.getToken(), str(data, "oldPassword"), str(data, "newPassword"));
                    yield Response.ok("Đổi mật khẩu thành công.");
                }
                case UPDATE_PROFILE -> Response.ok("Cập nhật thông tin thành công.",
                        service.updateProfile(request.getToken(), str(data, "fullName"), str(data, "email"), str(data, "phone")));
                case DASHBOARD -> Response.ok("Thống kê tổng quan.", service.dashboard(request.getToken()));
                case MY_TASKS -> Response.ok("Danh sách công việc.", (Serializable) service.myTasks(request.getToken()));
                case UPDATE_TASK_PROGRESS -> Response.ok("Cập nhật tiến độ thành công.",
                        service.updateTaskProgress(request.getToken(), lng(data, "taskId"), integer(data, "progress")));
                case COMPLETE_TASK -> Response.ok("Đã đánh dấu hoàn thành.",
                        service.completeTask(request.getToken(), lng(data, "taskId")));
                case ADD_TASK_NOTE -> Response.ok("Đã lưu ghi chú.",
                        service.addTaskNote(request.getToken(), lng(data, "taskId"), str(data, "note")));
                case SUBMIT_TASK_RESULT -> Response.ok("Đã nộp kết quả công việc.",
                        service.submitTaskResult(request.getToken(), lng(data, "taskId"), str(data, "resultText"),
                                str(data, "resultLink"), str(data, "note")));
                case LIST_EMPLOYEES -> Response.ok("Danh sách nhân viên.", (Serializable) service.listEmployees(request.getToken()));
                case LIST_EMPLOYEE_DIRECTORY -> Response.ok("Danh sách nhân viên.", (Serializable) service.listEmployeeDirectory(request.getToken()));
                case LIST_PENDING_EMPLOYEES -> Response.ok("Danh sách đang chờ duyệt.", (Serializable) service.listPendingEmployees(request.getToken()));
                case CREATE_EMPLOYEE -> Response.ok("Đã thêm nhân viên.",
                        service.createEmployee(request.getToken(), str(data, "username"), str(data, "password"),
                                str(data, "fullName"), str(data, "email"), str(data, "phone")));
                case UPDATE_EMPLOYEE -> Response.ok("Đã cập nhật nhân viên.",
                        service.updateEmployee(request.getToken(), lng(data, "employeeId"), str(data, "fullName"),
                                str(data, "email"), str(data, "phone"), role(data, "role")));
                case APPROVE_EMPLOYEE -> Response.ok("Đã phê duyệt tài khoản.",
                        service.approveEmployee(request.getToken(), lng(data, "employeeId")));
                case DISABLE_EMPLOYEE -> Response.ok("Đã khóa tài khoản.",
                        service.disableEmployee(request.getToken(), lng(data, "employeeId")));
                case ENABLE_EMPLOYEE -> Response.ok("Đã kích hoạt tài khoản.",
                        service.enableEmployee(request.getToken(), lng(data, "employeeId")));
                case CREATE_PROJECT -> Response.ok("Cập nhật thông tin dự án thành công.",
                        service.createProject(request.getToken(), str(data, "name"), str(data, "description"),
                                date(data, "startDate"), date(data, "deadline")));
                case LIST_PROJECTS -> Response.ok("Danh sách dự án.", (Serializable) service.listProjects(request.getToken()));
                case UPDATE_PROJECT -> Response.ok("Cập nhật dự án thành công.",
                        service.updateProject(request.getToken(), lng(data, "projectId"), str(data, "name"),
                                str(data, "description"), date(data, "startDate"), date(data, "deadline"),
                                projectStatus(data, "status")));
                case DELETE_PROJECT -> {
                    service.deleteProject(request.getToken(), lng(data, "projectId"));
                    yield Response.ok("Đã xóa dự án.");
                }
                case ASSIGN_PROJECT_MEMBER -> Response.ok("Đã phân công nhân viên vào dự án.",
                        service.assignProjectMember(request.getToken(), lng(data, "projectId"), lng(data, "employeeId")));
                case CREATE_TASK -> Response.ok("Tạo công việc thành công.",
                        service.createTask(request.getToken(), lng(data, "projectId"), lng(data, "assigneeId"),
                                str(data, "title"), str(data, "description"), date(data, "startDate"), date(data, "deadline"),
                                priority(data, "priority")));
                case LIST_TASKS -> Response.ok("Danh sách công việc.", (Serializable) service.listTasks(request.getToken()));
                case LOGOUT -> {
                    service.logout(request.getToken());
                    yield Response.ok("Đã đăng xuất.");
                }
                case SHUTDOWN -> Response.ok("Server vận hành bình thường.");
            };
        } catch (SecurityException | IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            return Response.error("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private static String str(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static long lng(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static int integer(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static LocalDate date(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private static ProjectStatus projectStatus(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value instanceof ProjectStatus status) {
            return status;
        }
        return ProjectStatus.valueOf(String.valueOf(value));
    }

    private static Role role(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value instanceof Role role) {
            return role;
        }
        return Role.valueOf(String.valueOf(value));
    }

    private static TaskPriority priority(Map<String, Serializable> data, String key) {
        Object value = data.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return TaskPriority.MEDIUM;
        }
        if (value instanceof TaskPriority taskPriority) {
            return taskPriority;
        }
        return TaskPriority.valueOf(String.valueOf(value));
    }
}
