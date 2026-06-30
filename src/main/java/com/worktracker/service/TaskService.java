package com.worktracker.service;

import com.worktracker.model.*;
import com.worktracker.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    // =========================================================================
    // TASK CRUD OPERATIONS
    // =========================================================================

    public static synchronized String generateNextTaskId() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<String> query = session.createQuery(
                "select t.taskId from Task t where t.taskId like 'TASK-%' order by t.taskId desc", 
                String.class
            );
            query.setMaxResults(1);
            String lastId = query.uniqueResult();
            if (lastId == null) {
                return "TASK-001";
            }
            try {
                int numericPart = Integer.parseInt(lastId.substring(5));
                return String.format("TASK-%03d", numericPart + 1);
            } catch (NumberFormatException e) {
                return "TASK-001";
            }
        }
    }

    public static void saveTask(Task task) {
        if (task.getTaskId() == null || task.getTaskId().trim().isEmpty()) {
            task.setTaskId(generateNextTaskId());
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                
                if (task.getAssignedDate() == null) {
                    task.setAssignedDate(LocalDate.now());
                }

                session.merge(task);
                
                // Auto log activity
                logActivityInternal(session, "Created/Updated Task: " + task.getTaskId() + " - " + task.getTaskName());
                
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error saving task", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static Task getTaskById(String taskId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Eagerly fetch processes and revisions to avoid lazy init issues in UI
            Task t = session.get(Task.class, taskId);
            if (t != null) {
                t.getProcesses().size();
                t.getRevisions().size();
            }
            return t;
        }
    }

    public static List<Task> getAllTasks() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Task order by taskId desc", Task.class).list();
        }
    }

    public static void deleteTask(String taskId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                Task task = session.get(Task.class, taskId);
                if (task != null) {
                    session.remove(task);
                    logActivityInternal(session, "Deleted Task: " + taskId);
                }
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error deleting task", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static List<Task> searchTasks(String text, String category, String status, String priority, LocalDate fromDate, LocalDate toDate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder sb = new StringBuilder("from Task t where 1=1");
            if (text != null && !text.trim().isEmpty()) {
                sb.append(" and (t.taskId like :text or t.taskName like :text or t.project like :text or t.client like :text or t.description like :text)");
            }
            if (category != null && !category.equals("All")) {
                sb.append(" and t.workCategory = :category");
            }
            if (status != null && !status.equals("All")) {
                sb.append(" and t.status = :status");
            }
            if (priority != null && !priority.equals("All")) {
                sb.append(" and t.priority = :priority");
            }
            if (fromDate != null) {
                sb.append(" and t.assignedDate >= :fromDate");
            }
            if (toDate != null) {
                sb.append(" and t.assignedDate <= :toDate");
            }
            sb.append(" order by t.taskId desc");

            Query<Task> query = session.createQuery(sb.toString(), Task.class);
            if (text != null && !text.trim().isEmpty()) {
                query.setParameter("text", "%" + text + "%");
            }
            if (category != null && !category.equals("All")) {
                query.setParameter("category", category);
            }
            if (status != null && !status.equals("All")) {
                query.setParameter("status", status);
            }
            if (priority != null && !priority.equals("All")) {
                query.setParameter("priority", priority);
            }
            if (fromDate != null) {
                query.setParameter("fromDate", fromDate);
            }
            if (toDate != null) {
                query.setParameter("toDate", toDate);
            }

            return query.list();
        }
    }

    // =========================================================================
    // SUB-PROCESS OPERATIONS
    // =========================================================================

    public static void saveProcess(TaskProcess process) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                
                Task task = session.get(Task.class, process.getTask().getTaskId());
                process.setTask(task);
                session.merge(process);

                // Recalculate parent task completion percentage
                rollupTaskCompletionPercentage(session, task);

                logActivityInternal(session, "Updated process '" + process.getProcessName() + "' on Task " + task.getTaskId());
                
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error saving process", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static void deleteProcess(Long processId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                TaskProcess process = session.get(TaskProcess.class, processId);
                if (process != null) {
                    Task task = process.getTask();
                    task.getProcesses().remove(process);
                    session.remove(process);

                    rollupTaskCompletionPercentage(session, task);
                    logActivityInternal(session, "Removed process '" + process.getProcessName() + "' from Task " + task.getTaskId());
                }
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error deleting process", e);
                throw new RuntimeException(e);
            }
        }
    }

    private static void rollupTaskCompletionPercentage(Session session, Task task) {
        // Fetch all processes associated with the task
        Query<Double> query = session.createQuery(
            "select avg(p.completionPercentage) from TaskProcess p where p.task = :task", 
            Double.class
        );
        query.setParameter("task", task);
        Double avg = query.uniqueResult();
        if (avg != null) {
            task.setCompletionPercentage(Math.round(avg * 10.0) / 10.0);
        } else {
            task.setCompletionPercentage(0.0);
        }
        session.merge(task);
    }

    // =========================================================================
    // REVISION HISTORY OPERATIONS
    // =========================================================================

    public static void addRevision(String taskId, String reason, long timeSpentSeconds, double completionPercentage, String notes) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                Task task = session.get(Task.class, taskId);
                if (task == null) {
                    throw new IllegalArgumentException("Task not found: " + taskId);
                }

                // Determine next revision number
                Query<Integer> query = session.createQuery(
                    "select max(r.revisionNumber) from Revision r where r.task = :task",
                    Integer.class
                );
                query.setParameter("task", task);
                Integer maxRev = query.uniqueResult();
                int nextRev = (maxRev == null) ? 1 : maxRev + 1;

                Revision rev = new Revision();
                rev.setTask(task);
                rev.setRevisionNumber(nextRev);
                rev.setDate(LocalDateTime.now());
                rev.setReason(reason);
                rev.setTimeSpentSeconds(timeSpentSeconds);
                rev.setCompletionPercentage(completionPercentage);
                rev.setNotes(notes);

                session.persist(rev);

                // Update task state if revision overrides it
                task.setStatus("In Progress");
                task.setCompletionPercentage(completionPercentage);
                session.merge(task);

                logActivityInternal(session, "Added Revision #" + nextRev + " to Task " + taskId + " due to: " + reason);

                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error adding revision", e);
                throw new RuntimeException(e);
            }
        }
    }

    // =========================================================================
    // SHIFT TRACKING OPERATIONS
    // =========================================================================

    public static ShiftLog startShift(String shiftType) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                ShiftLog shift = new ShiftLog(LocalDateTime.now(), shiftType);
                session.persist(shift);
                logActivityInternal(session, "Logged in to Shift: " + shiftType);
                tx.commit();
                return shift;
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error starting shift", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static void endShift(Long shiftId, long breakTimeSeconds) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                ShiftLog shift = session.get(ShiftLog.class, shiftId);
                if (shift != null) {
                    LocalDateTime logoutTime = LocalDateTime.now();
                    shift.setLogoutTime(logoutTime);
                    shift.setBreakTimeSeconds(breakTimeSeconds);

                    // Calculate total shift hours: (Logout - Login) - Break Time
                    java.time.Duration duration = java.time.Duration.between(shift.getLoginTime(), logoutTime);
                    long totalSeconds = duration.getSeconds() - breakTimeSeconds;
                    double totalHours = Math.max(0.0, totalSeconds / 3600.0);
                    // Round to 2 decimal places
                    shift.setTotalHours(Math.round(totalHours * 100.0) / 100.0);

                    session.merge(shift);
                    logActivityInternal(session, String.format("Logged out of Shift: %s. Hours worked: %.2f", shift.getShiftType(), shift.getTotalHours()));
                }
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error ending shift", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static ShiftLog getActiveShift() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<ShiftLog> query = session.createQuery(
                "from ShiftLog where logoutTime is null order by loginTime desc", 
                ShiftLog.class
            );
            query.setMaxResults(1);
            return query.uniqueResult();
        }
    }

    public static List<ShiftLog> getRecentShifts() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from ShiftLog order by loginTime desc", ShiftLog.class)
                    .setMaxResults(15)
                    .list();
        }
    }

    // =========================================================================
    // WORK CATEGORY MANAGEMENT
    // =========================================================================

    public static List<WorkCategory> getCategories() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from WorkCategory order by name", WorkCategory.class).list();
        }
    }

    public static void saveCategory(WorkCategory category) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.merge(category);
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error saving category", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static void deleteCategory(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                WorkCategory category = session.get(WorkCategory.class, id);
                if (category != null) {
                    session.remove(category);
                    logActivityInternal(session, "Removed category: " + category.getName());
                }
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error deleting category", e);
                throw new RuntimeException(e);
            }
        }
    }

    // =========================================================================
    // ACTIVITY LOGGING
    // =========================================================================

    public static void logActivity(String text) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                logActivityInternal(session, text);
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.error("Error logging activity", e);
            }
        }
    }

    private static void logActivityInternal(Session session, String text) {
        ActivityLog log = new ActivityLog(LocalDateTime.now(), text);
        session.persist(log);
    }

    public static List<ActivityLog> getRecentActivities(int maxResults) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from ActivityLog order by timestamp desc", ActivityLog.class)
                    .setMaxResults(maxResults)
                    .list();
        }
    }

    // =========================================================================
    // ANALYTICS & DASHBOARD METRICS
    // =========================================================================

    public static double getWorkingHoursForDateRange(LocalDateTime start, LocalDateTime end) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Aggregate durations from task processes and revisions
            Query<Long> processQuery = session.createQuery(
                "select sum(p.durationSeconds) from TaskProcess p where p.startTime >= :start and p.startTime <= :end", 
                Long.class
            );
            processQuery.setParameter("start", start);
            processQuery.setParameter("end", end);
            Long processSeconds = processQuery.uniqueResult();

            Query<Long> revisionQuery = session.createQuery(
                "select sum(r.timeSpentSeconds) from Revision r where r.date >= :start and r.date <= :end", 
                Long.class
            );
            revisionQuery.setParameter("start", start);
            revisionQuery.setParameter("end", end);
            Long revisionSeconds = revisionQuery.uniqueResult();

            long totalSeconds = (processSeconds == null ? 0 : processSeconds) + (revisionSeconds == null ? 0 : revisionSeconds);
            return totalSeconds / 3600.0;
        }
    }

    public static double getShiftHoursForDateRange(LocalDateTime start, LocalDateTime end) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Double> query = session.createQuery(
                "select sum(s.totalHours) from ShiftLog s where s.loginTime >= :start and s.loginTime <= :end and s.logoutTime is not null", 
                Double.class
            );
            query.setParameter("start", start);
            query.setParameter("end", end);
            Double hours = query.uniqueResult();
            return hours == null ? 0.0 : hours;
        }
    }
}
