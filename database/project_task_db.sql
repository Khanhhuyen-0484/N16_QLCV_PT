-- =====================================================================
-- CSDL mẫu cho ứng dụng Quản lý phân công công việc (JPA + Socket)
-- Có thể chạy file này trong HeidiSQL để tạo bảng + dữ liệu demo.
-- (Nếu không chạy, server cũng tự tạo bảng và dữ liệu mẫu khi khởi động.)
-- =====================================================================
CREATE DATABASE IF NOT EXISTS project_task_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE project_task_db;

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(60) NOT NULL,
    passwordHash VARCHAR(128) NOT NULL,
    fullName VARCHAR(120) NOT NULL,
    email VARCHAR(120),
    phone VARCHAR(30),
    role VARCHAR(20) NOT NULL,              -- ADMIN | MANAGER | EMPLOYEE
    status VARCHAR(20) NOT NULL,            -- PENDING | ACTIVE | ON_LEAVE | LOCKED | RESIGNED
    statusReason VARCHAR(255),
    statusFrom DATE,
    statusUntil DATE,
    createdAt DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employees_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    startDate DATE,
    deadline DATE,
    status VARCHAR(20) NOT NULL,            -- PLANNING | IN_PROGRESS | COMPLETED | CANCELLED
    manager_id BIGINT,                      -- quản lý phụ trách
    PRIMARY KEY (id),
    CONSTRAINT fk_projects_manager FOREIGN KEY (manager_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_members (
    project_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    PRIMARY KEY (project_id, employee_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_project_members_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    note VARCHAR(1000),
    resultText VARCHAR(2000),
    resultLink VARCHAR(500),
    startDate DATE,
    deadline DATE,
    completedAt DATETIME,
    progress INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,            -- TODO | IN_PROGRESS | ON_HOLD | COMPLETED | CANCELLED
    priority VARCHAR(20),                   -- LOW | MEDIUM | HIGH | URGENT
    project_id BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    assigner_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES employees(id),
    CONSTRAINT fk_tasks_assigner FOREIGN KEY (assigner_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS account_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    fromStatus VARCHAR(20),
    toStatus VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    note VARCHAR(1000),
    fromDate DATE,
    toDate DATE,
    changedAt DATETIME NOT NULL,
    changed_by_id BIGINT,
    changedByName VARCHAR(120),
    PRIMARY KEY (id),
    CONSTRAINT fk_history_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_history_changed_by FOREIGN KEY (changed_by_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------- tài khoản
-- admin123 = 240be518...  ;  123456 = 8d969eef...
INSERT IGNORE INTO employees
    (id, username, passwordHash, fullName, email, phone, role, status, statusReason, statusFrom, statusUntil, createdAt)
VALUES
    (1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
     'Quản trị hệ thống', 'admin@company.local', '0900000000', 'ADMIN', 'ACTIVE', NULL, NULL, NULL, NOW()),
    (2, 'quanly01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'Trần Thị Lan', 'lan.tran@company.local', '0903000001', 'MANAGER', 'ACTIVE', NULL, NULL, NULL, NOW()),
    (3, 'quanly02', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'Lê Hoàng Nam', 'nam.le@company.local', '0903000002', 'MANAGER', 'ACTIVE', NULL, NULL, NULL, NOW()),
    (4, 'nhanvien01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'Nguyễn Minh An', 'an@company.local', '0901000001', 'EMPLOYEE', 'ACTIVE', NULL, NULL, NULL, NOW()),
    (5, 'minhdev', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'Nguyễn Văn Minh', 'minh@company.local', '0912345678', 'EMPLOYEE', 'ACTIVE', NULL, NULL, NULL, NOW()),
    (6, 'thuha', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'Phạm Thu Hà', 'ha.pham@company.local', '0934000003', 'EMPLOYEE', 'ON_LEAVE', 'Nghỉ thai sản',
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 170 DAY), NOW()),
    (7, 'nhanvien_moi', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'Nhân Viên Mới', 'new.employee@company.local', '0987654321', 'EMPLOYEE', 'PENDING', NULL, NULL, NULL, NOW());

INSERT IGNORE INTO account_status_history
    (id, employee_id, fromStatus, toStatus, reason, note, fromDate, toDate, changedAt, changed_by_id, changedByName)
VALUES
    (1, 1, NULL, 'ACTIVE', 'Dữ liệu mẫu', NULL, CURDATE(), NULL, NOW(), NULL, 'Hệ thống'),
    (2, 2, NULL, 'ACTIVE', 'Dữ liệu mẫu', NULL, CURDATE(), NULL, NOW(), NULL, 'Hệ thống'),
    (3, 3, NULL, 'ACTIVE', 'Dữ liệu mẫu', NULL, CURDATE(), NULL, NOW(), NULL, 'Hệ thống'),
    (4, 4, NULL, 'ACTIVE', 'Dữ liệu mẫu', NULL, CURDATE(), NULL, NOW(), NULL, 'Hệ thống'),
    (5, 5, NULL, 'ACTIVE', 'Dữ liệu mẫu', NULL, CURDATE(), NULL, NOW(), NULL, 'Hệ thống'),
    (6, 6, NULL, 'ACTIVE', 'Dữ liệu mẫu', NULL, CURDATE(), NULL, NOW(), NULL, 'Hệ thống'),
    (7, 6, 'ACTIVE', 'ON_LEAVE', 'Nghỉ thai sản', 'Nghỉ thai sản 6 tháng',
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 170 DAY), NOW(), 2, 'Trần Thị Lan'),
    (8, 7, NULL, 'PENDING', 'Nhân viên tự đăng ký, chờ duyệt', NULL, CURDATE(), NULL, NOW(), NULL, 'Hệ thống');

-- ---------------------------------------------------------------- dự án
INSERT IGNORE INTO projects (id, name, description, startDate, deadline, status, manager_id)
VALUES
    (1, 'Hệ thống quản lý công việc nội bộ',
     'Xây dựng ứng dụng client/server quản lý phân công công việc cho công ty.',
     DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 2 MONTH), 'IN_PROGRESS', 2),
    (2, 'Website giới thiệu công ty',
     'Thiết kế lại website giới thiệu sản phẩm và tuyển dụng.',
     DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 3 MONTH), 'PLANNING', 3);

INSERT IGNORE INTO project_members (project_id, employee_id)
VALUES (1, 4), (1, 5), (1, 6), (2, 5);

-- ---------------------------------------------------------------- công việc
INSERT IGNORE INTO project_tasks
    (id, title, description, note, resultText, resultLink, startDate, deadline, completedAt,
     progress, status, priority, project_id, assignee_id, assigner_id)
VALUES
    (1, 'Thiết kế giao diện Dashboard',
     'Hoàn thiện giao diện tổng quan dự án, thống kê và danh sách công việc gần deadline.',
     'Đang hoàn thiện bố cục và màu sắc.', NULL, NULL,
     DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY), NULL,
     65, 'IN_PROGRESS', 'HIGH', 1, 4, 2),
    (2, 'Xây dựng chức năng phân công công việc',
     'Tạo form giao việc, chọn nhân viên, deadline và mức độ ưu tiên.',
     'Đã nối socket client/server.', NULL, NULL,
     DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 7 DAY), NULL,
     35, 'IN_PROGRESS', 'MEDIUM', 1, 5, 2),
    (3, 'Kiểm thử đăng nhập và phân quyền',
     'Kiểm tra luồng đăng nhập, đổi mật khẩu, duyệt tài khoản và khóa tài khoản.',
     'Đã hoàn thành kiểm thử chính.', 'Đã kiểm thử các luồng chính.', NULL,
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
     100, 'COMPLETED', 'MEDIUM', 1, 4, 2),
    (4, 'Viết báo cáo tiến độ dự án',
     'Tổng hợp số lượng công việc, tiến độ và trạng thái quá hạn.',
     'Cần cập nhật phần công việc quá hạn.', NULL, NULL,
     DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY), NULL,
     20, 'IN_PROGRESS', 'URGENT', 1, 5, 2),
    (5, 'Chuẩn bị dữ liệu demo',
     'Tạo dữ liệu mẫu để máy khác clone project có thể chạy thử ngay.',
     '', NULL, NULL,
     CURDATE(), DATE_ADD(CURDATE(), INTERVAL 10 DAY), NULL,
     0, 'TODO', 'LOW', 1, 4, 2),
    (6, 'Viết tài liệu hướng dẫn sử dụng',
     'Soạn tài liệu hướng dẫn cho quản lý và nhân viên.',
     'Tạm hoãn trong thời gian nghỉ thai sản.', NULL, NULL,
     DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY), NULL,
     40, 'ON_HOLD', 'MEDIUM', 1, 6, 2),
    (7, 'Khảo sát yêu cầu website',
     'Thu thập yêu cầu từ phòng kinh doanh và nhân sự.',
     '', NULL, NULL,
     DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY), NULL,
     0, 'TODO', 'MEDIUM', 2, 5, 3);

-- Tài khoản mẫu:
--   Admin    : admin / admin123
--   Quản lý  : quanly01 / 123456  (phụ trách dự án 1)   |   quanly02 / 123456 (phụ trách dự án 2)
--   Nhân viên: nhanvien01 / 123456  |  minhdev / 123456
--   Tạm dừng : thuha / 123456 (nghỉ thai sản - không đăng nhập được)
--   Chờ duyệt: nhanvien_moi / 123456
