package com.company.project.client;

import com.company.project.dto.EmployeeView;
import com.company.project.dto.ProjectView;
import com.company.project.dto.TaskView;
import com.company.project.entity.ProjectStatus;
import com.company.project.entity.Role;
import com.company.project.network.Action;
import com.company.project.network.Request;
import com.company.project.network.Response;
import com.company.project.service.AppService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TaskClient implements AutoCloseable {
    private final Socket socket;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private final Scanner scanner = new Scanner(System.in);
    private String token;
    private EmployeeView currentUser;

    public TaskClient(String host, int port) throws Exception {
        socket = new Socket(host, port);
        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 2026;
        try (TaskClient client = new TaskClient(host, port)) {
            client.run();
        }
    }

    private void run() throws Exception {
        while (true) {
            System.out.println();
            System.out.println("=== ỨNG DỤNG QUẢN LÝ PHÂN CÔNG CÔNG VIỆC ===");
            System.out.println("1. Đăng nhập");
            System.out.println("2. Đăng ký nhân viên mới");
            System.out.println("0. Thoát");
            String choice = prompt("Chọn: ");
            switch (choice) {
                case "1" -> login();
                case "2" -> register();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    private void login() throws Exception {
        Response response = send(Action.LOGIN, null, map(
                "username", prompt("Tên đăng nhập: "),
                "password", prompt("Mật khẩu: ")
        ));
        print(response);
        if (!response.isSuccess()) {
            return;
        }
        AppService.LoginResult result = (AppService.LoginResult) response.getData();
        token = result.token();
        currentUser = result.employee();
        if (currentUser.role() == Role.MANAGER) {
            managerMenu();
        } else {
            employeeMenu();
        }
    }

    private void register() throws Exception {
        Response response = send(Action.REGISTER, null, map(
                "username", prompt("Tên đăng nhập: "),
                "password", prompt("Mật khẩu: "),
                "fullName", prompt("Họ tên: "),
                "email", prompt("Email: "),
                "phone", prompt("Số điện thoại: ")
        ));
        print(response);
    }

    private void employeeMenu() throws Exception {
        while (token != null) {
            System.out.println();
            System.out.println("=== MENU NHÂN VIÊN ===");
            System.out.println("1. Xem thông tin cá nhân");
            System.out.println("2. Cập nhật thông tin cá nhân");
            System.out.println("3. Đổi mật khẩu");
            System.out.println("4. Xem công việc được giao");
            System.out.println("5. Cập nhật tiến độ công việc");
            System.out.println("6. Đánh dấu công việc hoàn thành");
            System.out.println("7. Ghi chú công việc");
            System.out.println("8. Xem dự án tham gia");
            System.out.println("0. Đăng xuất");
            String choice = prompt("Chọn: ");
            switch (choice) {
                case "1" -> showMe();
                case "2" -> updateProfile();
                case "3" -> changePassword();
                case "4" -> listMyTasks();
                case "5" -> updateTaskProgress();
                case "6" -> completeTask();
                case "7" -> addTaskNote();
                case "8" -> listProjects();
                case "0" -> logout();
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    private void managerMenu() throws Exception {
        while (token != null) {
            System.out.println();
            System.out.println("=== MENU QUẢN LÝ ===");
            System.out.println("1. Xem thông tin cá nhân");
            System.out.println("2. Đổi mật khẩu");
            System.out.println("3. Danh sách nhân viên");
            System.out.println("4. Duyệt tài khoản nhân viên mới");
            System.out.println("5. Khóa tài khoản nhân viên");
            System.out.println("6. Cập nhật thông tin dự án");
            System.out.println("7. Thông tin dự án");
            System.out.println("8. Cập nhật trạng thái dự án");
            System.out.println("9. Kiểm tra quyền xóa dự án");
            System.out.println("10. Phân công nhân viên vào dự án");
            System.out.println("11. Tạo công việc cho nhân viên");
            System.out.println("12. Danh sách tất cả công việc");
            System.out.println("0. Đăng xuất");
            String choice = prompt("Chọn: ");
            switch (choice) {
                case "1" -> showMe();
                case "2" -> changePassword();
                case "3" -> listEmployees();
                case "4" -> approveEmployee();
                case "5" -> disableEmployee();
                case "6" -> createProject();
                case "7" -> listProjects();
                case "8" -> updateProject();
                case "9" -> deleteProject();
                case "10" -> assignProjectMember();
                case "11" -> createTask();
                case "12" -> listAllTasks();
                case "0" -> logout();
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    private void showMe() throws Exception {
        Response response = send(Action.ME, token, Map.of());
        print(response);
        if (response.isSuccess()) {
            printEmployee((EmployeeView) response.getData());
        }
    }

    private void updateProfile() throws Exception {
        Response response = send(Action.UPDATE_PROFILE, token, map(
                "fullName", prompt("Họ tên mới: "),
                "email", prompt("Email mới: "),
                "phone", prompt("Số điện thoại mới: ")
        ));
        print(response);
    }

    private void changePassword() throws Exception {
        Response response = send(Action.CHANGE_PASSWORD, token, map(
                "oldPassword", prompt("Mật khẩu cũ: "),
                "newPassword", prompt("Mật khẩu mới: ")
        ));
        print(response);
    }

    private void listMyTasks() throws Exception {
        Response response = send(Action.MY_TASKS, token, Map.of());
        print(response);
        if (response.isSuccess()) {
            printTasks(castList(response.getData(), TaskView.class));
        }
    }

    private void updateTaskProgress() throws Exception {
        Response response = send(Action.UPDATE_TASK_PROGRESS, token, map(
                "taskId", readLong("Mã công việc: "),
                "progress", readInt("Tiến độ 0-100: ")
        ));
        print(response);
    }

    private void completeTask() throws Exception {
        Response response = send(Action.COMPLETE_TASK, token, map("taskId", readLong("Mã công việc: ")));
        print(response);
    }

    private void addTaskNote() throws Exception {
        Response response = send(Action.ADD_TASK_NOTE, token, map(
                "taskId", readLong("Mã công việc: "),
                "note", prompt("Ghi chú: ")
        ));
        print(response);
    }

    private void listEmployees() throws Exception {
        Response response = send(Action.LIST_EMPLOYEES, token, Map.of());
        print(response);
        if (response.isSuccess()) {
            castList(response.getData(), EmployeeView.class).forEach(this::printEmployee);
        }
    }

    private void approveEmployee() throws Exception {
        Response pending = send(Action.LIST_PENDING_EMPLOYEES, token, Map.of());
        print(pending);
        if (pending.isSuccess()) {
            castList(pending.getData(), EmployeeView.class).forEach(this::printEmployee);
        }
        Response response = send(Action.APPROVE_EMPLOYEE, token, map("employeeId", readLong("Mã nhân viên cần duyệt: ")));
        print(response);
    }

    private void disableEmployee() throws Exception {
        Response response = send(Action.DISABLE_EMPLOYEE, token, map("employeeId", readLong("Mã nhân viên cần khóa: ")));
        print(response);
    }

    private void createProject() throws Exception {
        Response response = send(Action.CREATE_PROJECT, token, map(
                "name", prompt("Tên dự án: "),
                "description", prompt("Mô tả: "),
                "startDate", readDate("Ngày bắt đầu (yyyy-MM-dd, bỏ trống nếu chưa có): "),
                "deadline", readDate("Deadline (yyyy-MM-dd, bỏ trống nếu chưa có): ")
        ));
        print(response);
    }

    private void listProjects() throws Exception {
        Response response = send(Action.LIST_PROJECTS, token, Map.of());
        print(response);
        if (response.isSuccess()) {
            castList(response.getData(), ProjectView.class).forEach(this::printProject);
        }
    }

    private void updateProject() throws Exception {
        Response response = send(Action.UPDATE_PROJECT, token, map(
                "projectId", readLong("Mã dự án: "),
                "name", prompt("Tên dự án mới: "),
                "description", prompt("Mô tả mới: "),
                "startDate", readDate("Ngày bắt đầu (yyyy-MM-dd, bỏ trống nếu chưa có): "),
                "deadline", readDate("Deadline (yyyy-MM-dd, bỏ trống nếu chưa có): "),
                "status", ProjectStatus.valueOf(prompt("Trạng thái (PLANNING, IN_PROGRESS, COMPLETED, CANCELLED): "))
        ));
        print(response);
    }

    private void deleteProject() throws Exception {
        Response response = send(Action.DELETE_PROJECT, token, map("projectId", readLong("Mã dự án cần xóa: ")));
        print(response);
    }

    private void assignProjectMember() throws Exception {
        Response response = send(Action.ASSIGN_PROJECT_MEMBER, token, map(
                "projectId", readLong("Mã dự án: "),
                "employeeId", readLong("Mã nhân viên: ")
        ));
        print(response);
    }

    private void createTask() throws Exception {
        Response response = send(Action.CREATE_TASK, token, map(
                "projectId", readLong("Mã dự án: "),
                "assigneeId", readLong("Mã nhân viên được giao: "),
                "title", prompt("Tên công việc: "),
                "description", prompt("Mô tả công việc: "),
                "startDate", readDate("Ngày bắt đầu (yyyy-MM-dd, bỏ trống nếu chưa có): "),
                "deadline", readDate("Deadline (yyyy-MM-dd, bỏ trống nếu chưa có): ")
        ));
        print(response);
    }

    private void listAllTasks() throws Exception {
        Response response = send(Action.LIST_TASKS, token, Map.of());
        print(response);
        if (response.isSuccess()) {
            printTasks(castList(response.getData(), TaskView.class));
        }
    }

    private void logout() throws Exception {
        print(send(Action.LOGOUT, token, Map.of()));
        token = null;
        currentUser = null;
    }

    private Response send(Action action, String token, Map<String, Serializable> data) throws Exception {
        output.writeObject(new Request(action, token, data));
        output.flush();
        return (Response) input.readObject();
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    private long readLong(String label) {
        return Long.parseLong(prompt(label));
    }

    private int readInt(String label) {
        return Integer.parseInt(prompt(label));
    }

    private LocalDate readDate(String label) {
        String value = prompt(label);
        return value.isBlank() ? null : LocalDate.parse(value);
    }

    private void print(Response response) {
        System.out.println((response.isSuccess() ? "[OK] " : "[LỖI] ") + response.getMessage());
    }

    private void printEmployee(EmployeeView employee) {
        System.out.printf("#%d | %s | %s | %s | %s | %s | %s%n",
                employee.id(), employee.username(), employee.fullName(), employee.email(),
                employee.phone(), employee.role(), employee.status());
    }

    private void printProject(ProjectView project) {
        System.out.printf("#%d | %s | %s -> %s | %s%n",
                project.id(), project.name(), project.startDate(), project.deadline(), project.status());
        System.out.println("  Mô tả: " + project.description());
        System.out.print("  Thành viên: ");
        if (project.members().isEmpty()) {
            System.out.println("(chưa có)");
        } else {
            System.out.println(project.members().stream()
                    .map(member -> "#" + member.id() + " " + member.fullName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
        }
    }

    private void printTasks(List<TaskView> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("Chưa có công việc.");
            return;
        }
        for (TaskView task : tasks) {
            System.out.printf("#%d | [%s] %s | %d%% | deadline %s | dự án: %s | NV: %s%n",
                    task.id(), task.status(), task.title(), task.progress(), task.deadline(),
                    task.projectName(), task.assigneeName());
            System.out.println("  Mô tả: " + task.description());
            System.out.println("  Ghi chú: " + (task.note() == null ? "" : task.note()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object data, Class<T> type) {
        return (List<T>) data;
    }

    private static Map<String, Serializable> map(Object... keyValues) {
        Map<String, Serializable> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], (Serializable) keyValues[i + 1]);
        }
        return map;
    }

    @Override
    public void close() throws Exception {
        scanner.close();
        input.close();
        output.close();
        socket.close();
    }
}
