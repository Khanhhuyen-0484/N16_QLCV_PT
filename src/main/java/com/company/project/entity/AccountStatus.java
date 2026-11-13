package com.company.project.entity;

/**
 * Trạng thái tài khoản.
 * PENDING  : nhân viên tự đăng ký, chờ quản lý duyệt.
 * ACTIVE   : đang hoạt động.
 * ON_LEAVE : tạm dừng (nghỉ ốm, thai sản, nghỉ phép dài...).
 * LOCKED   : bị khóa (vi phạm, nghi lộ mật khẩu...).
 * RESIGNED : đã nghỉ việc (xóa mềm, giữ lại lịch sử công việc).
 */
public enum AccountStatus {
    PENDING("Chờ duyệt"),
    ACTIVE("Đang hoạt động"),
    ON_LEAVE("Tạm dừng"),
    LOCKED("Đã khóa"),
    RESIGNED("Đã nghỉ việc");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
