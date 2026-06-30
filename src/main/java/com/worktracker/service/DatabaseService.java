package com.worktracker.service;

import com.worktracker.model.AppSettings;
import com.worktracker.model.WorkCategory;
import com.worktracker.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private static final String[] DEFAULT_CATEGORIES = {
            "Development - New",
            "Development - Revision",
            "Enhancement",
            "Bug Fix",
            "Research",
            "Documentation",
            "Meeting",
            "QA",
            "GIS Editing",
            "AutoCAD",
            "FME Development",
            "Python Development",
            "SQL Development",
            "Testing",
            "Deployment"
    };

    public static void initializeDatabase() {
        logger.info("Initializing database schema and defaults...");
        // Ensure Hibernate SessionFactory is loaded
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();

                // 1. Seed AppSettings if missing
                AppSettings settings = session.get(AppSettings.class, 1L);
                if (settings == null) {
                    logger.info("No app settings found. Seeding default AppSettings.");
                    settings = new AppSettings();
                    session.persist(settings);
                } else {
                    // Update database configuration to point to custom db path from settings
                    HibernateUtil.setDatabasePath(settings.getDatabaseLocation());
                }

                // 2. Seed Work Categories if missing
                Long categoryCount = session.createQuery("select count(c) from WorkCategory c", Long.class).uniqueResult();
                if (categoryCount == null || categoryCount == 0) {
                    logger.info("No work categories found. Seeding default categories.");
                    for (String catName : DEFAULT_CATEGORIES) {
                        WorkCategory category = new WorkCategory(catName, true);
                        session.persist(category);
                    }
                }

                tx.commit();
                logger.info("Database initialization completed successfully.");
            } catch (Exception e) {
                if (tx != null) {
                    tx.rollback();
                }
                logger.error("Error during database seeding", e);
                throw new RuntimeException("Database initialization failed", e);
            }
        }
    }

    public static AppSettings getSettings() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(AppSettings.class, 1L);
        }
    }

    public static void saveSettings(AppSettings settings) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.merge(settings);
                tx.commit();
                
                // Dynamic database configuration switch
                HibernateUtil.setDatabasePath(settings.getDatabaseLocation());
            } catch (Exception e) {
                if (tx != null) {
                    tx.rollback();
                }
                logger.error("Failed to save settings", e);
                throw new RuntimeException(e);
            }
        }
    }
}
