# Ứng dụng quản lý phân công công việc

Ứng dụng Java minh họa bài tập lớn **Java Persistence API + Network Programming (Client/Server - Socket)**.
Hệ thống quản lý phân công công việc trong **một dự án duy nhất của công ty**.

## Chức năng

- Nhân viên:
  - Đăng ký tài khoản mới và chờ quản lý phê duyệt.
  - Đăng nhập, xem/cập nhật thông tin cá nhân, đổi mật khẩu.
  - Xem các công việc được giao, tiến độ, thời gian và deadline.
  - Cập nhật tiến độ, đánh dấu hoàn thành, ghi chú và nộp kết quả công việc.
  - Xem các dự án mình tham gia.
- Quản lý:
  - Đăng nhập bằng tài khoản quản lý.
  - Quản lý tài khoản nhân viên: xem danh sách, phê duyệt, khóa tài khoản.
  - Xem/cập nhật thông tin dự án duy nhất.
  - Phân công nhân viên vào dự án.
  - Tạo công việc, đặt deadline, mức độ ưu tiên và giao cho nhân viên.
  - Xem thống kê tiến độ, công việc quá hạn và kết quả nhân viên nộp.

## Công nghệ

- Java 21
- Socket TCP với `ServerSocket`, `Socket`, `ObjectInputStream`, `ObjectOutputStream`
- Jakarta Persistence API
- Hibernate ORM
- MariaDB/MySQL, có thể quản lý bằng HeidiSQL
- FlatLaf cho giao diện Swing hiện đại
- Maven

## Cách chạy

Cần cài Maven, JDK 21 và MySQL/MariaDB.

Tạo database và dữ liệu mẫu trong HeidiSQL bằng cách chạy file:

[database/project_task_db.sql](database/project_task_db.sql)

File này tạo sẵn bảng, dự án, tài khoản và công việc demo. Hoặc chỉ tạo database bằng lệnh:

```sql
CREATE DATABASE IF NOT EXISTS project_task_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Mặc định ứng dụng kết nối:

```text
Host: localhost
Port: 3306
Database: project_task_db
User mặc định: root
Password mặc định: rỗng
```

Nếu MySQL/MariaDB của bạn dùng mật khẩu khác, set biến môi trường trước khi chạy. Ví dụ PowerShell:

```powershell
$env:TASKFLOW_DB_USER="root"
$env:TASKFLOW_DB_PASSWORD="mat_khau_cua_ban"
```

Mở terminal 1 để chạy server:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.company.project.server.TaskServer"
```

Mở terminal 2 để chạy client giao diện console:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.company.project.client.TaskClient"
```

Hoặc chạy client giao diện cửa sổ Swing:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.company.project.client.TaskSwingClient"
```

Server mặc định chạy cổng `2026`. Hibernate sẽ tự tạo/cập nhật bảng trong MySQL khi server chạy.
Khi database chưa có công việc, server sẽ tự tạo dữ liệu demo để máy khác clone về chạy thử ngay.

Tài khoản mẫu:

```text
Quản lý:
  username: admin
  password: admin123

Nhân viên đã duyệt:
  username: nhanvien01
  password: 123456

Nhân viên đã duyệt:
  username: minhdev
  password: 123456

Nhân viên chờ duyệt:
  username: nhanvien_moi
  password: 123456
```

Có thể truyền host/port cho client:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.company.project.client.TaskClient" "-Dexec.args=localhost 2026"
```

## Cấu trúc thư mục

```text
src/main/java/com/company/project
  client/      Ứng dụng console và Swing GUI cho người dùng
  dto/         Dữ liệu trả về qua socket
  entity/      Entity JPA
  network/     Request/Response/Action cho socket protocol
  server/      Server socket và dispatcher
  service/     Nghiệp vụ và thao tác JPA
  util/        JPA utility, password hashing
src/main/resources/META-INF/persistence.xml
```

## Lưu ý demo

- Nhân viên đăng ký mới sẽ có trạng thái `PENDING`; quản lý cần vào menu duyệt tài khoản.
- Khi quản lý tạo công việc cho nhân viên, nhân viên đó cũng được thêm vào danh sách thành viên dự án.
- Xóa dự án sẽ xóa các công việc thuộc dự án đó.
