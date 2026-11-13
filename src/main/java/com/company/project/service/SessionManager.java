package com.company.project.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý phiên đăng nhập: token -> id tài khoản.
 * Dùng ConcurrentHashMap vì nhiều luồng ClientWorker truy cập đồng thời.
 */
public class SessionManager {
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public String create(long employeeId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, employeeId);
        return token;
    }

    public Long employeeId(String token) {
        return token == null ? null : sessions.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /** Đăng xuất mọi phiên của một tài khoản (khi bị khóa, tạm dừng hoặc xóa). */
    public void removeEmployee(long employeeId) {
        sessions.values().removeIf(id -> id == employeeId);
    }
}
