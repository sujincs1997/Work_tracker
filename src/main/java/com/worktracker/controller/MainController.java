package com.worktracker.controller;

import com.worktracker.model.AppSettings;
import com.worktracker.model.ShiftLog;
import com.worktracker.model.Task;
import com.worktracker.model.TaskProcess;
import com.worktracker.service.BackupService;
import com.worktracker.service.DatabaseService;
import com.worktracker.service.TaskService;
import com.worktracker.util.HibernateUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private Button btnDashboard;
    @FXML private Button btnTasks;
    @FXML private Button btnTimeTracking;
    @FXML private Button btnCategories;
    @FXML private Button btnImport;
    @FXML private Button btnReports;
    @FXML private Button btnSettings;

    @FXML private Label lblShiftStatus;
    @FXML private ComboBox<String> comboShiftType;
    @FXML private Button btnShiftAction;

    @FXML private Label lblHeaderTitle;
    @FXML private Label lblRunningTask;
    @FXML private Label lblHeaderTimer;
    @FXML private Button btnThemeToggle;

    @FXML private StackPane contentArea;

    // Singleton / Global Controller Reference
    private static MainController instance;

    // Global Timer State
    public static Task activeTask;
    public static TaskProcess activeProcess;
    public static long timerSecondsWorked = 0;
    public static long timerSecondsBreak = 0;
    public static long timerSecondsIdle = 0;
    public static String timerStatus = "STOPPED"; // RUNNING, PAUSED, STOPPED
    private static Timeline globalTimeline;
    private static long lastInputTime = System.currentTimeMillis();
    private static final long IDLE_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes

    // Shift State
    private ShiftLog activeShift;

    public static MainController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        
        // Populate Shift Types
        comboShiftType.setItems(FXCollections.observableArrayList("General", "Morning", "Evening", "Night"));
        comboShiftType.setValue("General");

        // Load Shift Status from Database on startup
        activeShift = TaskService.getActiveShift();
        updateShiftUI();

        // Setup global timer check
        setupGlobalTimer();

        // Load Default View (Dashboard)
        showDashboard();

        // Apply theme settings
        applyCurrentTheme();
        
        // Listen to scene keypress/mouse events for idle tracking
        Platform.runLater(() -> {
            Scene scene = contentArea.getScene();
            if (scene != null) {
                scene.addEventFilter(javafx.scene.input.MouseEvent.ANY, e -> lastInputTime = System.currentTimeMillis());
                scene.addEventFilter(javafx.scene.input.KeyEvent.ANY, e -> lastInputTime = System.currentTimeMillis());
            }
        });
    }

    private void setupGlobalTimer() {
        globalTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if ("RUNNING".equals(timerStatus)) {
                long now = System.currentTimeMillis();
                if (now - lastInputTime > IDLE_THRESHOLD_MS) {
                    timerSecondsIdle++;
                } else {
                    timerSecondsWorked++;
                }
                updateTimerDisplay();
            } else if ("PAUSED".equals(timerStatus)) {
                timerSecondsBreak++;
                updateTimerDisplay();
            }
        }));
        globalTimeline.setCycleCount(Timeline.INDEFINITE);
        globalTimeline.play();
    }

    public void updateTimerDisplay() {
        long displaySeconds = "PAUSED".equals(timerStatus) ? timerSecondsBreak : timerSecondsWorked;
        long hours = displaySeconds / 3600;
        long minutes = (displaySeconds % 3600) / 60;
        long secs = displaySeconds % 60;
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, secs);

        lblHeaderTimer.setText(timeString);

        if (activeTask != null) {
            String procName = activeProcess != null ? " (" + activeProcess.getProcessName() + ")" : "";
            lblRunningTask.setText(activeTask.getTaskId() + " - " + activeTask.getTaskName() + procName);
        } else {
            lblRunningTask.setText("No Active Task");
        }

        // Notify Dashboard Controller if it's active
        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().refreshTimerConsoleDisplay();
        }
    }

    // Timer Controls API
    public static void startTimer(Task task, TaskProcess process) {
        if (!"STOPPED".equals(timerStatus)) {
            stopTimer("Saved automatically when starting another task");
        }
        activeTask = task;
        activeProcess = process;
        timerSecondsWorked = 0;
        timerSecondsBreak = 0;
        timerSecondsIdle = 0;
        timerStatus = "RUNNING";
        lastInputTime = System.currentTimeMillis();
        
        TaskService.logActivity("Started Timer on Task " + task.getTaskId() + (process != null ? " Process: " + process.getProcessName() : ""));
        
        if (instance != null) {
            instance.updateTimerDisplay();
        }
    }

    public static void pauseTimer() {
        if ("RUNNING".equals(timerStatus)) {
            timerStatus = "PAUSED";
            TaskService.logActivity("Paused Timer on Task " + (activeTask != null ? activeTask.getTaskId() : ""));
        }
    }

    public static void resumeTimer() {
        if ("PAUSED".equals(timerStatus)) {
            timerStatus = "RUNNING";
            lastInputTime = System.currentTimeMillis();
            TaskService.logActivity("Resumed Timer on Task " + (activeTask != null ? activeTask.getTaskId() : ""));
        }
    }

    public static void stopTimer(String notes) {
        if (!"STOPPED".equals(timerStatus) && activeTask != null) {
            timerStatus = "STOPPED";
            
            // Save duration to Sub-Process or parent Task
            if (activeProcess != null) {
                activeProcess.setDurationSeconds(activeProcess.getDurationSeconds() + timerSecondsWorked);
                if (notes != null && !notes.trim().isEmpty()) {
                    String existing = activeProcess.getNotes() != null ? activeProcess.getNotes() + "\n" : "";
                    activeProcess.setNotes(existing + LocalDateTime.now() + ": " + notes);
                }
                if (activeProcess.getStartTime() == null) {
                    activeProcess.setStartTime(LocalDateTime.now().minusSeconds(timerSecondsWorked));
                }
                activeProcess.setEndTime(LocalDateTime.now());
                TaskService.saveProcess(activeProcess);
            } else {
                // If no sub-process, we create a generic process called "General Tracking"
                TaskProcess proc = new TaskProcess("General Tracking", activeTask);
                proc.setDurationSeconds(timerSecondsWorked);
                proc.setNotes(notes);
                proc.setStartTime(LocalDateTime.now().minusSeconds(timerSecondsWorked));
                proc.setEndTime(LocalDateTime.now());
                proc.setCompletionPercentage(activeTask.getCompletionPercentage());
                TaskService.saveProcess(proc);
            }

            TaskService.logActivity("Stopped Timer. Worked: " + timerSecondsWorked + "s. Break: " + timerSecondsBreak + "s. Idle: " + timerSecondsIdle + "s.");
            
            activeTask = null;
            activeProcess = null;
            timerSecondsWorked = 0;
            timerSecondsBreak = 0;
            timerSecondsIdle = 0;

            if (instance != null) {
                instance.updateTimerDisplay();
            }
        }
    }

    // =========================================================================
    // VIEW NAVIGATION
    // =========================================================================

    public void showDashboard() {
        loadView("dashboard", "Dashboard Overview", btnDashboard);
    }

    public void showTasks() {
        loadView("tasks", "Task Management", btnTasks);
    }

    public void showTimeTracking() {
        loadView("time_tracking", "Shift & Time Tracking", btnTimeTracking);
    }

    public void showCategories() {
        loadView("categories", "Work Categories", btnCategories);
    }

    public void showImport() {
        loadView("import", "Import Wizard", btnImport);
    }

    public void showReports() {
        loadView("reports", "Reports & Analytics", btnReports);
    }

    public void showSettings() {
        loadView("settings", "Settings", btnSettings);
    }

    private void loadView(String fxmlName, String title, Button activeBtn) {
        try {
            lblHeaderTitle.setText(title);
            
            // Highlight active button in sidebar
            resetButtonStyles();
            activeBtn.getStyleClass().add("active");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlName + ".fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            logger.info("Loaded view: {}", fxmlName);
        } catch (IOException e) {
            logger.error("Failed to load view: " + fxmlName, e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not load view: " + fxmlName + "\n" + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void resetButtonStyles() {
        btnDashboard.getStyleClass().remove("active");
        btnTasks.getStyleClass().remove("active");
        btnTimeTracking.getStyleClass().remove("active");
        btnCategories.getStyleClass().remove("active");
        btnImport.getStyleClass().remove("active");
        btnReports.getStyleClass().remove("active");
        btnSettings.getStyleClass().remove("active");
    }

    // =========================================================================
    // SHIFT CONTROLS
    // =========================================================================

    @FXML
    private void handleShiftAction(ActionEvent event) {
        if (activeShift == null) {
            // Log in
            String type = comboShiftType.getValue();
            if (type == null) type = "General";
            activeShift = TaskService.startShift(type);
            updateShiftUI();
        } else {
            // Log out (Prompt for break time minutes)
            TextInputDialog dialog = new TextInputDialog("0");
            dialog.setTitle("Log Out Shift");
            dialog.setHeaderText("Log Out: " + activeShift.getShiftType() + " Shift");
            dialog.setContentText("Enter total break duration in minutes:");
            dialog.showAndWait().ifPresent(minsStr -> {
                long breakMins = 0;
                try {
                    breakMins = Long.parseLong(minsStr);
                } catch (NumberFormatException e) {
                    // Default to 0
                }
                TaskService.endShift(activeShift.getId(), breakMins * 60);
                activeShift = null;
                updateShiftUI();
                
                // If dynamic shifts table is active, refresh it
                if (TimeTrackingController.getInstance() != null) {
                    TimeTrackingController.getInstance().loadShiftHistory();
                }
                if (DashboardController.getInstance() != null) {
                    DashboardController.getInstance().refreshMetrics();
                }
            });
        }
    }

    private void updateShiftUI() {
        if (activeShift == null) {
            lblShiftStatus.setText("Offline");
            lblShiftStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;"); // Red
            btnShiftAction.setText("Login");
            btnShiftAction.getStyleClass().setAll("button", "btn-success");
            comboShiftType.setDisable(false);
        } else {
            lblShiftStatus.setText("Active: " + activeShift.getShiftType());
            lblShiftStatus.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;"); // Green
            btnShiftAction.setText("Logout");
            btnShiftAction.getStyleClass().setAll("button", "btn-danger");
            comboShiftType.setValue(activeShift.getShiftType());
            comboShiftType.setDisable(true);
        }
    }

    // =========================================================================
    // THEME MANAGEMENT
    // =========================================================================

    @FXML
    private void toggleTheme(ActionEvent event) {
        AppSettings settings = DatabaseService.getSettings();
        if (settings != null) {
            String newTheme = "Light".equals(settings.getTheme()) ? "Dark" : "Light";
            settings.setTheme(newTheme);
            DatabaseService.saveSettings(settings);
            applyCurrentTheme();
        }
    }

    public void applyCurrentTheme() {
        AppSettings settings = DatabaseService.getSettings();
        if (settings == null) return;
        
        String themeFile = "Dark".equals(settings.getTheme()) ? "/css/dark.css" : "/css/light.css";
        btnThemeToggle.setText("Dark".equals(settings.getTheme()) ? "🌙" : "☀️");
        
        Platform.runLater(() -> {
            Scene scene = contentArea.getScene();
            if (scene != null) {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource(themeFile).toExternalForm());
                logger.info("Applied theme stylesheet: {}", themeFile);
            }
        });
    }

    public void onShutdown() {
        // Stop timer
        if (!"STOPPED".equals(timerStatus)) {
            stopTimer("Auto-saved during application shutdown");
        }
        
        // Auto-backup
        AppSettings settings = DatabaseService.getSettings();
        if (settings != null && settings.isAutoBackup()) {
            logger.info("Executing auto-backup on application exit...");
            BackupService.performBackup();
        }
        
        HibernateUtil.shutdown();
    }
}
