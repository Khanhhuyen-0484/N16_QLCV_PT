package com.company.project.entity;

/**
 * Lý do xóa tài khoản.
 * Nghỉ việc / hết hợp đồng / sa thải: xóa mềm (chuyển sang RESIGNED, giữ lịch sử).
 * Tạo nhầm / trùng: xóa hẳn khỏi CSDL, chỉ khi tài khoản chưa có công việc nào.
 */
public enum RemovalReason {
    RESIGNED("Nghỉ việc", true),
    CONTRACT_ENDED("Hết hợp đồng", true),
    DISMISSED("Bị sa thải", true),
    CREATED_BY_MISTAKE("Tạo nhầm / trùng tài khoản", false);

    private final String label;
    private final boolean softDelete;

    RemovalReason(String label, boolean softDelete) {
        this.label = label;
        this.softDelete = softDelete;
    }

    public String label() {
        return label;
    }

    public boolean isSoftDelete() {
        return softDelete;
    }

    @Override
    public String toString() {
        return label;
    }
}
