package com.company.project.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Khởi tạo EntityManagerFactory một lần (persistence-unit "taskPU") và cấp EntityManager cho từng yêu cầu.
 * Thông tin kết nối có thể ghi đè bằng biến môi trường TASKFLOW_DB_URL / TASKFLOW_DB_USER / TASKFLOW_DB_PASSWORD.
 */
public final class EntityManagerFactoryUtil {
    private static final EntityManagerFactory FACTORY =
            Persistence.createEntityManagerFactory("taskPU", properties());

    private EntityManagerFactoryUtil() {
    }

    public static EntityManager getEntityManager() {
        return FACTORY.createEntityManager();
    }

    public static void close() {
        if (FACTORY.isOpen()) {
            FACTORY.close();
        }
    }

    private static Map<String, String> properties() {
        Map<String, String> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", setting("TASKFLOW_DB_URL",
                "jdbc:mariadb://localhost:3306/project_task_db?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8"));
        props.put("jakarta.persistence.jdbc.user", setting("TASKFLOW_DB_USER", "root"));
        props.put("jakarta.persistence.jdbc.password", setting("TASKFLOW_DB_PASSWORD", "root123"));
        return props;
    }

    private static String setting(String key, String fallback) {
        String property = System.getProperty(key);
        if (property != null && !property.isBlank()) {
            return property;
        }
        String environment = System.getenv(key);
        if (environment != null && !environment.isBlank()) {
            return environment;
        }
        return fallback;
    }
}
