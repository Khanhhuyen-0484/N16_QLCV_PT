package com.company.project.entity;

/**
 * Vai trò người dùng: ADMIN (quyền cao nhất) > MANAGER (quản lý) > EMPLOYEE (nhân viên).
 */
public enum Role {
    ADMIN("Quản trị viên"),
    MANAGER("Quản lý"),
    EMPLOYEE("Nhân viên");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
