package com.company.project.entity;

public enum ProjectStatus {
    PLANNING("Lên kế hoạch"),
    IN_PROGRESS("Đang thực hiện"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Dự án đã đóng thì công việc chỉ được xem, không được cập nhật. */
    public boolean isClosed() {
        return this == COMPLETED || this == CANCELLED;
    }
}
