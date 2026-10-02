package com.company.project.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable {
    private final Action action;
    private final String token;
    private final Map<String, Serializable> data;

    public Request(Action action) {
        this(action, null, new HashMap<>());
    }

    public Request(Action action, String token) {
        this(action, token, new HashMap<>());
    }

    public Request(Action action, String token, Map<String, Serializable> data) {
        this.action = action;
        this.token = token;
        this.data = data == null ? new HashMap<>() : new HashMap<>(data);
    }

    public Action getAction() {
        return action;
    }

    public String getToken() {
        return token;
    }

    public Map<String, Serializable> getData() {
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Invalid request field: " + key);
        }
        return (T) value;
    }
}
