package com.company.project.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public final class JpaUtil {
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory("taskPU", properties());

    private JpaUtil() {
    }

    public static EntityManager em() {
        return FACTORY.createEntityManager();
    }

    public static void close() {
        FACTORY.close();
    }

    private static Map<String, String> properties() {
        Map<String, String> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", setting("TASKFLOW_DB_URL",
                "jdbc:mariadb://localhost:3306/project_task_db?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8"));
        props.put("jakarta.persistence.jdbc.user", setting("TASKFLOW_DB_USER", "root"));
        props.put("jakarta.persistence.jdbc.password", setting("TASKFLOW_DB_PASSWORD", ""));
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
