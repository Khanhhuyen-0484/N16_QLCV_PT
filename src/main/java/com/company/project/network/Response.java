package com.company.project.network;

import java.io.Serializable;

/** Phản hồi server gửi về client. sessionExpired = true khi client cần đăng nhập lại. */
public class Response implements Serializable {
    private final boolean success;
    private final String message;
    private final Serializable data;
    private final boolean sessionExpired;

    private Response(boolean success, String message, Serializable data, boolean sessionExpired) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.sessionExpired = sessionExpired;
    }

    public static Response ok(String message, Serializable data) {
        return new Response(true, message, data, false);
    }

    public static Response ok(String message) {
        return ok(message, null);
    }

    public static Response error(String message) {
        return new Response(false, message, null, false);
    }

    public static Response sessionExpired(String message) {
        return new Response(false, message, null, true);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Serializable getData() {
        return data;
    }

    public boolean isSessionExpired() {
        return sessionExpired;
    }
}
