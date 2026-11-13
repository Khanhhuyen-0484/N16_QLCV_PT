# Ứng dụng quản lý phân công công việc

Bài tập lớn **Java Persistence API + Network Programming (Client/Server - Socket)**.
Ứng dụng quản lý phân công công việc trong các dự án của một công ty, phân quyền 3 cấp:
**Admin > Quản lý > Nhân viên**.

## Phân quyền

| Chức năng | Admin | Quản lý | Nhân viên |
|---|:-:|:-:|:-:|
| Đăng nhập, xem / sửa hồ sơ, đổi mật khẩu | ✅ | ✅ | ✅ |
| Tự đăng ký tài khoản (chờ quản lý duyệt) | — | — | ✅ |
| Duyệt / từ chối đăng ký | ✅ | ✅ | ❌ |
| Tạo tài khoản quản lý | ✅ | ❌ | ❌ |
| Tạo tài khoản nhân viên | ✅ | ✅ | ❌ |
| Sửa, tạm dừng, khóa, xóa nhân viên | ✅ | ✅ | ❌ |
| Sửa, khóa, xóa quản lý; đổi vai trò | ✅ | ❌ | ❌ |
| Dự án: đăng, sửa, xóa, cập nhật trạng thái, phân công thành viên | ✅ mọi dự án | ✅ dự án phụ trách | ❌ (chỉ xem dự án tham gia) |
| Giao việc, sửa, xóa, chuyển giao, mở lại công việc | ✅ | ✅ | ❌ |
| Xem việc được giao, cập nhật tiến độ, ghi chú, check hoàn thành, nộp kết quả | — | — | ✅ |

- Chỉ có **1 Admin** (server tạo sẵn), không ai xóa / khóa / hạ quyền được Admin.
- Không ai tự khóa / tự xóa chính mình. Mọi kiểm tra quyền đều thực hiện ở **server**.

## Quy trình tài khoản

```
Nhân viên tự đăng ký ──► CHỜ DUYỆT ──► Quản lý DUYỆT ──► HOẠT ĐỘNG
                              └──────► Quản lý TỪ CHỐI ──► bản đăng ký bị xóa
Admin / Quản lý tạo trực tiếp ─────────────────────────► HOẠT ĐỘNG

HOẠT ĐỘNG ──► TẠM DỪNG (nghỉ ốm, thai sản, nghỉ phép dài...) ──► Kích hoạt lại
HOẠT ĐỘNG ──► KHÓA (vi phạm, lộ mật khẩu...)                 ──► Mở khóa
HOẠT ĐỘNG ──► ĐÃ NGHỈ VIỆC (xóa mềm)
```

- **Tạm dừng**: chọn lý do, ngày bắt đầu, ngày dự kiến quay lại. Công việc đang làm được **giữ lại và tạm hoãn**
  hoặc **chuyển giao** cho nhân viên khác. Khi kích hoạt lại, việc tạm hoãn được tiếp tục.
- **Xóa tài khoản** bắt buộc chọn lý do:
  - *Nghỉ việc / Hết hợp đồng / Bị sa thải* → xóa mềm (chuyển sang "Đã nghỉ việc", giữ lịch sử công việc).
  - *Tạo nhầm / trùng* → xóa hẳn khỏi CSDL, chỉ khi tài khoản chưa có công việc nào.
  - Điều kiện: không còn công việc chưa hoàn thành (phải chuyển giao trước), không phụ trách dự án nào.
- Mọi thay đổi trạng thái được lưu vào **lịch sử trạng thái** (ai đổi, lúc nào, lý do).

## Quy trình dự án / công việc

- Dự án mới ở trạng thái *Lên kế hoạch*, có **quản lý phụ trách** (người tạo; Admin có thể đổi).
- Chuyển dự án sang *Đã hoàn thành* khi còn việc chưa xong phải xác nhận. Dự án đã hoàn thành / đã hủy thì
  công việc chỉ được xem.
- Xóa dự án ở mọi trạng thái (có xác nhận), các công việc của dự án bị xóa theo.
- Giao việc: chọn dự án → chọn thành viên đang hoạt động của dự án. Chỉ gỡ được thành viên khi người đó không
  còn việc chưa xong trong dự án.
- Trạng thái công việc: *Chưa bắt đầu → Đang thực hiện → Hoàn thành*, thêm *Tạm hoãn* (khi người làm tạm dừng).

## Công nghệ và kiến thức áp dụng

| Kiến thức | Áp dụng trong dự án |
|---|---|
| Socket TCP | `ServerSocket` / `Socket`, truyền `Request` / `Response` (Serializable) bằng `ObjectInputStream` / `ObjectOutputStream` |
| Đa luồng | Server dùng thread pool (`ExecutorService`) - mỗi client một luồng; phiên đăng nhập lưu trong `ConcurrentHashMap`; client Swing gọi server bằng `SwingWorker`, `ServerConnection.send` được `synchronized` |
| JPA / Hibernate | Entity, `persistence.xml`, quan hệ **2 chiều** `@OneToMany(mappedBy)` / `@ManyToOne`, `@ManyToMany` + `@JoinTable`, `cascade`, `orphanRemoval`, `@NamedQuery`, bulk update `executeUpdate()` |
| Mô hình 3 tầng | `entity` → `repository` (interface + Impl) → `service` (nghiệp vụ, phân quyền, transaction) → `server` / `client` |

- Java 21, Maven, MariaDB/MySQL (HeidiSQL), FlatLaf cho giao diện Swing.

## Cách chạy

Cần JDK 21, Maven và MariaDB/MySQL.

1. (Tùy chọn) Chạy [database/project_task_db.sql](database/project_task_db.sql) trong HeidiSQL để tạo bảng + dữ liệu mẫu.
   Nếu không chạy, server tự tạo bảng (Hibernate `update`) và dữ liệu mẫu khi CSDL còn trống.
2. Mặc định kết nối `localhost:3306/project_task_db`, user `root`. Nếu mật khẩu MySQL/MariaDB khác, đặt biến môi trường:

```powershell
$env:TASKFLOW_DB_USER="root"
$env:TASKFLOW_DB_PASSWORD="mat_khau_cua_ban"
```

3. Terminal 1 - chạy server (cổng mặc định `2026`):

```powershell
mvn compile exec:java "-Dexec.mainClass=com.company.project.server.TaskServer"
```

4. Terminal 2 - chạy client giao diện Swing:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.company.project.client.TaskSwingClient"
```

Hoặc client console (dùng để kiểm thử nhanh):

```powershell
mvn compile exec:java "-Dexec.mainClass=com.company.project.client.TaskClient"
```

Có thể truyền host / port cho client: `"-Dexec.args=192.168.1.10 2026"`.

## Kiểm thử

Bộ kiểm thử tích hợp [WorkflowTest.java](src/test/java/com/company/project/WorkflowTest.java) (JUnit 5) gồm
**99 test case** chạy qua Socket: đăng nhập, đăng ký/duyệt, phân quyền, dự án, giao việc, nhân viên cập nhật
công việc, tạm dừng/kích hoạt lại, xóa tài khoản, trạng thái dự án, đổi vai trò, lịch sử.

```powershell
mvn test
```

- Test tự khởi động server ở cổng `2099` với CSDL riêng `project_task_test` (tạo mới khi bắt đầu, xóa khi kết thúc),
  **không ảnh hưởng dữ liệu thật**.
- Kết quả từng test case được ghi ra `target/test-report/testcases.tsv`.
- File test case Excel: [docs/TestCase_QuanLyCongViec.xlsx](docs/TestCase_QuanLyCongViec.xlsx)
  (sheet Tổng hợp, Test case, Dữ liệu kiểm thử, Hình chụp màn hình).

## Tài liệu

| Tài liệu | Đường dẫn |
|---|---|
| Báo cáo (Word) | [docs/BaoCao/BaoCao_N16_QuanLyPhanCongCongViec.docx](docs/BaoCao/BaoCao_N16_QuanLyPhanCongCongViec.docx) |
| Slide thuyết trình | [docs/Slide/SlideThuyetTrinh_N16_QuanLyPhanCongCongViec.pptx](docs/Slide/SlideThuyetTrinh_N16_QuanLyPhanCongCongViec.pptx) |
| Kịch bản thuyết trình & câu hỏi phản biện | [docs/ThuyetTrinh/KichBan_ThuyetTrinh_PhanBien_N16.docx](docs/ThuyetTrinh/KichBan_ThuyetTrinh_PhanBien_N16.docx) |
| Test case | [docs/TestCase_QuanLyCongViec.xlsx](docs/TestCase_QuanLyCongViec.xlsx) |
| Sơ đồ (use case, ERD, lớp, kiến trúc, trạng thái, tuần tự) | [docs/diagrams](docs/diagrams) |

## Tài khoản mẫu

```text
Admin      : admin / admin123
Quản lý    : quanly01 / 123456   (phụ trách "Hệ thống quản lý công việc nội bộ")
Quản lý    : quanly02 / 123456   (phụ trách "Website giới thiệu công ty")
Nhân viên  : nhanvien01 / 123456
Nhân viên  : minhdev / 123456
Tạm dừng   : thuha / 123456      (nghỉ thai sản - không đăng nhập được)
Chờ duyệt  : nhanvien_moi / 123456
```

## Cấu trúc thư mục

```text
src/main/java/com/company/project
  entity/      Entity JPA (Employee, Project, ProjectTask, AccountStatusHistory) + enum
  repository/  Tầng truy cập dữ liệu: interface + Impl, dùng NamedQuery
  service/     Nghiệp vụ, phân quyền, transaction (AuthService, AccountService, ProjectService, TaskService)
  dto/         Dữ liệu trả về qua socket (Serializable)
  network/     Request / Response / Action - giao thức giữa client và server
  server/      TaskServer (ServerSocket + thread pool), ClientWorker, RequestDispatcher
  client/      ServerConnection, TaskSwingClient (Swing), TaskClient (console)
  util/        EntityManagerFactoryUtil, PasswordUtil (SHA-256)
src/main/resources/META-INF/persistence.xml
database/project_task_db.sql
```

## Nâng cấp từ phiên bản cũ

Khi khởi động, server tự chuyển dữ liệu cũ: cột trạng thái kiểu `enum` → `varchar`, `DISABLED` → `LOCKED`,
`DONE` → `COMPLETED`, tài khoản `admin` → vai trò `ADMIN`, gán quản lý phụ trách cho dự án chưa có.
