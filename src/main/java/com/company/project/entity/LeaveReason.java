package com.company.project.entity;

/** Lý do tạm dừng tài khoản (nghỉ dài ngày). */
public enum LeaveReason {
    SICK_LEAVE("Nghỉ ốm"),
    MATERNITY_LEAVE("Nghỉ thai sản"),
    LONG_VACATION("Nghỉ phép dài ngày"),
    BUSINESS_TRIP("Công tác / đào tạo dài ngày"),
    OTHER("Lý do khác");

    private final String label;

    LeaveReason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
