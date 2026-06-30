package com.worktracker.controller;

import com.worktracker.model.AppSettings;
import com.worktracker.model.Revision;
import com.worktracker.model.Task;
import com.worktracker.model.TaskProcess;
import com.worktracker.service.DatabaseService;
import com.worktracker.service.ImportExportService;
import com.worktracker.service.TaskService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private ComboBox<String> comboProject;
    @FXML private ComboBox<String> comboClient;

    @FXML private PieChart chartCategory;
    @FXML private BarChart<String, Number> chartProject;
    @FXML private LineChart<String, Number> chartProductivity;
    @FXML private PieChart chartStatus;

    private List<Task> allTasks;

    @FXML
    public void initialize() {
        // Set default date range to current month
        dpFrom.setValue(LocalDate.now().withDayOfMonth(1));
        dpTo.setValue(LocalDate.now());

        refreshFilterOptions();
        loadReportData();
    }

    private void refreshFilterOptions() {
        allTasks = TaskService.getAllTasks();

        Set<String> projects = allTasks.stream().map(Task::getProject).filter(Objects::nonNull).collect(Collectors.toSet());
        List<String> projList = new ArrayList<>(projects);
        Collections.sort(projList);
        projList.add(0, "All Projects");
        comboProject.setItems(FXCollections.observableArrayList(projList));
        comboProject.setValue("All Projects");

        Set<String> clients = allTasks.stream().map(Task::getClient).filter(Objects::nonNull).collect(Collectors.toSet());
        List<String> clientList = new ArrayList<>(clients);
        Collections.sort(clientList);
        clientList.add(0, "All Clients");
        comboClient.setItems(FXCollections.observableArrayList(clientList));
        comboClient.setValue("All Clients");
    }

    private void loadReportData() {
        Platform.runLater(() -> {
            List<Task> filteredTasks = getFilteredTasks();
            renderCategoryChart(filteredTasks);
            renderProjectChart(filteredTasks);
            renderStatusChart(filteredTasks);
            renderProductivityChart();
        });
    }

    private List<Task> getFilteredTasks() {
        String proj = comboProject.getValue();
        String client = comboClient.getValue();
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        return allTasks.stream()
                .filter(t -> {
                    if (proj != null && !proj.equals("All Projects") && !proj.equals(t.getProject())) return false;
                    if (client != null && !client.equals("All Clients") && !client.equals(t.getClient())) return false;
                    if (from != null && t.getAssignedDate() != null && t.getAssignedDate().isBefore(from)) return false;
                    if (to != null && t.getAssignedDate() != null && t.getAssignedDate().isAfter(to)) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void renderCategoryChart(List<Task> tasks) {
        chartCategory.getData().clear();
        Map<String, Double> timeMap = new HashMap<>();

        for (Task t : tasks) {
            String cat = t.getWorkCategory();
            double hours = 0.0;
            for (TaskProcess p : t.getProcesses()) {
                hours += p.getDurationSeconds() / 3600.0;
            }
            for (Revision r : t.getRevisions()) {
                hours += r.getTimeSpentSeconds() / 3600.0;
            }

            if (hours > 0.0) {
                timeMap.put(cat, timeMap.getOrDefault(cat, 0.0) + hours);
            }
        }

        if (timeMap.isEmpty()) {
            chartCategory.setTitle("No time data available");
            return;
        }

        chartCategory.setTitle("Hours by Category");
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        timeMap.forEach((cat, hrs) -> data.add(new PieChart.Data(String.format("%s (%.1fh)", cat, hrs), hrs)));
        chartCategory.setData(data);
    }

    private void renderProjectChart(List<Task> tasks) {
        chartProject.getData().clear();
        Map<String, Double> timeMap = new HashMap<>();

        for (Task t : tasks) {
            String proj = t.getProject();
            double hours = 0.0;
            for (TaskProcess p : t.getProcesses()) {
                hours += p.getDurationSeconds() / 3600.0;
            }
            for (Revision r : t.getRevisions()) {
                hours += r.getTimeSpentSeconds() / 3600.0;
            }

            if (hours > 0.0) {
                timeMap.put(proj, timeMap.getOrDefault(proj, 0.0) + hours);
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Hours");
        timeMap.forEach((proj, hrs) -> series.getData().add(new XYChart.Data<>(proj, hrs)));
        
        chartProject.getData().add(series);
    }

    private void renderStatusChart(List<Task> tasks) {
        chartStatus.getData().clear();
        Map<String, Long> statusMap = tasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));

        if (statusMap.isEmpty()) {
            chartStatus.setTitle("No task status data available");
            return;
        }

        chartStatus.setTitle("Task Status Counts");
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        statusMap.forEach((status, count) -> data.add(new PieChart.Data(String.format("%s (%d)", status, count), count)));
        chartStatus.setData(data);
    }

    private void renderProductivityChart() {
        chartProductivity.getData().clear();
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        if (from == null || to == null) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Productivity %");

        long days = ChronoUnit.DAYS.between(from, to);
        // Cap days to 30 to avoid messy charts
        LocalDate startDay = (days > 30) ? to.minusDays(30) : from;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd");

        for (LocalDate date = startDay; !date.isAfter(to); date = date.plusDays(1)) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            double workHrs = TaskService.getWorkingHoursForDateRange(start, end);
            double shiftHrs = TaskService.getShiftHoursForDateRange(start, end);

            double productivity = 0.0;
            if (shiftHrs > 0) {
                productivity = Math.min(100.0, (workHrs / shiftHrs) * 100.0);
            } else if (workHrs > 0) {
                productivity = 100.0;
            }

            series.getData().add(new XYChart.Data<>(date.format(dtf), productivity));
        }

        chartProductivity.getData().add(series);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        refreshFilterOptions();
        loadReportData();
    }

    @FXML
    private void handleExportPdf(ActionEvent event) {
        exportReport("PDF", "*.pdf", "pdf");
    }

    @FXML
    private void handleExportExcel(ActionEvent event) {
        exportReport("Excel", "*.xlsx", "excel");
    }

    @FXML
    private void handleExportCsv(ActionEvent event) {
        exportReport("CSV", "*.csv", "csv");
    }

    private void exportReport(String label, String extension, String format) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Tasks Report");
        chooser.setInitialFileName("tasks_report_" + LocalDate.now() + "." + (format.equals("excel") ? "xlsx" : format));
        
        AppSettings settings = DatabaseService.getSettings();
        if (settings != null && settings.getExportFolder() != null) {
            File exportDir = new File(settings.getExportFolder());
            if (exportDir.exists()) {
                chooser.setInitialDirectory(exportDir);
            }
        }
        
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(label + " files", extension));

        Stage stage = (Stage) dpFrom.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            try {
                List<Task> filtered = getFilteredTasks();
                ImportExportService.exportTasks(file, filtered, format);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Report exported successfully to:\n" + file.getAbsolutePath(), ButtonType.OK);
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
                logger.error("Failed report export", e);
            }
        }
    }
}
