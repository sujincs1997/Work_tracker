package com.worktracker.controller;

import com.worktracker.service.ImportExportService;
import com.worktracker.service.TaskService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class ImportController {
    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);

    @FXML private Label lblStep1;
    @FXML private Label lblStep2;
    @FXML private Label lblStep3;

    @FXML private VBox paneStep1;
    @FXML private VBox paneStep2;
    @FXML private VBox paneStep3;

    @FXML private TextField txtFilePath;
    @FXML private Label lblFileInfo;
    @FXML private Label lblImportValidationInfo;

    @FXML private GridPane gridMapping;
    @FXML private TableView<String[]> tablePreview;

    @FXML private RadioButton radioSkip;
    @FXML private RadioButton radioOverwrite;

    @FXML private Button btnBack;
    @FXML private Button btnNext;
    @FXML private Button btnImportExecute;

    private int currentStep = 1;
    private File selectedFile;
    private List<String[]> fileRows = new ArrayList<>();
    private String[] headers;

    // Database fields to map
    private static final String[] DB_FIELDS = {
            "taskId", "taskName", "project", "client", "workOrder", 
            "workCategory", "priority", "status", "completionPercentage", 
            "assignedDate", "dueDate", "notes"
    };

    private static final String[] DB_FIELD_LABELS = {
            "Task ID", "Task Name *", "Project *", "Client *", "Work Order", 
            "Work Category *", "Priority *", "Status *", "Completion %", 
            "Assigned Date", "Due Date", "Notes"
    };

    private final Map<String, ComboBox<String>> mappingCombos = new HashMap<>();

    @FXML
    public void initialize() {
        showStep(1);
    }

    private void showStep(int step) {
        currentStep = step;
        paneStep1.setVisible(step == 1);
        paneStep2.setVisible(step == 2);
        paneStep3.setVisible(step == 3);

        // Highlight steps
        lblStep1.setStyle(step == 1 ? "-fx-font-weight: bold; -fx-text-fill: -primary-color;" : "-fx-font-weight: bold; -fx-text-fill: -text-muted;");
        lblStep2.setStyle(step == 2 ? "-fx-font-weight: bold; -fx-text-fill: -primary-color;" : "-fx-font-weight: bold; -fx-text-fill: -text-muted;");
        lblStep3.setStyle(step == 3 ? "-fx-font-weight: bold; -fx-text-fill: -primary-color;" : "-fx-font-weight: bold; -fx-text-fill: -text-muted;");

        // Footer buttons
        btnBack.setVisible(step > 1);
        btnNext.setVisible(step < 3);
        btnImportExecute.setVisible(step == 3);
    }

    @FXML
    private void handleBrowseFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Task Spreadsheet");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Spreadsheets (*.xlsx, *.xls, *.csv)", "*.xlsx", "*.xls", "*.csv"),
                new FileChooser.ExtensionFilter("Excel files", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("CSV files", "*.csv")
        );

        Stage stage = (Stage) txtFilePath.getScene().getWindow();
        selectedFile = chooser.showOpenDialog(stage);

        if (selectedFile != null) {
            txtFilePath.setText(selectedFile.getAbsolutePath());
            try {
                fileRows = ImportExportService.previewFile(selectedFile);
                if (fileRows.isEmpty()) {
                    lblFileInfo.setText("File is empty.");
                    lblFileInfo.setStyle("-fx-text-fill: -danger;");
                } else {
                    headers = fileRows.get(0);
                    lblFileInfo.setText(String.format("Loaded successfully. Found %d rows and %d columns.", fileRows.size() - 1, headers.length));
                    lblFileInfo.setStyle("-fx-text-fill: -success;");
                }
            } catch (Exception e) {
                lblFileInfo.setText("Error reading file: " + e.getMessage());
                lblFileInfo.setStyle("-fx-text-fill: -danger;");
                logger.error("Error reading import file", e);
                selectedFile = null;
            }
        }
    }

    @FXML
    private void handleNext(ActionEvent event) {
        if (currentStep == 1) {
            if (selectedFile == null || fileRows.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a valid CSV or Excel file first.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            buildMappingGrid();
            showStep(2);
        } else if (currentStep == 2) {
            // Validate mapping
            if (!validateRequiredMappings()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please map at least Task Name, Project, Client, and Category.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            buildPreviewTable();
            showStep(3);
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    private void buildMappingGrid() {
        gridMapping.getChildren().clear();
        mappingCombos.clear();

        List<String> colOptions = new ArrayList<>();
        colOptions.add("--- None ---");
        for (int i = 0; i < headers.length; i++) {
            colOptions.add(String.format("Column %d: %s", i + 1, headers[i]));
        }

        for (int i = 0; i < DB_FIELDS.length; i++) {
            String field = DB_FIELDS[i];
            String labelText = DB_FIELD_LABELS[i];

            Label label = new Label(labelText);
            label.setStyle("-fx-font-weight: bold;");
            
            ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(colOptions));
            combo.setValue("--- None ---");
            combo.setMaxWidth(Double.MAX_VALUE);

            // Auto matching logic
            autoMatchField(field, headers, combo);

            mappingCombos.put(field, combo);

            gridMapping.add(label, 0, i);
            gridMapping.add(combo, 1, i);
        }
    }

    private void autoMatchField(String dbField, String[] fileHeaders, ComboBox<String> combo) {
        String cleanField = dbField.toLowerCase();
        for (int i = 0; i < fileHeaders.length; i++) {
            String cleanHeader = fileHeaders[i].toLowerCase().replace(" ", "").replace("_", "").replace("-", "");
            
            boolean match = false;
            if (cleanField.equals(cleanHeader)) match = true;
            else if (cleanField.equals("taskid") && (cleanHeader.contains("taskid") || cleanHeader.equals("id"))) match = true;
            else if (cleanField.equals("taskname") && (cleanHeader.contains("name") || cleanHeader.contains("title"))) match = true;
            else if (cleanField.equals("workcategory") && (cleanHeader.contains("category") || cleanHeader.contains("type"))) match = true;
            else if (cleanField.equals("workorder") && (cleanHeader.contains("order") || cleanHeader.contains("wo"))) match = true;
            else if (cleanField.equals("completionpercentage") && (cleanHeader.contains("completion") || cleanHeader.contains("progress") || cleanHeader.contains("%"))) match = true;
            else if (cleanField.contains("date") && cleanHeader.contains(cleanField.replace("date", ""))) match = true;
            else if (cleanHeader.contains(cleanField) || cleanField.contains(cleanHeader)) match = true;

            if (match) {
                combo.getSelectionModel().select(i + 1); // Index 0 is "--- None ---"
                break;
            }
        }
    }

    private boolean validateRequiredMappings() {
        // Required: taskName, project, client, workCategory
        return isMapped("taskName") && isMapped("project") && isMapped("client") && isMapped("workCategory");
    }

    private boolean isMapped(String field) {
        ComboBox<String> combo = mappingCombos.get(field);
        return combo != null && combo.getSelectionModel().getSelectedIndex() > 0;
    }

    private Map<String, Integer> getFieldMappings() {
        Map<String, Integer> mappings = new HashMap<>();
        for (String field : DB_FIELDS) {
            ComboBox<String> combo = mappingCombos.get(field);
            int idx = combo.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                mappings.put(field, idx - 1); // Map to actual 0-indexed data column
            }
        }
        return mappings;
    }

    private void buildPreviewTable() {
        tablePreview.getColumns().clear();

        Map<String, Integer> mappings = getFieldMappings();
        
        // Setup columns based on mapped fields
        for (String field : DB_FIELDS) {
            Integer fileColIdx = mappings.get(field);
            if (fileColIdx != null) {
                int dbFieldIdx = Arrays.asList(DB_FIELDS).indexOf(field);
                String label = DB_FIELD_LABELS[dbFieldIdx].replace(" *", "");
                
                final int finalColIdx = fileColIdx;
                TableColumn<String[], String> col = new TableColumn<>(label + " (Col " + (fileColIdx + 1) + ")");
                col.setCellValueFactory(cellData -> {
                    String[] row = cellData.getValue();
                    if (row != null && finalColIdx < row.length) {
                        return new SimpleStringProperty(row[finalColIdx]);
                    }
                    return new SimpleStringProperty("");
                });
                tablePreview.getColumns().add(col);
            }
        }

        // Add first 5 data rows (excluding headers)
        int previewRowsCount = Math.min(6, fileRows.size());
        List<String[]> previewList = new ArrayList<>();
        for (int i = 1; i < previewRowsCount; i++) {
            previewList.add(fileRows.get(i));
        }

        tablePreview.setItems(FXCollections.observableArrayList(previewList));

        lblImportValidationInfo.setText(String.format("Mapped columns. Ready to import %d tasks from %s.", fileRows.size() - 1, selectedFile.getName()));
    }

    @FXML
    private void handleImportExecute(ActionEvent event) {
        Map<String, Integer> mappings = getFieldMappings();
        boolean overwrite = radioOverwrite.isSelected();
        boolean skip = radioSkip.isSelected();

        // Extract only data rows (exclude header row)
        List<String[]> dataRows = fileRows.subList(1, fileRows.size());

        try {
            int importedCount = ImportExportService.executeImport(dataRows, mappings, overwrite, skip);
            
            // Log import activity
            TaskService.logActivity(String.format("Imported %d tasks from file: %s", importedCount, selectedFile.getName()));

            Alert alert = new Alert(Alert.AlertType.INFORMATION, String.format("Successfully imported %d tasks!", importedCount), ButtonType.OK);
            alert.showAndWait();

            // Refresh Task lists & dashboard
            if (TaskController.class != null) {
                // If they go to tasks later, it will be reloaded
            }
            if (DashboardController.getInstance() != null) {
                DashboardController.getInstance().refreshTasksDropdown();
                DashboardController.getInstance().refreshMetrics();
                DashboardController.getInstance().refreshActivities();
            }

            // Return to step 1
            txtFilePath.clear();
            selectedFile = null;
            fileRows.clear();
            showStep(1);

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Import failed: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
            logger.error("Import failure", e);
        }
    }
}
