package com.company.project;

import com.company.project.client.ServerConnection;
import com.company.project.dto.AccountSummaryView;
import com.company.project.dto.EmployeeView;
import com.company.project.dto.LoginResult;
import com.company.project.dto.ProjectView;
import com.company.project.dto.StatusHistoryView;
import com.company.project.dto.TaskView;
import com.company.project.entity.AccountStatus;
import com.company.project.entity.LeaveReason;
import com.company.project.entity.ProjectStatus;
import com.company.project.entity.RemovalReason;
import com.company.project.entity.Role;
import com.company.project.entity.TaskPriority;
import com.company.project.entity.TaskStatus;
import com.company.project.network.Action;
import com.company.project.network.Request;
import com.company.project.network.Response;
import com.company.project.server.TaskServer;
import com.company.project.util.EntityManagerFactoryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kiểm thử tích hợp toàn bộ quy trình qua Socket.
 * - Tự khởi động TaskServer ở cổng 2099 với CSDL riêng "project_task_test" (không ảnh hưởng dữ liệu thật).
 * - Mỗi kịch bản là một test case (TC001, TC002, ...) hiển thị riêng trong báo cáo JUnit.
 * - Kết quả được ghi ra target/test-report/testcases.tsv để lập file Excel test case.
 * Chạy: mvn test
 */
class WorkflowTest {
    private static final int PORT = 2099;
    private static final String TEST_DB = "project_task_test";

    private record TestCase(String id, String group, String precondition, String role, String action,
                            String expected, String actual, boolean passed) {
    }

    private static final List<TestCase> CASES = new ArrayList<>();
    private static String group = "";
    private static String precondition = "";

    // ---------------------------------------------------------------- môi trường test

    @BeforeAll
    static void startServer() throws Exception {
        dropTestDatabase();
        System.setProperty("TASKFLOW_DB_URL", "jdbc:mariadb://localhost:3306/" + TEST_DB
                + "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8");
        Thread server = new Thread(() -> {
            try {
                TaskServer.main(new String[]{String.valueOf(PORT)});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "test-server");
        server.setDaemon(true);
        server.start();
        long deadline = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < deadline) {
            try (Socket ignored = new Socket("localhost", PORT)) {
                return;
            } catch (IOException e) {
                Thread.sleep(300);
            }
        }
        throw new IllegalStateException("Server test không khởi động được ở cổng " + PORT);
    }

    @AfterAll
    static void cleanUp() throws Exception {
        writeReport();
        EntityManagerFactoryUtil.close();
        dropTestDatabase();
    }

    private static void dropTestDatabase() throws Exception {
        String user = setting("TASKFLOW_DB_USER", "root");
        String password = setting("TASKFLOW_DB_PASSWORD", "root123");
        try (Connection connection = DriverManager.getConnection("jdbc:mariadb://localhost:3306/", user, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS " + TEST_DB);
        }
    }

    private static String setting(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void writeReport() throws IOException {
        Path dir = Path.of("target", "test-report");
        Files.createDirectories(dir);
        StringBuilder out = new StringBuilder("id\tgroup\tprecondition\trole\taction\texpected\tactual\tstatus\n");
        for (TestCase c : CASES) {
            out.append(String.join("\t", c.id(), c.group(), c.precondition(), c.role(), c.action(), c.expected(),
                    clean(c.actual()), c.passed() ? "PASS" : "FAIL")).append('\n');
        }
        Files.writeString(dir.resolve("testcases.tsv"), out.toString(), StandardCharsets.UTF_8);
    }

    private static String clean(String text) {
        return text == null ? "" : text.replace('\t', ' ').replace('\n', ' ');
    }

    // ---------------------------------------------------------------- tiện ích kiểm thử

    /** Một client kết nối tới server test, đại diện cho một người dùng. */
    private static final class User {
        final String label;
        final ServerConnection connection;
        String token;

        User(String label) throws IOException {
            this.label = label;
            this.connection = new ServerConnection("localhost", PORT);
        }

        Response send(Action action, Object... keyValues) {
            Map<String, Serializable> data = new HashMap<>();
            for (int i = 0; i < keyValues.length; i += 2) {
                if (keyValues[i + 1] != null) {
                    data.put((String) keyValues[i], (Serializable) keyValues[i + 1]);
                }
            }
            return connection.send(new Request(action, token, data));
        }

        Response login(String username, String password) {
            token = null;
            Response response = send(Action.LOGIN, "username", username, "password", password);
            if (response.isSuccess()) {
                token = ((LoginResult) response.getData()).token();
            }
            return response;
        }
    }

    private static void section(String name, String pre) {
        group = name;
        precondition = pre;
    }

    private static void record(String role, String action, String expected, String actual, boolean passed) {
        String id = String.format("TC%03d", CASES.size() + 1);
        CASES.add(new TestCase(id, group, precondition, role, action, expected, actual, passed));
    }

    /** Mong đợi thao tác thành công. */
    private static void ok(User user, String action, Response response) {
        record(user.label, action, "Thành công", response.getMessage(), response.isSuccess());
    }

    /** Mong đợi server từ chối, thông báo có chứa đoạn chữ chỉ định. */
    private static void rejected(User user, String action, Response response, String messagePart) {
        String expected = "Bị từ chối" + (messagePart == null ? "" : ", thông báo chứa: \"" + messagePart + "\"");
        record(user.label, action, expected, response.getMessage(),
                !response.isSuccess() && (messagePart == null || response.getMessage().contains(messagePart)));
    }

    /** Kiểm tra dữ liệu sau thao tác. */
    private static void verify(String role, String action, String expected, boolean condition, String actual) {
        record(role, action, expected, condition ? "Đúng như mong đợi" + (actual.isBlank() ? "" : " (" + actual + ")")
                : "Sai: " + actual, condition);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> list(Response response) {
        return response.getData() == null ? List.of() : (List<T>) response.getData();
    }

    private static Map<String, EmployeeView> accounts(User admin) {
        Map<String, EmployeeView> map = new HashMap<>();
        for (AccountSummaryView row : WorkflowTest.<AccountSummaryView>list(admin.send(Action.LIST_ACCOUNTS))) {
            map.put(row.employee().username(), row.employee());
        }
        return map;
    }

    private static long id(User admin, String username) {
        return accounts(admin).get(username).id();
    }

    private static TaskView task(User user, String title) {
        return WorkflowTest.<TaskView>list(user.send(Action.LIST_TASKS)).stream()
                .filter(t -> t.title().equals(title)).findFirst().orElse(null);
    }

    private static ProjectView project(User user, String name) {
        return WorkflowTest.<ProjectView>list(user.send(Action.LIST_PROJECTS)).stream()
                .filter(p -> p.name().equals(name)).findFirst().orElse(null);
    }

    // ---------------------------------------------------------------- kịch bản

    @TestFactory
    List<DynamicTest> workflow() throws Exception {
        runScenarios();
        return CASES.stream()
                .map(c -> DynamicTest.dynamicTest(c.id() + " [" + c.group() + "] " + c.action(),
                        () -> Assertions.assertTrue(c.passed(), c.actual())))
                .toList();
    }

    private void runScenarios() throws Exception {
        User admin = new User("Admin");
        User lan = new User("Quản lý (quanly01)");
        User nam = new User("Quản lý (quanly02)");
        User guest = new User("Khách / người dùng mới");
        User newEmployee = new User("Nhân viên mới (test_nv)");
        User minh = new User("Nhân viên (minhdev)");
        User an = new User("Nhân viên (nhanvien01)");

        // ============================================================ 1. Đăng nhập
        section("1. Đăng nhập", "Server đang chạy, CSDL có dữ liệu mẫu");
        ok(admin, "Đăng nhập admin / admin123", admin.login("admin", "admin123"));
        rejected(guest, "Đăng nhập admin với mật khẩu sai", guest.login("admin", "sai"), "Sai tên đăng nhập");
        rejected(guest, "Đăng nhập tài khoản đang chờ duyệt (nhanvien_moi)", guest.login("nhanvien_moi", "123456"), "chờ quản lý duyệt");
        rejected(guest, "Đăng nhập tài khoản đang tạm dừng - nghỉ thai sản (thuha)", guest.login("thuha", "123456"), "tạm dừng");
        ok(lan, "Đăng nhập quản lý quanly01", lan.login("quanly01", "123456"));
        ok(nam, "Đăng nhập quản lý quanly02", nam.login("quanly02", "123456"));
        verify("Admin", "Kiểm tra vai trò của tài khoản admin", "Vai trò = Quản trị viên (ADMIN)",
                accounts(admin).get("admin").role() == Role.ADMIN, "");

        // ============================================================ 2. Đăng ký và duyệt
        section("2. Đăng ký & duyệt tài khoản", "Chưa có tài khoản test_nv, test_reject");
        ok(guest, "Nhân viên mới tự đăng ký (test_nv)", guest.send(Action.REGISTER, "username", "test_nv",
                "password", "123456", "fullName", "Test NV"));
        rejected(guest, "Đăng ký trùng tên đăng nhập", guest.send(Action.REGISTER, "username", "test_nv",
                "password", "123456", "fullName", "X"), "đã tồn tại");
        rejected(guest, "Đăng ký với mật khẩu dưới 6 ký tự", guest.send(Action.REGISTER, "username", "abc",
                "password", "123", "fullName", "X"), "ít nhất 6");
        rejected(guest, "Đăng nhập khi tài khoản chưa được duyệt", guest.login("test_nv", "123456"), "chờ");
        List<AccountSummaryView> lanAccounts = list(lan.send(Action.LIST_ACCOUNTS));
        verify(lan.label, "Quản lý xem danh sách tài khoản", "Chỉ thấy tài khoản nhân viên",
                lanAccounts.stream().allMatch(a -> a.employee().role() == Role.EMPLOYEE), lanAccounts.size() + " tài khoản");
        long testNv = id(admin, "test_nv");
        ok(lan, "Quản lý duyệt đăng ký của test_nv", lan.send(Action.APPROVE_ACCOUNT, "employeeId", testNv));
        ok(newEmployee, "Nhân viên vừa được duyệt đăng nhập", newEmployee.login("test_nv", "123456"));
        ok(guest, "Đăng ký tài khoản test_reject", guest.send(Action.REGISTER, "username", "test_reject",
                "password", "123456", "fullName", "Bị Từ Chối"));
        ok(lan, "Quản lý từ chối đăng ký của test_reject", lan.send(Action.REJECT_ACCOUNT, "employeeId", id(admin, "test_reject")));
        verify("Admin", "Kiểm tra bản đăng ký bị từ chối", "Tài khoản test_reject bị xóa khỏi hệ thống",
                !accounts(admin).containsKey("test_reject"), "");
        rejected(newEmployee, "Nhân viên gọi chức năng xem danh sách tài khoản", newEmployee.send(Action.LIST_ACCOUNTS),
                "chỉ dành cho quản lý");

        // ============================================================ 3. Phân quyền tài khoản
        section("3. Phân quyền tạo / sửa / khóa tài khoản", "Đã đăng nhập Admin và quản lý quanly01");
        rejected(lan, "Quản lý tạo tài khoản quản lý", lan.send(Action.CREATE_ACCOUNT, "username", "ql_x",
                "password", "123456", "fullName", "X", "role", Role.MANAGER), "chỉ được tạo tài khoản nhân viên");
        rejected(admin, "Admin tạo thêm một tài khoản Admin", admin.send(Action.CREATE_ACCOUNT, "username", "ad_x",
                "password", "123456", "fullName", "X", "role", Role.ADMIN), "một tài khoản Admin");
        ok(admin, "Admin tạo tài khoản quản lý quanly03", admin.send(Action.CREATE_ACCOUNT, "username", "quanly03",
                "password", "123456", "fullName", "Quản Lý Ba", "role", Role.MANAGER));
        ok(lan, "Quản lý tạo tài khoản nhân viên (nham)", lan.send(Action.CREATE_ACCOUNT, "username", "nham",
                "password", "123456", "fullName", "Tạo Nhầm", "role", Role.EMPLOYEE));
        rejected(lan, "Quản lý đổi vai trò nhân viên thành quản lý", lan.send(Action.UPDATE_ACCOUNT,
                "employeeId", id(admin, "nhanvien01"), "fullName", "Nguyễn Minh An", "role", Role.MANAGER), "Chỉ Admin");
        rejected(lan, "Quản lý khóa tài khoản quản lý khác", lan.send(Action.LOCK_ACCOUNT,
                "employeeId", id(admin, "quanly02"), "reason", "x"), "Quản lý chỉ được");
        rejected(lan, "Quản lý khóa tài khoản Admin", lan.send(Action.LOCK_ACCOUNT,
                "employeeId", id(admin, "admin"), "reason", "x"), null);
        rejected(admin, "Admin tự khóa tài khoản của mình", admin.send(Action.LOCK_ACCOUNT,
                "employeeId", id(admin, "admin"), "reason", "x"), "chính tài khoản");
        rejected(lan, "Khóa tài khoản nhưng không nhập lý do", lan.send(Action.LOCK_ACCOUNT,
                "employeeId", id(admin, "nham"), "reason", ""), "Lý do khóa");
        ok(lan, "Quản lý khóa nhân viên, lý do: nghi lộ mật khẩu", lan.send(Action.LOCK_ACCOUNT,
                "employeeId", id(admin, "nham"), "reason", "Nghi lộ mật khẩu"));
        rejected(guest, "Đăng nhập khi tài khoản bị khóa", guest.login("nham", "123456"), "đã bị khóa");
        ok(lan, "Quản lý mở khóa tài khoản", lan.send(Action.UNLOCK_ACCOUNT, "employeeId", id(admin, "nham")));

        // ============================================================ 4. Dự án
        section("4. Quản lý dự án & thành viên", "quanly01 phụ trách dự án nội bộ, quanly02 phụ trách dự án Website");
        verify(lan.label, "Quản lý xem danh sách dự án", "Chỉ thấy dự án mình phụ trách",
                WorkflowTest.<ProjectView>list(lan.send(Action.LIST_PROJECTS)).stream()
                        .allMatch(p -> p.managerName().equals("Trần Thị Lan")), "");
        ProjectView website = project(admin, "Website giới thiệu công ty");
        rejected(lan, "Sửa dự án do quản lý khác phụ trách", lan.send(Action.UPDATE_PROJECT, "projectId", website.id(),
                "name", "Hack", "status", ProjectStatus.PLANNING), "không phụ trách");
        rejected(lan, "Xóa dự án do quản lý khác phụ trách", lan.send(Action.DELETE_PROJECT, "projectId", website.id()),
                "không phụ trách");
        rejected(lan, "Tạo dự án có deadline trước ngày bắt đầu", lan.send(Action.CREATE_PROJECT, "name", "Sai",
                "startDate", LocalDate.now(), "deadline", LocalDate.now().minusDays(1)), "Deadline");
        ok(lan, "Tạo dự án mới \"DA Test\"", lan.send(Action.CREATE_PROJECT, "name", "DA Test", "description", "test",
                "startDate", LocalDate.now(), "deadline", LocalDate.now().plusDays(30)));
        ProjectView daTest = project(lan, "DA Test");
        verify(lan.label, "Kiểm tra dự án vừa tạo", "Trạng thái Lên kế hoạch, quản lý phụ trách là người tạo",
                daTest != null && daTest.status() == ProjectStatus.PLANNING && daTest.managerName().equals("Trần Thị Lan"), "");
        ok(admin, "Admin tạo dự án và giao cho quanly02 phụ trách", admin.send(Action.CREATE_PROJECT, "name", "DA Admin",
                "managerId", id(admin, "quanly02")));
        verify(nam.label, "quanly02 xem danh sách dự án", "Thấy dự án Admin vừa giao", project(nam, "DA Admin") != null, "");
        ok(lan, "Thêm test_nv vào dự án", lan.send(Action.ADD_PROJECT_MEMBER, "projectId", daTest.id(), "employeeId", testNv));
        rejected(lan, "Thêm trùng thành viên đã có", lan.send(Action.ADD_PROJECT_MEMBER, "projectId", daTest.id(),
                "employeeId", testNv), "đã là thành viên");
        rejected(lan, "Thêm nhân viên đang tạm dừng vào dự án", lan.send(Action.ADD_PROJECT_MEMBER, "projectId", daTest.id(),
                "employeeId", id(admin, "thuha")), "Tạm dừng");

        // ============================================================ 5. Giao việc
        section("5. Giao việc", "Dự án DA Test có thành viên test_nv");
        ok(lan, "Giao việc cho thành viên dự án", lan.send(Action.CREATE_TASK, "projectId", daTest.id(), "assigneeId", testNv,
                "title", "Việc test 1", "deadline", LocalDate.now().plusDays(5), "priority", TaskPriority.HIGH));
        rejected(lan, "Giao việc cho người chưa là thành viên dự án", lan.send(Action.CREATE_TASK, "projectId", daTest.id(),
                "assigneeId", id(admin, "minhdev"), "title", "X"), "chưa là thành viên");
        rejected(lan, "Giao việc trong dự án không phụ trách", lan.send(Action.CREATE_TASK, "projectId", website.id(),
                "assigneeId", id(admin, "minhdev"), "title", "X"), "không phụ trách");

        // ============================================================ 6. Nhân viên làm việc
        section("6. Nhân viên cập nhật công việc", "test_nv được giao \"Việc test 1\"");
        TaskView t1 = task(lan, "Việc test 1");
        ok(newEmployee, "Xem danh sách công việc của tôi", newEmployee.send(Action.MY_TASKS));
        verify(newEmployee.label, "Xem dự án của tôi", "Chỉ thấy dự án mình tham gia (DA Test)",
                WorkflowTest.<ProjectView>list(newEmployee.send(Action.LIST_PROJECTS)).stream()
                        .map(ProjectView::name).toList().equals(List.of("DA Test")), "");
        ok(newEmployee, "Cập nhật tiến độ 50%", newEmployee.send(Action.UPDATE_TASK_PROGRESS, "taskId", t1.id(), "progress", 50));
        rejected(newEmployee, "Cập nhật tiến độ 150% (ngoài khoảng 0-100)", newEmployee.send(Action.UPDATE_TASK_PROGRESS,
                "taskId", t1.id(), "progress", 150), "0 - 100");
        ok(newEmployee, "Ghi chú trên công việc đang làm", newEmployee.send(Action.ADD_TASK_NOTE, "taskId", t1.id(), "note", "Đang làm"));
        ok(newEmployee, "Check hoàn thành công việc", newEmployee.send(Action.COMPLETE_TASK, "taskId", t1.id()));
        ok(minh, "Đăng nhập minhdev", minh.login("minhdev", "123456"));
        rejected(minh, "Cập nhật công việc của người khác", minh.send(Action.UPDATE_TASK_PROGRESS, "taskId", t1.id(),
                "progress", 10), "không được giao cho bạn");
        ok(lan, "Quản lý mở lại công việc đã hoàn thành", lan.send(Action.REOPEN_TASK, "taskId", t1.id()));
        TaskView reopened = task(lan, "Việc test 1");
        verify(lan.label, "Kiểm tra công việc vừa mở lại", "Trạng thái Đang thực hiện, tiến độ ≤ 90%",
                reopened.status() == TaskStatus.IN_PROGRESS && reopened.progress() <= 90, reopened.progress() + "%");
        rejected(lan, "Gỡ thành viên còn việc chưa hoàn thành khỏi dự án", lan.send(Action.REMOVE_PROJECT_MEMBER,
                "projectId", daTest.id(), "employeeId", testNv), "chưa hoàn thành");
        ok(lan, "Quản lý sửa công việc (mô tả, mức ưu tiên)", lan.send(Action.UPDATE_TASK, "taskId", t1.id(),
                "title", "Việc test 1", "description", "đã sửa", "priority", TaskPriority.URGENT));

        // ============================================================ 7. Tạm dừng
        section("7. Tạm dừng / kích hoạt lại tài khoản", "nhanvien01 đang có 2 công việc chưa hoàn thành");
        ok(an, "Đăng nhập nhanvien01", an.login("nhanvien01", "123456"));
        long anId = id(admin, "nhanvien01");
        rejected(lan, "Tạm dừng nhưng không nhập ngày dự kiến quay lại", lan.send(Action.SUSPEND_ACCOUNT,
                "employeeId", anId, "reason", LeaveReason.SICK_LEAVE), "ngày dự kiến quay lại");
        ok(lan, "Tạm dừng nhanvien01 (nghỉ ốm 14 ngày, giữ lại công việc)", lan.send(Action.SUSPEND_ACCOUNT,
                "employeeId", anId, "reason", LeaveReason.SICK_LEAVE, "until", LocalDate.now().plusDays(14), "transferTasks", false));
        Response me = an.send(Action.ME);
        verify(an.label, "Thao tác tiếp khi vừa bị tạm dừng", "Phiên đăng nhập bị hủy ngay",
                !me.isSuccess() && me.isSessionExpired(), me.getMessage());
        rejected(an, "Đăng nhập khi đang tạm dừng", an.login("nhanvien01", "123456"), "Nghỉ ốm");
        long onHold = WorkflowTest.<TaskView>list(admin.send(Action.LIST_TASKS)).stream()
                .filter(t -> t.assigneeName().equals("Nguyễn Minh An") && t.status() == TaskStatus.ON_HOLD).count();
        verify("Admin", "Kiểm tra công việc của người tạm dừng", "Công việc chưa xong chuyển sang Tạm hoãn",
                onHold == 2, onHold + " việc tạm hoãn");
        rejected(lan, "Giao việc cho nhân viên đang tạm dừng", lan.send(Action.CREATE_TASK,
                "projectId", project(lan, "Hệ thống quản lý công việc nội bộ").id(), "assigneeId", anId, "title", "X"), "Tạm dừng");
        ok(lan, "Kích hoạt lại nhanvien01 (quay lại làm việc)", lan.send(Action.REACTIVATE_ACCOUNT, "employeeId", anId));
        long stillOnHold = WorkflowTest.<TaskView>list(admin.send(Action.LIST_TASKS)).stream()
                .filter(t -> t.assigneeName().equals("Nguyễn Minh An") && t.status() == TaskStatus.ON_HOLD).count();
        verify("Admin", "Kiểm tra công việc sau khi kích hoạt lại", "Công việc tạm hoãn được tiếp tục",
                stillOnHold == 0, stillOnHold + " việc còn tạm hoãn");
        long minhId = id(admin, "minhdev");
        rejected(lan, "Tạm dừng + chuyển giao việc thuộc dự án không phụ trách", lan.send(Action.SUSPEND_ACCOUNT,
                "employeeId", minhId, "reason", LeaveReason.LONG_VACATION, "until", LocalDate.now().plusDays(20),
                "transferTasks", true, "transferToId", anId), "không phụ trách");
        ok(admin, "Admin tạm dừng minhdev và chuyển giao việc cho nhanvien01", admin.send(Action.SUSPEND_ACCOUNT,
                "employeeId", minhId, "reason", LeaveReason.LONG_VACATION, "until", LocalDate.now().plusDays(20),
                "transferTasks", true, "transferToId", anId));
        verify("Admin", "Kiểm tra người nhận chuyển giao", "nhanvien01 được thêm vào dự án Website",
                project(admin, "Website giới thiệu công ty").members().stream().anyMatch(m -> m.username().equals("nhanvien01")), "");
        ok(admin, "Admin kích hoạt lại minhdev", admin.send(Action.REACTIVATE_ACCOUNT, "employeeId", minhId));

        // ============================================================ 8. Xóa tài khoản
        section("8. Xóa tài khoản", "test_nv còn 1 việc chưa xong; tài khoản nham chưa có công việc");
        rejected(lan, "Xóa nhân viên còn công việc chưa hoàn thành", lan.send(Action.DELETE_ACCOUNT, "employeeId", testNv,
                "reason", RemovalReason.RESIGNED), "chuyển giao");
        rejected(lan, "Xóa tài khoản nhưng không chọn lý do", lan.send(Action.DELETE_ACCOUNT, "employeeId", testNv), "lý do");
        ok(lan, "Chuyển toàn bộ việc của test_nv cho nhanvien01", lan.send(Action.TRANSFER_OPEN_TASKS,
                "fromEmployeeId", testNv, "toEmployeeId", anId));
        ok(lan, "Xóa mềm test_nv - lý do Nghỉ việc", lan.send(Action.DELETE_ACCOUNT, "employeeId", testNv,
                "reason", RemovalReason.RESIGNED, "note", "Nghỉ từ hôm nay"));
        verify("Admin", "Kiểm tra tài khoản sau khi xóa mềm", "Trạng thái Đã nghỉ việc, được gỡ khỏi dự án",
                accounts(admin).get("test_nv").status() == AccountStatus.RESIGNED
                        && project(lan, "DA Test").members().stream().noneMatch(m -> m.username().equals("test_nv")), "");
        Response resigned = newEmployee.send(Action.MY_TASKS);
        verify(newEmployee.label, "Thao tác tiếp khi vừa bị xóa", "Phiên đăng nhập bị hủy ngay",
                resigned.isSessionExpired(), resigned.getMessage());
        rejected(guest, "Đăng nhập tài khoản đã nghỉ việc", guest.login("test_nv", "123456"), "không còn hoạt động");
        verify("Admin", "Kiểm tra lịch sử công việc của người đã nghỉ", "Công việc cũ vẫn còn trong hệ thống",
                WorkflowTest.<TaskView>list(admin.send(Action.LIST_TASKS)).stream().anyMatch(t -> t.title().equals("Việc test 1")), "");
        rejected(admin, "Xóa hẳn (tạo nhầm) nhân viên đã có công việc", admin.send(Action.DELETE_ACCOUNT, "employeeId", anId,
                "reason", RemovalReason.CREATED_BY_MISTAKE), "không thể xóa hẳn");
        ok(lan, "Xóa hẳn tài khoản tạo nhầm (nham)", lan.send(Action.DELETE_ACCOUNT, "employeeId", id(admin, "nham"),
                "reason", RemovalReason.CREATED_BY_MISTAKE));
        verify("Admin", "Kiểm tra tài khoản bị xóa hẳn", "Không còn trong hệ thống", !accounts(admin).containsKey("nham"), "");
        rejected(admin, "Xóa quản lý đang phụ trách dự án", admin.send(Action.DELETE_ACCOUNT, "employeeId", id(admin, "quanly02"),
                "reason", RemovalReason.RESIGNED), "phụ trách");
        rejected(lan, "Quản lý xóa tài khoản quản lý khác", lan.send(Action.DELETE_ACCOUNT, "employeeId", id(admin, "quanly03"),
                "reason", RemovalReason.RESIGNED), "Quản lý chỉ được");
        ok(admin, "Admin xóa quản lý không phụ trách dự án - Hết hợp đồng", admin.send(Action.DELETE_ACCOUNT,
                "employeeId", id(admin, "quanly03"), "reason", RemovalReason.CONTRACT_ENDED));
        rejected(lan, "Xóa tài khoản đang chờ duyệt", lan.send(Action.DELETE_ACCOUNT, "employeeId", id(admin, "nhanvien_moi"),
                "reason", RemovalReason.RESIGNED), "Từ chối");

        // ============================================================ 9. Trạng thái và xóa dự án
        section("9. Trạng thái dự án & xóa dự án", "DA Test còn 1 công việc chưa hoàn thành");
        ProjectView daTest2 = project(lan, "DA Test");
        rejected(lan, "Đánh dấu dự án hoàn thành khi còn việc (chưa xác nhận)", lan.send(Action.UPDATE_PROJECT,
                "projectId", daTest2.id(), "name", "DA Test", "status", ProjectStatus.COMPLETED), "xác nhận");
        ok(lan, "Đánh dấu dự án hoàn thành (có xác nhận)", lan.send(Action.UPDATE_PROJECT, "projectId", daTest2.id(),
                "name", "DA Test", "status", ProjectStatus.COMPLETED, "confirmIncomplete", true));
        User an2 = new User("Nhân viên (nhanvien01)");
        an2.login("nhanvien01", "123456");
        rejected(an2, "Cập nhật công việc khi dự án đã hoàn thành", an2.send(Action.UPDATE_TASK_PROGRESS,
                "taskId", t1.id(), "progress", 60), "chỉ được xem");
        rejected(lan, "Giao việc mới khi dự án đã hoàn thành", lan.send(Action.CREATE_TASK, "projectId", daTest2.id(),
                "assigneeId", anId, "title", "X"), "chỉ được xem");
        ok(lan, "Xóa dự án DA Test", lan.send(Action.DELETE_PROJECT, "projectId", daTest2.id()));
        verify("Admin", "Kiểm tra công việc của dự án đã xóa", "Công việc bị xóa theo dự án",
                task(admin, "Việc test 1") == null, "");

        // ============================================================ 10. Vai trò, lịch sử, cá nhân
        section("10. Đổi vai trò, lịch sử, thông tin cá nhân", "Dữ liệu sau các bước trên");
        rejected(admin, "Nâng nhân viên còn việc lên quản lý", admin.send(Action.UPDATE_ACCOUNT, "employeeId", anId,
                "fullName", "Nguyễn Minh An", "role", Role.MANAGER), "chưa hoàn thành");
        rejected(admin, "Hạ quản lý đang phụ trách dự án xuống nhân viên", admin.send(Action.UPDATE_ACCOUNT,
                "employeeId", id(admin, "quanly02"), "fullName", "Lê Hoàng Nam", "role", Role.EMPLOYEE), "phụ trách");
        List<StatusHistoryView> history = list(lan.send(Action.LIST_STATUS_HISTORY, "employeeId", anId));
        verify(lan.label, "Xem lịch sử trạng thái của nhanvien01", "Có bản ghi Tạm dừng và Quay lại làm việc",
                history.stream().anyMatch(h -> h.toStatus() == AccountStatus.ON_LEAVE)
                        && history.stream().anyMatch(h -> "Quay lại làm việc".equals(h.reason())), history.size() + " bản ghi");
        ok(admin, "Xem trang tổng quan (Admin)", admin.send(Action.DASHBOARD));
        ok(lan, "Xem trang tổng quan (Quản lý)", lan.send(Action.DASHBOARD));
        ok(an2, "Xem trang tổng quan (Nhân viên)", an2.send(Action.DASHBOARD));
        ok(an2, "Đổi mật khẩu 123456 -> 654321", an2.send(Action.CHANGE_PASSWORD, "oldPassword", "123456", "newPassword", "654321"));
        ok(guest, "Đăng nhập bằng mật khẩu mới", guest.login("nhanvien01", "654321"));
        ok(an2, "Cập nhật thông tin cá nhân (email, số điện thoại)", an2.send(Action.UPDATE_PROFILE,
                "fullName", "Nguyễn Minh An", "email", "an.new@company.local", "phone", "0901"));
    }
}
