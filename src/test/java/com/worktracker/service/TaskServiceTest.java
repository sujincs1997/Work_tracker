package com.worktracker.service;

import com.worktracker.model.Task;
import com.worktracker.model.TaskProcess;
import com.worktracker.model.WorkCategory;
import com.worktracker.util.HibernateUtil;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TaskServiceTest {

    private static final String TEST_DB_PATH = "worktracker_test.db";

    @BeforeAll
    public void setupAll() {
        // Direct Hibernate to use a separate test database file
        HibernateUtil.setDatabasePath(TEST_DB_PATH);
        DatabaseService.initializeDatabase();
    }

    @AfterAll
    public void tearDownAll() {
        HibernateUtil.shutdown();
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @BeforeEach
    public void clearDatabase() {
        // Clear tasks before each test to maintain isolation
        for (Task t : TaskService.getAllTasks()) {
            TaskService.deleteTask(t.getTaskId());
        }
    }

    @Test
    public void testTaskCrud() {
        Task task = new Task("TASK-001", "Develop Feature X", "Project A", "Client Y", "Development - New", "High", "Pending");
        task.setCompletionPercentage(0.0);
        TaskService.saveTask(task);

        Task retrieved = TaskService.getTaskById("TASK-001");
        assertNotNull(retrieved);
        assertEquals("Develop Feature X", retrieved.getTaskName());
        assertEquals("Project A", retrieved.getProject());

        // Test Update
        retrieved.setStatus("In Progress");
        TaskService.saveTask(retrieved);
        
        Task updated = TaskService.getTaskById("TASK-001");
        assertEquals("In Progress", updated.getStatus());

        // Test Delete
        TaskService.deleteTask("TASK-001");
        assertNull(TaskService.getTaskById("TASK-001"));
    }

    @Test
    public void testTaskIdAutoGeneration() {
        // Save first task
        Task task1 = new Task();
        task1.setTaskName("Test A");
        task1.setProject("P1");
        task1.setClient("C1");
        task1.setWorkCategory("Research");
        task1.setPriority("Low");
        task1.setStatus("Pending");
        TaskService.saveTask(task1);

        String generatedId = task1.getTaskId();
        assertTrue(generatedId.startsWith("TASK-"));

        // Generate next ID check
        String nextId = TaskService.generateNextTaskId();
        assertNotEquals(generatedId, nextId);
    }

    @Test
    public void testSubProcessCompletionRollup() {
        Task task = new Task("TASK-002", "Rollup Test", "Proj", "Client", "Research", "Low", "Pending");
        TaskService.saveTask(task);

        TaskProcess p1 = new TaskProcess("SubTask 1", task);
        p1.setCompletionPercentage(50.0);
        TaskService.saveProcess(p1);

        TaskProcess p2 = new TaskProcess("SubTask 2", task);
        p2.setCompletionPercentage(100.0);
        TaskService.saveProcess(p2);

        // Fetch task again and verify completion rollup is average (75.0%)
        Task updatedTask = TaskService.getTaskById("TASK-002");
        assertNotNull(updatedTask);
        assertEquals(75.0, updatedTask.getCompletionPercentage(), 0.1);
    }

    @Test
    public void testRevisionsLogging() {
        Task task = new Task("TASK-003", "Revision Test", "Proj", "Client", "QA", "Medium", "Pending");
        TaskService.saveTask(task);

        TaskService.addRevision("TASK-003", "Client requested design layout changes", 7200L, 85.0, "Modified styling rules.");

        Task updatedTask = TaskService.getTaskById("TASK-003");
        assertNotNull(updatedTask);
        assertEquals(1, updatedTask.getRevisions().size());
        assertEquals("In Progress", updatedTask.getStatus());
        assertEquals(85.0, updatedTask.getCompletionPercentage());
        assertEquals("Client requested design layout changes", updatedTask.getRevisions().get(0).getReason());
    }
}
