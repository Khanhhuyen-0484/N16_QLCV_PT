package com.company.project.service;

/** Phiên đăng nhập không còn hợp lệ: client cần quay về màn hình đăng nhập. */
public class SessionExpiredException extends SecurityException {
    public SessionExpiredException(String message) {
        super(message);
    }
}
