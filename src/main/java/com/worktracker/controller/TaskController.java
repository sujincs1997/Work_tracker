package com.worktracker.controller;

import com.worktracker.model.Revision;
import com.worktracker.model.Task;
import com.worktracker.model.TaskProcess;
import com.worktracker.model.WorkCategory;
import com.worktracker.service.TaskService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskController {
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    // Filter controls
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboCategoryFilter;
    @FXML private ComboBox<String> comboStatusFilter;
    @FXML private ComboBox<String> comboPriorityFilter;
    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;

    // Task table
    @FXML private TableView<Task> tableTasks;
    @FXML private TableColumn<Task, String> colTaskId;
    @FXML private TableColumn<Task, String> colTaskName;
    @FXML private TableColumn<Task, String> colProject;
    @FXML private TableColumn<Task, String> colClient;
    @FXML private TableColumn<Task, String> colCategory;
    @FXML private TableColumn<Task, String> colPriority;
    @FXML private TableColumn<Task, String> colStatus;
    @FXML private TableColumn<Task, Double> colCompletion;
    @FXML private TableColumn<Task, LocalDate> colAssignedDate;

    // Form Details
    @FXML private TabPane tabPaneTaskDetails;
    @FXML private Tab tabProcesses;
    @FXML private Tab tabRevisions;
    @FXML private Label lblFormTitle;
    @FXML private TextField txtTaskId;
    @FXML private TextField txtTaskName;
    @FXML private ComboBox<String> comboProjectForm;
    @FXML private TextField txtClient;
    @FXML private TextField txtWorkOrder;
    @FXML private ComboBox<WorkCategory> comboCategory;
    @FXML private ComboBox<String> comboPriority;
    @FXML private ComboBox<String> comboStatus;
    @FXML private Slider sliderCompletion;
    @FXML private Label lblCompletionVal;
    @FXML private DatePicker dpAssignedDate;
    @FXML private DatePicker dpDueDate;
    @FXML private TextArea txtDescription;
    @FXML private TextArea txtNotes;

    // Sub-Processes
    @FXML private TextField txtNewProcessName;
    @FXML private TableView<TaskProcess> tableProcesses;
    @FXML private TableColumn<TaskProcess, String> colProcName;
    @FXML private TableColumn<TaskProcess, Double> colProcComp;
    @FXML private TableColumn<TaskProcess, String> colProcDuration;
    @FXML private TableColumn<TaskProcess, Void> colProcActions;

    // Revisions
    @FXML private TextField txtRevReason;
    @FXML private TextField txtRevHours;
    @FXML private TextField txtRevMinutes;
    @FXML private Slider sliderRevCompletion;
    @FXML private Label lblRevCompVal;
    @FXML private TextArea txtRevNotes;
    @FXML private ListView<String> listRevisions;

    private Task selectedTask;

    @FXML
    public void initialize() {
        // Initialize filters
        setupFilters();

        // Initialize Table Columns
        setupTableColumns();

        // Load tasks list
        loadTasks();

        // Table selection listener
        tableTasks.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadTaskDetails(newSelection);
            }
        });

        // Setup completion slider listeners
        sliderCompletion.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblCompletionVal.setText(String.format("%.1f%%", newVal.doubleValue()));
        });
        sliderRevCompletion.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblRevCompVal.setText(String.format("%.0f%%", newVal.doubleValue()));
        });

        // Set default form values
        clearForm();
    }

    private void setupFilters() {
        // Load categories for filter
        List<String> categories = TaskService.getCategories().stream().map(WorkCategory::getName).collect(Collectors.toList());
        categories.add(0, "All");
        comboCategoryFilter.setItems(FXCollections.observableArrayList(categories));
        comboCategoryFilter.setValue("All");

        comboStatusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "In Progress", "Completed", "Paused"));
        comboStatusFilter.setValue("All");

        comboPriorityFilter.setItems(FXCollections.observableArrayList("All", "Low", "Medium", "High", "Critical"));
        comboPriorityFilter.setValue("All");

        // Load categories in form combo
        List<WorkCategory> catObjects = TaskService.getCategories();
        comboCategory.setItems(FXCollections.observableArrayList(catObjects));

        comboPriority.setItems(FXCollections.observableArrayList("Low", "Medium", "High", "Critical"));
        comboStatus.setItems(FXCollections.observableArrayList("Pending", "In Progress", "Completed", "Paused"));
        refreshProjectFormDropdown();
    }

    private void refreshProjectFormDropdown() {
        try {
            java.util.Set<String> projects = TaskService.getAllTasks().stream()
                    .map(Task::getProject)
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .collect(Collectors.toSet());
            comboProjectForm.setItems(FXCollections.observableArrayList(projects).sorted());
        } catch (Exception e) {
            logger.error("Failed to load project suggestions", e);
        }
    }

    private void setupTableColumns() {
        colTaskId.setCellValueFactory(new PropertyValueFactory<>("taskId"));
        colTaskName.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("project"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("client"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("workCategory"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCompletion.setCellValueFactory(new PropertyValueFactory<>("completionPercentage"));
        colAssignedDate.setCellValueFactory(new PropertyValueFactory<>("assignedDate"));
    }

    public void loadTasks() {
        tableTasks.setItems(FXCollections.observableArrayList(TaskService.getAllTasks()));
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String text = txtSearch.getText();
        String cat = comboCategoryFilter.getValue();
        String stat = comboStatusFilter.getValue();
        String prio = comboPriorityFilter.getValue();
        LocalDate from = dpFromDate.getValue();
        LocalDate to = dpToDate.getValue();

        List<Task> results = TaskService.searchTasks(text, cat, stat, prio, from, to);
        tableTasks.setItems(FXCollections.observableArrayList(results));
    }

    @FXML
    private void handleClearFilters(ActionEvent event) {
        txtSearch.clear();
        comboCategoryFilter.setValue("All");
        comboStatusFilter.setValue("All");
        comboPriorityFilter.setValue("All");
        dpFromDate.setValue(null);
        dpToDate.setValue(null);
        loadTasks();
    }

    private void loadTaskDetails(Task task) {
        selectedTask = TaskService.getTaskById(task.getTaskId()); // reload eager relation
        if (selectedTask == null) return;

        lblFormTitle.setText("Edit Task: " + selectedTask.getTaskId());
        txtTaskId.setText(selectedTask.getTaskId());
        txtTaskId.setDisable(true); // Don't let primary key change
        txtTaskName.setText(selectedTask.getTaskName());
        comboProjectForm.setValue(selectedTask.getProject());
        txtClient.setText(selectedTask.getClient());
        txtWorkOrder.setText(selectedTask.getWorkOrder() != null ? selectedTask.getWorkOrder() : "");

        // Find Category in combo items
        for (WorkCategory cat : comboCategory.getItems()) {
            if (cat.getName().equals(selectedTask.getWorkCategory())) {
                comboCategory.setValue(cat);
                break;
            }
        }

        comboPriority.setValue(selectedTask.getPriority());
        comboStatus.setValue(selectedTask.getStatus());
        sliderCompletion.setValue(selectedTask.getCompletionPercentage());
        lblCompletionVal.setText(String.format("%.1f%%", selectedTask.getCompletionPercentage()));
        dpAssignedDate.setValue(selectedTask.getAssignedDate());
        dpDueDate.setValue(selectedTask.getDueDate());
        txtDescription.setText(selectedTask.getDescription() != null ? selectedTask.getDescription() : "");
        txtNotes.setText(selectedTask.getNotes() != null ? selectedTask.getNotes() : "");

        // Enable sub tabs
        tabProcesses.setDisable(false);
        tabRevisions.setDisable(false);

        // Load Sub-Processes
        loadSubProcesses();

        // Load Revisions
        loadRevisions();
    }

    private void clearForm() {
        selectedTask = null;
        lblFormTitle.setText("Create New Task");
        txtTaskId.clear();
        txtTaskId.setDisable(false);
        txtTaskName.clear();
        comboProjectForm.setValue("");
        txtClient.clear();
        txtWorkOrder.clear();
        comboCategory.setValue(null);
        comboPriority.setValue("Medium");
        comboStatus.setValue("Pending");
        sliderCompletion.setValue(0);
        lblCompletionVal.setText("0.0%");
        dpAssignedDate.setValue(LocalDate.now());
        dpDueDate.setValue(null);
        txtDescription.clear();
        txtNotes.clear();

        // Disable sub tabs since task is not created yet
        tabProcesses.setDisable(true);
        tabRevisions.setDisable(true);
    }

    @FXML
    private void handleNewTask(ActionEvent event) {
        // 1. Instantly generate a new Task ID
        String nextId = TaskService.generateNextTaskId();
        
        // 2. Pre-create a draft task in the database
        selectedTask = new Task();
        selectedTask.setTaskId(nextId);
        selectedTask.setTaskName("Quick Task (" + nextId + ")");
        selectedTask.setProject("General");
        selectedTask.setClient("General");
        
        List<WorkCategory> categories = TaskService.getCategories();
        String defaultCat = categories.isEmpty() ? "Development - New" : categories.get(0).getName();
        selectedTask.setWorkCategory(defaultCat);
        selectedTask.setPriority("Medium");
        selectedTask.setStatus("In Progress");
        selectedTask.setAssignedDate(LocalDate.now());
        selectedTask.setCompletionPercentage(0.0);
        
        // Save to DB immediately so it is tracked
        TaskService.saveTask(selectedTask);
        selectedTask = TaskService.getTaskById(nextId); // eager reload

        // 3. Load draft details into UI form
        lblFormTitle.setText("Edit Task: " + selectedTask.getTaskId());
        txtTaskId.setText(selectedTask.getTaskId());
        txtTaskId.setDisable(true);
        txtTaskName.setText(selectedTask.getTaskName());
        comboProjectForm.setValue(selectedTask.getProject());
        txtClient.setText(selectedTask.getClient());
        txtWorkOrder.clear();
        
        for (WorkCategory cat : comboCategory.getItems()) {
            if (cat.getName().equals(selectedTask.getWorkCategory())) {
                comboCategory.setValue(cat);
                break;
            }
        }
        
        comboPriority.setValue(selectedTask.getPriority());
        comboStatus.setValue(selectedTask.getStatus());
        sliderCompletion.setValue(0.0);
        lblCompletionVal.setText("0.0%");
        dpAssignedDate.setValue(selectedTask.getAssignedDate());
        dpDueDate.setValue(null);
        txtDescription.clear();
        txtNotes.clear();

        // Enable processes and revisions
        tabProcesses.setDisable(false);
        tabRevisions.setDisable(false);
        loadSubProcesses();
        loadRevisions();

        // 4. Refresh lists and highlight new task
        loadTasks();
        tableTasks.getSelectionModel().select(selectedTask);

        // 5. Start timer immediately
        MainController.startTimer(selectedTask, null);

        // 6. Refresh Dashboard combo if active
        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().refreshTasksDropdown();
            DashboardController.getInstance().refreshMetrics();
            DashboardController.getInstance().refreshActivities();
        }

        // 7. Request focus on Task Name field so the user can easily rename it while timer ticks
        txtTaskName.requestFocus();
        txtTaskName.selectAll();
    }

    @FXML
    private void handleCancelForm(ActionEvent event) {
        tableTasks.getSelectionModel().clearSelection();
        clearForm();
    }

    @FXML
    private void handleSaveTask(ActionEvent event) {
        String projName = comboProjectForm.getEditor().getText().trim();
        if (txtTaskName.getText().trim().isEmpty() || projName.isEmpty() 
            || txtClient.getText().trim().isEmpty() || comboCategory.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please fill in all required fields (*) before saving.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Task task = selectedTask;
        if (task == null) {
            task = new Task();
            String id = txtTaskId.getText().trim();
            if (!id.isEmpty()) {
                // Manual ID validation
                Task existing = TaskService.getTaskById(id);
                if (existing != null) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "A task with ID " + id + " already exists. Please choose a different ID.", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                task.setTaskId(id);
            }
        }

        task.setTaskName(txtTaskName.getText().trim());
        task.setProject(comboProjectForm.getEditor().getText().trim());
        task.setClient(txtClient.getText().trim());
        task.setWorkOrder(txtWorkOrder.getText().trim());
        task.setWorkCategory(comboCategory.getValue().getName());
        task.setPriority(comboPriority.getValue());
        task.setStatus(comboStatus.getValue());
        task.setCompletionPercentage(sliderCompletion.getValue());
        task.setAssignedDate(dpAssignedDate.getValue());
        task.setDueDate(dpDueDate.getValue());
        task.setDescription(txtDescription.getText());
        task.setNotes(txtNotes.getText());

        TaskService.saveTask(task);

        // Refresh lists
        loadTasks();
        refreshProjectFormDropdown();
        
        // Highlight task
        tableTasks.getSelectionModel().select(task);
        
        // Refresh dashboard drops
        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().refreshTasksDropdown();
            DashboardController.getInstance().refreshMetrics();
            DashboardController.getInstance().refreshActivities();
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Task saved successfully!", ButtonType.OK);
        alert.showAndWait();
    }

    // =========================================================================
    // SUB-PROCESSES BREAKDOWN
    // =========================================================================

    private void loadSubProcesses() {
        if (selectedTask == null) return;
        
        colProcName.setCellValueFactory(new PropertyValueFactory<>("processName"));
        colProcComp.setCellValueFactory(new PropertyValueFactory<>("completionPercentage"));
        
        colProcDuration.setCellValueFactory(cellData -> {
            long sec = cellData.getValue().getDurationSeconds();
            long h = sec / 3600;
            long m = (sec % 3600) / 60;
            long s = sec % 60;
            return new SimpleStringProperty(String.format("%02d:%02d:%02d", h, m, s));
        });

        // Add actions column (Start timer inline, Delete process)
        colProcActions.setCellFactory(param -> new TableCell<TaskProcess, Void>() {
            private final Button btnProcStart = new Button("▶");
            private final Button btnProcDelete = new Button("❌");
            private final HBox container = new HBox(5, btnProcStart, btnProcDelete);

            {
                btnProcStart.setStyle("-fx-background-color: transparent; -fx-text-fill: -primary-color; -fx-cursor: hand; -fx-padding: 2;");
                btnProcDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: -danger; -fx-cursor: hand; -fx-padding: 2;");
                
                btnProcStart.setOnAction(event -> {
                    TaskProcess proc = getTableView().getItems().get(getIndex());
                    if ("RUNNING".equals(MainController.timerStatus)) {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "A timer is already running. Please stop the current timer first.", ButtonType.OK);
                        alert.showAndWait();
                        return;
                    }
                    MainController.startTimer(selectedTask, proc);
                    MainController.getInstance().showDashboard();
                });

                btnProcDelete.setOnAction(event -> {
                    TaskProcess proc = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete process: " + proc.getProcessName() + "?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            TaskService.deleteProcess(proc.getId());
                            loadTaskDetails(selectedTask);
                            loadTasks();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });

        tableProcesses.setItems(FXCollections.observableArrayList(selectedTask.getProcesses()));
        
        // Double-click row to update completion %
        tableProcesses.setRowFactory(tv -> {
            TableRow<TaskProcess> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    TaskProcess rowData = row.getItem();
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(rowData.getCompletionPercentage()));
                    dialog.setTitle("Update Process Progress");
                    dialog.setHeaderText("Set Completion % for: " + rowData.getProcessName());
                    dialog.setContentText("Completion Percentage (0-100):");
                    dialog.showAndWait().ifPresent(valStr -> {
                        try {
                            double percent = Double.parseDouble(valStr);
                            if (percent < 0 || percent > 100) throw new NumberFormatException();
                            rowData.setCompletionPercentage(percent);
                            TaskService.saveProcess(rowData);
                            loadTaskDetails(selectedTask);
                            loadTasks();
                        } catch (NumberFormatException e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid percentage. Please input a decimal value between 0 and 100.", ButtonType.OK);
                            alert.showAndWait();
                        }
                    });
                }
            });
            return row;
        });
    }

    @FXML
    private void handleAddProcess(ActionEvent event) {
        String procName = txtNewProcessName.getText().trim();
        if (procName.isEmpty()) return;

        if (selectedTask == null) return;

        TaskProcess process = new TaskProcess(procName, selectedTask);
        process.setCompletionPercentage(0.0);
        process.setDurationSeconds(0L);

        TaskService.saveProcess(process);
        txtNewProcessName.clear();

        loadTaskDetails(selectedTask);
        loadTasks();
    }

    @FXML
    private void handleDeleteProcess(ActionEvent event) {
        TaskProcess proc = tableProcesses.getSelectionModel().getSelectedItem();
        if (proc != null) {
            TaskService.deleteProcess(proc.getId());
            loadTaskDetails(selectedTask);
            loadTasks();
        }
    }

    // =========================================================================
    // REVISIONS HISTORY
    // =========================================================================

    private void loadRevisions() {
        if (selectedTask == null) return;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<String> items = selectedTask.getRevisions().stream()
                .map(r -> {
                    double hrs = r.getTimeSpentSeconds() / 3600.0;
                    return String.format("Rev #%d (%s) - %s\nTime Spent: %.2f hrs | Completion: %.1f%%\nNotes: %s",
                            r.getRevisionNumber(),
                            r.getDate().format(dtf),
                            r.getReason(),
                            hrs,
                            r.getCompletionPercentage(),
                            r.getNotes() != null ? r.getNotes() : ""
                    );
                })
                .collect(Collectors.toList());

        listRevisions.setItems(FXCollections.observableArrayList(items));
    }

    @FXML
    private void handleAddRevision(ActionEvent event) {
        if (selectedTask == null) return;

        String reason = txtRevReason.getText().trim();
        if (reason.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter a reason for the revision.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        long hours = 0;
        long minutes = 0;
        try {
            if (!txtRevHours.getText().trim().isEmpty()) hours = Long.parseLong(txtRevHours.getText().trim());
            if (!txtRevMinutes.getText().trim().isEmpty()) minutes = Long.parseLong(txtRevMinutes.getText().trim());
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Hours and Minutes must be numbers.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        long totalSeconds = (hours * 3600) + (minutes * 60);
        double comp = sliderRevCompletion.getValue();
        String notes = txtRevNotes.getText();

        TaskService.addRevision(selectedTask.getTaskId(), reason, totalSeconds, comp, notes);

        // Clear revision form
        txtRevReason.clear();
        txtRevHours.clear();
        txtRevMinutes.clear();
        sliderRevCompletion.setValue(0);
        txtRevNotes.clear();

        // Refresh
        loadTaskDetails(selectedTask);
        loadTasks();

        if (DashboardController.getInstance() != null) {
            DashboardController.getInstance().refreshMetrics();
            DashboardController.getInstance().refreshActivities();
        }
    }

    // =========================================================================
    // CONTEXT MENUS & ACTIONS
    // =========================================================================

    @FXML
    private void handleContextMenuStartTimer(ActionEvent event) {
        Task task = tableTasks.getSelectionModel().getSelectedItem();
        if (task != null) {
            if ("RUNNING".equals(MainController.timerStatus)) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "A timer is already running. Please stop the current timer first.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            if ("Completed".equals(task.getStatus())) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Cannot start timer on completed tasks.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            // Start timer
            MainController.startTimer(task, null);
            MainController.getInstance().showDashboard();
        }
    }

    @FXML
    private void handleContextMenuEdit(ActionEvent event) {
        Task task = tableTasks.getSelectionModel().getSelectedItem();
        if (task != null) {
            tabPaneTaskDetails.getSelectionModel().select(0); // Focus details tab
        }
    }

    @FXML
    private void handleContextMenuRevision(ActionEvent event) {
        Task task = tableTasks.getSelectionModel().getSelectedItem();
        if (task != null) {
            tabPaneTaskDetails.getSelectionModel().select(2); // Focus revision tab
        }
    }

    @FXML
    private void handleContextMenuDelete(ActionEvent event) {
        Task task = tableTasks.getSelectionModel().getSelectedItem();
        if (task != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete task: " + task.getTaskId() + "? This action is irreversible.", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    if (MainController.activeTask != null && MainController.activeTask.getTaskId().equals(task.getTaskId())) {
                        MainController.stopTimer("Forced stop due to task deletion");
                    }
                    TaskService.deleteTask(task.getTaskId());
                    loadTasks();
                    handleNewTask(null);
                    
                    if (DashboardController.getInstance() != null) {
                        DashboardController.getInstance().refreshTasksDropdown();
                        DashboardController.getInstance().refreshMetrics();
                        DashboardController.getInstance().refreshActivities();
                    }
                }
            });
        }
    }
}
