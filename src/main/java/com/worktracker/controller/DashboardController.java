package com.worktracker.controller;

import com.worktracker.model.ActivityLog;
import com.worktracker.model.Task;
import com.worktracker.model.TaskProcess;
import com.worktracker.service.TaskService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Label lblTodayHours;
    @FXML private ProgressBar progressToday;
    
    @FXML private Label lblWeeklyHours;
    @FXML private ProgressBar progressWeekly;

    @FXML private Label lblMonthlyHours;
    @FXML private ProgressBar progressMonthly;

    @FXML private Label lblProductivity;
    @FXML private ProgressBar progressProductivity;

    @FXML private Label lblPendingTasks;
    @FXML private Label lblCompletedTasks;
    @FXML private Label lblCompletionPercent;
    @FXML private Label lblActiveTimer;

    // Timer Console
    @FXML private ComboBox<Task> comboActiveTask;
    @FXML private ComboBox<TaskProcess> comboActiveProcess;
    @FXML private Label lblConsoleTimer;
    @FXML private Button btnStart;
    @FXML private Button btnPause;
    @FXML private Button btnResume;
    @FXML private Button btnStop;
    @FXML private Label lblTimerWorking;
    @FXML private Label lblTimerBreak;
    @FXML private Label lblTimerIdle;
    @FXML private TextArea txtTimerNotes;

    @FXML private ListView<String> listActivities;

    private static DashboardController instance;

    public static DashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;

        // Populate tasks dropdown
        refreshTasksDropdown();

        // Setup converters for Comboboxes
        setupComboboxConverters();

        // Load dashboard stats
        refreshMetrics();

        // Load activities
        refreshActivities();

        // Set initial timer button states
        updateTimerControlsUI();

        // If a timer is already running globally, sync details
        if (!"STOPPED".equals(MainController.timerStatus)) {
            syncRunningTimerToCombobox();
        }
    }

    private void setupComboboxConverters() {
        comboActiveTask.setConverter(new StringConverter<Task>() {
            @Override
            public String toString(Task task) {
                return task == null ? "" : task.getTaskId() + " - " + task.getTaskName();
            }
            @Override
            public Task fromString(String string) {
                return null;
            }
        });

        comboActiveTask.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Reload task eagerly to fetch sub-processes from the DB
                Task task = TaskService.getTaskById(newVal.getTaskId());
                List<TaskProcess> processes = task.getProcesses();
                comboActiveProcess.setItems(FXCollections.observableArrayList(processes));
            } else {
                comboActiveProcess.setItems(FXCollections.emptyObservableList());
            }
        });

        comboActiveProcess.setConverter(new StringConverter<TaskProcess>() {
            @Override
            public String toString(TaskProcess process) {
                return process == null ? "" : process.getProcessName() + " (" + process.getCompletionPercentage() + "%)";
            }
            @Override
            public TaskProcess fromString(String string) {
                return null;
            }
        });
    }

    public void refreshTasksDropdown() {
        List<Task> activeTasks = TaskService.getAllTasks().stream()
                .filter(t -> !"Completed".equals(t.getStatus()))
                .collect(Collectors.toList());
        comboActiveTask.setItems(FXCollections.observableArrayList(activeTasks));
    }

    private void syncRunningTimerToCombobox() {
        if (MainController.activeTask != null) {
            // Find task in dropdown items
            for (Task t : comboActiveTask.getItems()) {
                if (t.getTaskId().equals(MainController.activeTask.getTaskId())) {
                    comboActiveTask.setValue(t);
                    break;
                }
            }
            if (MainController.activeProcess != null) {
                for (TaskProcess p : comboActiveProcess.getItems()) {
                    if (p.getId().equals(MainController.activeProcess.getId())) {
                        comboActiveProcess.setValue(p);
                        break;
                    }
                }
            }
        }
    }

    public void refreshMetrics() {
        Platform.runLater(() -> {
            try {
                List<Task> allTasks = TaskService.getAllTasks();

                long pending = allTasks.stream().filter(t -> !"Completed".equals(t.getStatus())).count();
                long completed = allTasks.stream().filter(t -> "Completed".equals(t.getStatus())).count();
                double completionRate = allTasks.isEmpty() ? 0.0 : (double) completed / allTasks.size() * 100.0;

                lblPendingTasks.setText(String.valueOf(pending));
                lblCompletedTasks.setText(String.valueOf(completed));
                lblCompletionPercent.setText(String.format("%.1f%%", completionRate));

                // 2. Fetch Working Hours
                LocalDate today = LocalDate.now();
                LocalDateTime startOfToday = today.atStartOfDay();
                LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
                
                LocalDateTime startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                LocalDateTime startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();

                double todayHrs = TaskService.getWorkingHoursForDateRange(startOfToday, endOfToday);
                double weeklyHrs = TaskService.getWorkingHoursForDateRange(startOfWeek, endOfToday);
                double monthlyHrs = TaskService.getWorkingHoursForDateRange(startOfMonth, endOfToday);

                lblTodayHours.setText(String.format("%.1f hrs", todayHrs));
                progressToday.setProgress(Math.min(1.0, todayHrs / 8.0)); // 8 hours daily target

                lblWeeklyHours.setText(String.format("%.1f hrs", weeklyHrs));
                progressWeekly.setProgress(Math.min(1.0, weeklyHrs / 40.0)); // 40 hours weekly target

                lblMonthlyHours.setText(String.format("%.1f hrs", monthlyHrs));
                progressMonthly.setProgress(Math.min(1.0, monthlyHrs / 160.0)); // 160 hours monthly target

                // 3. Productivity = (Working Hours / Shift Log Hours) * 100
                double shiftHrsToday = TaskService.getShiftHoursForDateRange(startOfToday, endOfToday);
                double productivity = 0.0;
                if (shiftHrsToday > 0) {
                    productivity = Math.min(100.0, (todayHrs / shiftHrsToday) * 100.0);
                } else if (todayHrs > 0) {
                    productivity = 100.0; // If they worked but forgot to login shift
                }
                lblProductivity.setText(String.format("%.1f%%", productivity));
                progressProductivity.setProgress(productivity / 100.0);

            } catch (Exception e) {
                logger.error("Error loading metrics", e);
            }
        });
    }

    public void refreshActivities() {
        Platform.runLater(() -> {
            List<ActivityLog> logs = TaskService.getRecentActivities(20);
            List<String> listItems = logs.stream()
                    .map(log -> {
                        String time = log.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                        return time + " - " + log.getActivityText();
                    })
                    .collect(Collectors.toList());
            listActivities.setItems(FXCollections.observableArrayList(listItems));
        });
    }

    public void refreshTimerConsoleDisplay() {
        Platform.runLater(() -> {
            long displaySeconds = "PAUSED".equals(MainController.timerStatus) ? MainController.timerSecondsBreak : MainController.timerSecondsWorked;
            lblConsoleTimer.setText(formatTime(displaySeconds));
            lblActiveTimer.setText(formatTime(MainController.timerSecondsWorked));

            lblTimerWorking.setText(formatTime(MainController.timerSecondsWorked));
            lblTimerBreak.setText(formatTime(MainController.timerSecondsBreak));
            lblTimerIdle.setText(formatTime(MainController.timerSecondsIdle));
        });
    }

    private String formatTime(long totalSecs) {
        long hours = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long secs = totalSecs % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    @FXML
    private void handleQuickNewTask(ActionEvent event) {
        // 1. Instantly generate a new Task ID and save a draft task in the DB
        String nextId = TaskService.generateNextTaskId();
        Task task = new Task();
        task.setTaskId(nextId);
        task.setTaskName("Quick Task (" + nextId + ")");
        task.setProject("General");
        task.setClient("General");
        
        List<com.worktracker.model.WorkCategory> categories = TaskService.getCategories();
        String defaultCat = categories.isEmpty() ? "Development - New" : categories.get(0).getName();
        task.setWorkCategory(defaultCat);
        task.setPriority("Medium");
        task.setStatus("In Progress");
        task.setAssignedDate(LocalDate.now());
        task.setCompletionPercentage(0.0);
        
        TaskService.saveTask(task);
        Task reloaded = TaskService.getTaskById(nextId); // eager reload

        // 2. Start global timer immediately
        MainController.startTimer(reloaded, null);

        // 3. Refresh dropdowns, select the new task, and update UI controls
        refreshTasksDropdown();
        
        // Find and select in combo
        for (Task t : comboActiveTask.getItems()) {
            if (t.getTaskId().equals(reloaded.getTaskId())) {
                comboActiveTask.setValue(t);
                break;
            }
        }
        comboActiveProcess.setValue(null);

        refreshMetrics();
        refreshActivities();
        updateTimerControlsUI();
    }

    @FXML
    private void handleStartTimer(ActionEvent event) {
        Task selectedTask = comboActiveTask.getValue();
        if (selectedTask == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a task to start tracking.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        TaskProcess selectedProcess = comboActiveProcess.getValue();
        
        // Change parent task status to In Progress
        if ("Pending".equals(selectedTask.getStatus())) {
            selectedTask.setStatus("In Progress");
            TaskService.saveTask(selectedTask);
        }

        MainController.startTimer(selectedTask, selectedProcess);
        updateTimerControlsUI();
    }

    @FXML
    private void handlePauseTimer(ActionEvent event) {
        MainController.pauseTimer();
        updateTimerControlsUI();
    }

    @FXML
    private void handleResumeTimer(ActionEvent event) {
        MainController.resumeTimer();
        updateTimerControlsUI();
    }

    @FXML
    private void handleStopTimer(ActionEvent event) {
        String notes = txtTimerNotes.getText();
        MainController.stopTimer(notes);
        
        txtTimerNotes.clear();
        comboActiveTask.setValue(null);
        comboActiveProcess.setValue(null);
        
        updateTimerControlsUI();
        refreshMetrics();
        refreshActivities();
        refreshTasksDropdown();
    }

    private void updateTimerControlsUI() {
        String status = MainController.timerStatus;
        if ("STOPPED".equals(status)) {
            comboActiveTask.setDisable(false);
            comboActiveProcess.setDisable(false);
            btnStart.setDisable(false);
            btnPause.setDisable(true);
            btnResume.setDisable(true);
            btnStop.setDisable(true);
            lblConsoleTimer.setText("00:00:00");
        } else if ("RUNNING".equals(status)) {
            comboActiveTask.setDisable(true);
            comboActiveProcess.setDisable(true);
            btnStart.setDisable(true);
            btnPause.setDisable(false);
            btnResume.setDisable(true);
            btnStop.setDisable(false);
        } else if ("PAUSED".equals(status)) {
            comboActiveTask.setDisable(true);
            comboActiveProcess.setDisable(true);
            btnStart.setDisable(true);
            btnPause.setDisable(true);
            btnResume.setDisable(false);
            btnStop.setDisable(false);
        }
    }
}
