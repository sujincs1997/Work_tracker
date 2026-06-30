package com.worktracker.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class HibernateUtil {
    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;
    private static String currentDbPath = "worktracker.db"; // Default location

    static {
        try {
            buildSessionFactory();
        } catch (Throwable ex) {
            logger.error("Initial SessionFactory creation failed.", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static synchronized void buildSessionFactory() {
        try {
            if (sessionFactory != null && !sessionFactory.isClosed()) {
                sessionFactory.close();
            }

            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");

            // Override SQLite DB URL dynamically
            String jdbcUrl = "jdbc:sqlite:" + currentDbPath;
            configuration.setProperty("hibernate.connection.url", jdbcUrl);
            logger.info("Initializing Hibernate SessionFactory with JDBC URL: {}", jdbcUrl);

            sessionFactory = configuration.buildSessionFactory();
            logger.info("Hibernate SessionFactory initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to build SessionFactory", e);
            throw new RuntimeException("Failed to initialize session factory: " + e.getMessage(), e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static synchronized void setDatabasePath(String newDbPath) {
        if (newDbPath == null || newDbPath.trim().isEmpty()) {
            return;
        }
        if (!currentDbPath.equals(newDbPath)) {
            // Ensure parent directories exist
            File file = new File(newDbPath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            currentDbPath = newDbPath;
            buildSessionFactory();
        }
    }

    public static String getDatabasePath() {
        return currentDbPath;
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            logger.info("Hibernate SessionFactory closed.");
        }
    }
}
