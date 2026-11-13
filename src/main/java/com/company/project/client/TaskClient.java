package com.company.project.client;

import com.company.project.dto.AccountSummaryView;
import com.company.project.dto.EmployeeView;
import com.company.project.dto.LoginResult;
import com.company.project.dto.ProjectView;
import com.company.project.dto.StatusHistoryView;
import com.company.project.dto.TaskView;
import com.company.project.entity.LeaveReason;
import com.company.project.entity.ProjectStatus;
import com.company.project.entity.RemovalReason;
import com.company.project.entity.Role;
import com.company.project.entity.TaskPriority;
import com.company.project.network.Action;
import com.company.project.network.Request;
import com.company.project.network.Response;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Client giao diện console - dùng để kiểm thử nhanh các chức năng qua socket. */
public class TaskClient implements AutoCloseable {
    private final ServerConnection connection;
    private final Scanner scanner = new Scanner(System.in);
    private String token;
    private EmployeeView currentUser;

    public TaskClient(String host, int port) throws Exception {
        connection = new ServerConnection(host, port);
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 2026;
        try (TaskClient client = new TaskClient(host, port)) {
            client.run();
        }
    }

    private void run() {
        while (true) {
            System.out.println();
            System.out.println("=== ỨNG DỤNG QUẢN LÝ PHÂN CÔNG CÔNG VIỆC ===");
            System.out.println("1. Đăng nhập");
            System.out.println("2. Đăng ký tài khoản nhân viên mới");
            System.out.println("0. Thoát");
            switch (prompt("Chọn: ")) {
                case "1" -> login();
                case "2" -> register();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    private void login() {
        Response response = send(Action.LOGIN, map("username", prompt("Tên đăng nhập: "), "password", prompt("Mật khẩu: ")));
        print(response);
        if (!response.isSuccess()) {
            return;
        }
        LoginResult result = (LoginResult) response.getData();
        token = result.token();
        currentUser = result.employee();
        System.out.println("Xin chào " + currentUser.fullName() + " (" + currentUser.role().label() + ")");
        if (currentUser.role() == Role.EMPLOYEE) {
            employeeMenu();
        } else {
            staffMenu();
        }
    }

    private void register() {
        print(send(Action.REGISTER, map(
                "username", prompt("Tên đăng nhập: "),
                "password", prompt("Mật khẩu (>= 6 ký tự): "),
                "fullName", prompt("Họ tên: "),
                "email", prompt("Email: "),
                "phone", prompt("Số điện thoại: "))));
    }

    // ---------------------------------------------------------------- nhân viên

    private void employeeMenu() {
        while (token != null) {
            System.out.println();
            System.out.println("=== MENU NHÂN VIÊN ===");
            System.out.println("1. Xem thông tin cá nhân      2. Cập nhật thông tin      3. Đổi mật khẩu");
            System.out.println("4. Công việc của tôi          5. Cập nhật tiến độ        6. Check hoàn thành");
            System.out.println("7. Ghi chú công việc          8. Nộp kết quả             9. Dự án của tôi");
            System.out.println("0. Đăng xuất");
            String choice = prompt("Chọn: ");
            switch (choice) {
                case "1" -> showMe();
                case "2" -> updateProfile();
                case "3" -> changePassword();
                case "4" -> listTasks(Action.MY_TASKS);
                case "5" -> print(send(Action.UPDATE_TASK_PROGRESS, map("taskId", readLong("Mã công việc: "),
                        "progress", readInt("Tiến độ 0-100: "))));
                case "6" -> print(send(Action.COMPLETE_TASK, map("taskId", readLong("Mã công việc: "))));
                case "7" -> print(send(Action.ADD_TASK_NOTE, map("taskId", readLong("Mã công việc: "),
                        "note", prompt("Ghi chú: "))));
                case "8" -> print(send(Action.SUBMIT_TASK_RESULT, map("taskId", readLong("Mã công việc: "),
                        "resultText", prompt("Nội dung kết quả: "), "resultLink", prompt("Link (có thể bỏ trống): "))));
                case "9" -> listProjects();
                case "0" -> logout();
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    // ---------------------------------------------------------------- admin / quản lý

    private void staffMenu() {
        boolean admin = currentUser.role() == Role.ADMIN;
        while (token != null) {
            System.out.println();
            System.out.println(admin ? "=== MENU ADMIN ===" : "=== MENU QUẢN LÝ ===");
            System.out.println("-- Tài khoản --");
            System.out.println(" 1. Danh sách tài khoản   2. Thêm tài khoản       3. Duyệt đăng ký      4. Từ chối đăng ký");
            System.out.println(" 5. Khóa                  6. Mở khóa              7. Tạm dừng (nghỉ)    8. Kích hoạt lại");
            System.out.println(" 9. Chuyển giao công việc 10. Xóa tài khoản       11. Lịch sử trạng thái");
            System.out.println("-- Dự án --");
            System.out.println("12. Danh sách dự án      13. Tạo dự án           14. Sửa / đổi trạng thái 15. Xóa dự án");
            System.out.println("16. Thêm thành viên      17. Gỡ thành viên");
            System.out.println("-- Công việc --");
            System.out.println("18. Danh sách công việc  19. Giao việc           20. Chuyển giao 1 việc 21. Mở lại việc");
            System.out.println("22. Xóa công việc");
            System.out.println("-- Cá nhân --");
            System.out.println("23. Thông tin cá nhân    24. Đổi mật khẩu         0. Đăng xuất");
            String choice = prompt("Chọn: ");
            switch (choice) {
                case "1" -> listAccounts();
                case "2" -> print(send(Action.CREATE_ACCOUNT, map(
                        "role", admin && prompt("Vai trò (1 = Nhân viên, 2 = Quản lý): ").equals("2") ? Role.MANAGER : Role.EMPLOYEE,
                        "username", prompt("Tên đăng nhập: "),
                        "password", prompt("Mật khẩu ban đầu: "),
                        "fullName", prompt("Họ tên: "),
                        "email", prompt("Email: "),
                        "phone", prompt("Số điện thoại: "))));
                case "3" -> {
                    listPending();
                    print(send(Action.APPROVE_ACCOUNT, map("employeeId", readLong("Mã tài khoản cần duyệt: "))));
                }
                case "4" -> {
                    listPending();
                    print(send(Action.REJECT_ACCOUNT, map("employeeId", readLong("Mã tài khoản cần từ chối: "))));
                }
                case "5" -> print(send(Action.LOCK_ACCOUNT, map("employeeId", readLong("Mã tài khoản: "),
                        "reason", prompt("Lý do khóa: "))));
                case "6" -> print(send(Action.UNLOCK_ACCOUNT, map("employeeId", readLong("Mã tài khoản: "))));
                case "7" -> suspend();
                case "8" -> print(send(Action.REACTIVATE_ACCOUNT, map("employeeId", readLong("Mã tài khoản: "))));
                case "9" -> print(send(Action.TRANSFER_OPEN_TASKS, map("fromEmployeeId", readLong("Chuyển từ nhân viên (mã): "),
                        "toEmployeeId", readLong("Sang nhân viên (mã): "))));
                case "10" -> deleteAccount();
                case "11" -> statusHistory();
                case "12" -> listProjects();
                case "13" -> print(send(Action.CREATE_PROJECT, map(
                        "name", prompt("Tên dự án: "),
                        "description", prompt("Mô tả: "),
                        "startDate", readDate("Ngày bắt đầu (yyyy-MM-dd, bỏ trống nếu chưa có): "),
                        "deadline", readDate("Deadline (yyyy-MM-dd, bỏ trống nếu chưa có): "),
                        "managerId", admin ? readOptionalLong("Mã quản lý phụ trách (bỏ trống = bạn): ") : null)));
                case "14" -> updateProject();
                case "15" -> print(send(Action.DELETE_PROJECT, map("projectId", readLong("Mã dự án cần xóa: "))));
                case "16" -> print(send(Action.ADD_PROJECT_MEMBER, map("projectId", readLong("Mã dự án: "),
                        "employeeId", readLong("Mã nhân viên: "))));
                case "17" -> print(send(Action.REMOVE_PROJECT_MEMBER, map("projectId", readLong("Mã dự án: "),
                        "employeeId", readLong("Mã nhân viên: "))));
                case "18" -> listTasks(Action.LIST_TASKS);
                case "19" -> print(send(Action.CREATE_TASK, map(
                        "projectId", readLong("Mã dự án: "),
                        "assigneeId", readLong("Mã nhân viên thực hiện: "),
                        "title", prompt("Tên công việc: "),
                        "description", prompt("Mô tả: "),
                        "startDate", readDate("Ngày bắt đầu (yyyy-MM-dd): "),
                        "deadline", readDate("Deadline (yyyy-MM-dd): "),
                        "priority", readPriority())));
                case "20" -> print(send(Action.TRANSFER_TASK, map("taskId", readLong("Mã công việc: "),
                        "assigneeId", readLong("Chuyển cho nhân viên (mã): "))));
                case "21" -> print(send(Action.REOPEN_TASK, map("taskId", readLong("Mã công việc: "))));
                case "22" -> print(send(Action.DELETE_TASK, map("taskId", readLong("Mã công việc: "))));
                case "23" -> showMe();
                case "24" -> changePassword();
                case "0" -> logout();
                default -> System.out.println("Lựa chọn không hợp lệ.");
            }
        }
    }

    private void suspend() {
        long id = readLong("Mã tài khoản cần tạm dừng: ");
        LeaveReason[] reasons = LeaveReason.values();
        for (int i = 0; i < reasons.length; i++) {
            System.out.println("  " + (i + 1) + ". " + reasons[i].label());
        }
        LeaveReason reason = reasons[Math.max(0, Math.min(reasons.length - 1, readInt("Lý do: ") - 1))];
        boolean transfer = prompt("Công việc đang làm: 1 = giữ lại & tạm hoãn, 2 = chuyển giao: ").equals("2");
        print(send(Action.SUSPEND_ACCOUNT, map(
                "employeeId", id,
                "reason", reason,
                "from", readDate("Từ ngày (yyyy-MM-dd, bỏ trống = hôm nay): "),
                "until", readDate("Dự kiến quay lại (yyyy-MM-dd): "),
                "note", prompt("Ghi chú: "),
                "transferTasks", transfer,
                "transferToId", transfer ? readLong("Chuyển cho nhân viên (mã): ") : null)));
    }

    private void deleteAccount() {
        long id = readLong("Mã tài khoản cần xóa: ");
        RemovalReason[] reasons = RemovalReason.values();
        for (int i = 0; i < reasons.length; i++) {
            System.out.println("  " + (i + 1) + ". " + reasons[i].label()
                    + (reasons[i].isSoftDelete() ? " (giữ lịch sử)" : " (xóa hẳn)"));
        }
        RemovalReason reason = reasons[Math.max(0, Math.min(reasons.length - 1, readInt("Lý do: ") - 1))];
        print(send(Action.DELETE_ACCOUNT, map("employeeId", id, "reason", reason, "note", prompt("Ghi chú: "))));
    }

    private void updateProject() {
        long id = readLong("Mã dự án: ");
        ProjectStatus[] statuses = ProjectStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            System.out.println("  " + (i + 1) + ". " + statuses[i].label());
        }
        ProjectStatus status = statuses[Math.max(0, Math.min(statuses.length - 1, readInt("Trạng thái: ") - 1))];
        Map<String, Serializable> data = map(
                "projectId", id,
                "name", prompt("Tên dự án: "),
                "description", prompt("Mô tả: "),
                "startDate", readDate("Ngày bắt đầu (yyyy-MM-dd): "),
                "deadline", readDate("Deadline (yyyy-MM-dd): "),
                "status", status);
        Response response = send(Action.UPDATE_PROJECT, data);
        if (!response.isSuccess() && response.getMessage().contains("xác nhận")
                && prompt(response.getMessage() + " (y/n): ").equalsIgnoreCase("y")) {
            data.put("confirmIncomplete", true);
            response = send(Action.UPDATE_PROJECT, data);
        }
        print(response);
    }

    // ---------------------------------------------------------------- hiển thị

    private void showMe() {
        Response response = send(Action.ME, Map.of());
        print(response);
        if (response.isSuccess()) {
            printEmployee((EmployeeView) response.getData());
        }
    }

    private void updateProfile() {
        print(send(Action.UPDATE_PROFILE, map("fullName", prompt("Họ tên mới: "), "email", prompt("Email mới: "),
                "phone", prompt("Số điện thoại mới: "))));
    }

    private void changePassword() {
        print(send(Action.CHANGE_PASSWORD, map("oldPassword", prompt("Mật khẩu cũ: "), "newPassword", prompt("Mật khẩu mới: "))));
    }

    private void listAccounts() {
        Response response = send(Action.LIST_ACCOUNTS, Map.of());
        print(response);
        if (response.isSuccess()) {
            List<AccountSummaryView> rows = castList(response.getData());
            for (AccountSummaryView row : rows) {
                printEmployee(row.employee());
                System.out.printf("      công việc: %d chưa xong, %d hoàn thành | dự án phụ trách: %d%n",
                        row.openTasks(), row.completedTasks(), row.managedProjects());
            }
        }
    }

    private void listPending() {
        Response response = send(Action.LIST_PENDING_ACCOUNTS, Map.of());
        if (response.isSuccess()) {
            List<EmployeeView> pending = castList(response.getData());
            System.out.println(pending.isEmpty() ? "(Không có đăng ký nào chờ duyệt)" : "Đang chờ duyệt:");
            pending.forEach(this::printEmployee);
        }
    }

    private void statusHistory() {
        Response response = send(Action.LIST_STATUS_HISTORY, map("employeeId", readLong("Mã tài khoản: ")));
        print(response);
        if (response.isSuccess()) {
            List<StatusHistoryView> history = castList(response.getData());
            for (StatusHistoryView h : history) {
                System.out.printf("  %s | %s -> %s | %s | %s%n", h.changedAt(),
                        h.fromStatus() == null ? "(mới)" : h.fromStatus().label(), h.toStatus().label(),
                        h.reason(), h.changedByName());
            }
        }
    }

    private void listProjects() {
        Response response = send(Action.LIST_PROJECTS, Map.of());
        print(response);
        if (!response.isSuccess()) {
            return;
        }
        List<ProjectView> projects = castList(response.getData());
        for (ProjectView p : projects) {
            System.out.printf("#%d | %s | QL: %s | %s -> %s | %s | %d/%d việc (%d%%)%n", p.id(), p.name(),
                    p.managerName(), p.startDate(), p.deadline(), p.status().label(), p.completedTasks(),
                    p.totalTasks(), p.progress());
            System.out.println("    Thành viên: " + (p.members().isEmpty() ? "(chưa có)" : p.members().stream()
                    .map(m -> "#" + m.id() + " " + m.fullName()).reduce((a, b) -> a + ", " + b).orElse("")));
        }
    }

    private void listTasks(Action action) {
        Response response = send(action, Map.of());
        print(response);
        if (!response.isSuccess()) {
            return;
        }
        List<TaskView> tasks = castList(response.getData());
        if (tasks.isEmpty()) {
            System.out.println("Chưa có công việc.");
        }
        for (TaskView t : tasks) {
            System.out.printf("#%d | [%s%s] %s | %d%% | deadline %s | dự án: %s | NV: %s%n", t.id(), t.status().label(),
                    t.overdue() ? ", QUÁ HẠN" : "", t.title(), t.progress(), t.deadline(), t.projectName(), t.assigneeName());
            if (t.note() != null && !t.note().isBlank()) {
                System.out.println("    Ghi chú: " + t.note());
            }
        }
    }

    private void printEmployee(EmployeeView e) {
        System.out.printf("#%d | %s | %s | %s | %s | %s%s%n", e.id(), e.username(), e.fullName(),
                e.email(), e.role().label(), e.status().label(),
                e.statusReason() == null ? "" : " (" + e.statusReason() + ")");
    }

    private void logout() {
        print(send(Action.LOGOUT, Map.of()));
        token = null;
        currentUser = null;
    }

    // ---------------------------------------------------------------- tiện ích

    private Response send(Action action, Map<String, Serializable> data) {
        Response response = connection.send(new Request(action, token, data));
        if (response.isSessionExpired()) {
            token = null;
            currentUser = null;
        }
        return response;
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "0";
    }

    private long readLong(String label) {
        while (true) {
            try {
                return Long.parseLong(prompt(label));
            } catch (NumberFormatException e) {
                System.out.println("Hãy nhập số.");
            }
        }
    }

    private Long readOptionalLong(String label) {
        String value = prompt(label);
        return value.isBlank() ? null : Long.parseLong(value);
    }

    private int readInt(String label) {
        while (true) {
            try {
                return Integer.parseInt(prompt(label));
            } catch (NumberFormatException e) {
                System.out.println("Hãy nhập số nguyên.");
            }
        }
    }

    private LocalDate readDate(String label) {
        while (true) {
            String value = prompt(label);
            try {
                return value.isBlank() ? null : LocalDate.parse(value);
            } catch (Exception e) {
                System.out.println("Ngày không hợp lệ, nhập theo dạng yyyy-MM-dd.");
            }
        }
    }

    private TaskPriority readPriority() {
        TaskPriority[] values = TaskPriority.values();
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i].label());
        }
        return values[Math.max(0, Math.min(values.length - 1, readInt("Mức ưu tiên: ") - 1))];
    }

    private void print(Response response) {
        System.out.println((response.isSuccess() ? "[OK] " : "[LỖI] ") + response.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object data) {
        return data == null ? List.of() : (List<T>) data;
    }

    private static Map<String, Serializable> map(Object... keyValues) {
        Map<String, Serializable> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                map.put((String) keyValues[i], (Serializable) keyValues[i + 1]);
            }
        }
        return map;
    }

    @Override
    public void close() {
        scanner.close();
        connection.close();
    }
}
