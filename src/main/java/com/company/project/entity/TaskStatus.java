package com.company.project.entity;

public enum TaskStatus {
    TODO("Chưa bắt đầu"),
    IN_PROGRESS("Đang thực hiện"),
    ON_HOLD("Tạm hoãn"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Công việc còn mở = chưa hoàn thành và chưa bị hủy. */
    public boolean isOpen() {
        return this == TODO || this == IN_PROGRESS || this == ON_HOLD;
    }
}
