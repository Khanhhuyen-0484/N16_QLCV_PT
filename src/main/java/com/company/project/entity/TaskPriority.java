package com.company.project.entity;

public enum TaskPriority {
    LOW("Thấp"),
    MEDIUM("Trung bình"),
    HIGH("Cao"),
    URGENT("Khẩn cấp");

    private final String label;

    TaskPriority(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
