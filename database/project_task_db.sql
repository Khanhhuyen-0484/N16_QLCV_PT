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
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
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
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
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
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20),
    project_id BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    assigner_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES employees(id),
    CONSTRAINT fk_tasks_assigner FOREIGN KEY (assigner_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO employees
    (id, username, passwordHash, fullName, email, phone, role, status, createdAt)
VALUES
    (1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
     'Default Manager', 'admin@company.local', '0000000000', 'MANAGER', 'ACTIVE', NOW()),
    (2, 'nhanvien01', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'Nguyễn Minh An', 'an@company.local', '0901000001', 'EMPLOYEE', 'ACTIVE', NOW()),
    (3, 'minhdev', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'Nguyễn Văn Minh', 'minh@company.local', '0912345678', 'EMPLOYEE', 'ACTIVE', NOW()),
    (4, 'nhanvien_moi', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
     'Nhân Viên Mới', 'new.employee@company.local', '0987654321', 'EMPLOYEE', 'PENDING', NOW());

INSERT IGNORE INTO projects
    (id, name, description, startDate, deadline, status)
VALUES
    (1, 'Dự án công ty',
     'Dự án quản lý phân công công việc trong một dự án của công ty.',
     CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 MONTH), 'IN_PROGRESS');

INSERT IGNORE INTO project_members (project_id, employee_id)
VALUES
    (1, 1),
    (1, 2),
    (1, 3),
    (1, 4);

INSERT IGNORE INTO project_tasks
    (id, title, description, note, resultText, resultLink, startDate, deadline, completedAt,
     progress, status, priority, project_id, assignee_id, assigner_id)
VALUES
    (1, 'Thiết kế giao diện Dashboard',
     'Hoàn thiện giao diện tổng quan dự án, thống kê và danh sách công việc gần deadline.',
     'Đang hoàn thiện bố cục và màu sắc.', NULL, NULL,
     DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY), NULL,
     65, 'IN_PROGRESS', 'HIGH', 1, 2, 1),
    (2, 'Xây dựng chức năng phân công công việc',
     'Tạo form giao việc, chọn nhân viên, deadline và mức độ ưu tiên.',
     'Đã nối socket client/server.', NULL, NULL,
     DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 7 DAY), NULL,
     35, 'IN_PROGRESS', 'MEDIUM', 1, 3, 1),
    (3, 'Kiểm thử đăng nhập và phân quyền',
     'Kiểm tra luồng đăng nhập, đổi mật khẩu, duyệt tài khoản và khóa tài khoản.',
     'Đã hoàn thành kiểm thử chính.', 'Đã kiểm thử các luồng chính.', NULL,
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
     100, 'COMPLETED', 'MEDIUM', 1, 2, 1),
    (4, 'Viết báo cáo tiến độ dự án',
     'Tổng hợp số lượng công việc, tiến độ và trạng thái quá hạn.',
     'Cần cập nhật phần công việc quá hạn.', NULL, NULL,
     DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY), NULL,
     20, 'IN_PROGRESS', 'URGENT', 1, 3, 1),
    (5, 'Chuẩn bị dữ liệu demo',
     'Tạo dữ liệu mẫu để máy khác clone project có thể chạy thử ngay.',
     '', NULL, NULL,
     CURDATE(), DATE_ADD(CURDATE(), INTERVAL 10 DAY), NULL,
     0, 'TODO', 'LOW', 1, 2, 1);

-- Tài khoản mẫu:
-- Quản lý: admin / admin123
-- Nhân viên: nhanvien01 / 123456
-- Nhân viên: minhdev / 123456
-- Chờ duyệt: nhanvien_moi / 123456
