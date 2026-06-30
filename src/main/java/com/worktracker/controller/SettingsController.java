package com.worktracker.controller;

import com.worktracker.model.AppSettings;
import com.worktracker.service.DatabaseService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @FXML private TextField txtDbPath;
    @FXML private TextField txtBackupPath;
    @FXML private TextField txtExportPath;
    @FXML private CheckBox chkAutoBackup;
    @FXML private ComboBox<String> comboTheme;

    @FXML private TextField txtShiftMorningStart;
    @FXML private TextField txtShiftMorningEnd;
    @FXML private TextField txtShiftGeneralStart;
    @FXML private TextField txtShiftGeneralEnd;
    @FXML private TextField txtShiftEveningStart;
    @FXML private TextField txtShiftEveningEnd;
    @FXML private TextField txtShiftNightStart;
    @FXML private TextField txtShiftNightEnd;

    private AppSettings settings;

    @FXML
    public void initialize() {
        comboTheme.setItems(FXCollections.observableArrayList("Light", "Dark"));
        loadSettings();
    }

    private void loadSettings() {
        settings = DatabaseService.getSettings();
        if (settings == null) {
            settings = new AppSettings();
        }

        txtDbPath.setText(settings.getDatabaseLocation());
        txtBackupPath.setText(settings.getBackupFolder());
        txtExportPath.setText(settings.getExportFolder());
        chkAutoBackup.setSelected(settings.isAutoBackup());
        comboTheme.setValue(settings.getTheme());

        txtShiftMorningStart.setText(settings.getShiftMorningStart());
        txtShiftMorningEnd.setText(settings.getShiftMorningEnd());
        txtShiftGeneralStart.setText(settings.getShiftGeneralStart());
        txtShiftGeneralEnd.setText(settings.getShiftGeneralEnd());
        txtShiftEveningStart.setText(settings.getShiftEveningStart());
        txtShiftEveningEnd.setText(settings.getShiftEveningEnd());
        txtShiftNightStart.setText(settings.getShiftNightStart());
        txtShiftNightEnd.setText(settings.getShiftNightEnd());
    }

    @FXML
    private void handleBrowseDb(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select SQLite Database Location");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database files (*.db)", "*.db"));
        
        File currentDb = new File(txtDbPath.getText());
        if (currentDb.exists() && currentDb.getParentFile() != null) {
            chooser.setInitialDirectory(currentDb.getParentFile());
        }

        Stage stage = (Stage) txtDbPath.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            txtDbPath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleBrowseBackup(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Backup Directory");
        
        File currentBackup = new File(txtBackupPath.getText());
        if (currentBackup.exists()) {
            chooser.setInitialDirectory(currentBackup);
        }

        Stage stage = (Stage) txtBackupPath.getScene().getWindow();
        File folder = chooser.showDialog(stage);
        if (folder != null) {
            txtBackupPath.setText(folder.getAbsolutePath());
        }
    }

    @FXML
    private void handleBrowseExport(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Exports Directory");

        File currentExport = new File(txtExportPath.getText());
        if (currentExport.exists()) {
            chooser.setInitialDirectory(currentExport);
        }

        Stage stage = (Stage) txtExportPath.getScene().getWindow();
        File folder = chooser.showDialog(stage);
        if (folder != null) {
            txtExportPath.setText(folder.getAbsolutePath());
        }
    }

    @FXML
    private void handleSaveSettings(ActionEvent event) {
        settings.setDatabaseLocation(txtDbPath.getText().trim());
        settings.setBackupFolder(txtBackupPath.getText().trim());
        settings.setExportFolder(txtExportPath.getText().trim());
        settings.setAutoBackup(chkAutoBackup.isSelected());
        settings.setTheme(comboTheme.getValue());

        settings.setShiftMorningStart(txtShiftMorningStart.getText().trim());
        settings.setShiftMorningEnd(txtShiftMorningEnd.getText().trim());
        settings.setShiftGeneralStart(txtShiftGeneralStart.getText().trim());
        settings.setShiftGeneralEnd(txtShiftGeneralEnd.getText().trim());
        settings.setShiftEveningStart(txtShiftEveningStart.getText().trim());
        settings.setShiftEveningEnd(txtShiftEveningEnd.getText().trim());
        settings.setShiftNightStart(txtShiftNightStart.getText().trim());
        settings.setShiftNightEnd(txtShiftNightEnd.getText().trim());

        try {
            DatabaseService.saveSettings(settings);
            
            // Apply theme immediately if main controller is present
            if (MainController.getInstance() != null) {
                MainController.getInstance().applyCurrentTheme();
                MainController.getInstance().initialize(); // Re-read settings/shifts
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Settings updated successfully!", ButtonType.OK);
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save settings: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
            logger.error("Failed settings save", e);
        }
    }

    @FXML
    private void handleResetDefaults(ActionEvent event) {
        AppSettings defaults = new AppSettings();
        
        txtDbPath.setText(defaults.getDatabaseLocation());
        txtBackupPath.setText(defaults.getBackupFolder());
        txtExportPath.setText(defaults.getExportFolder());
        chkAutoBackup.setSelected(defaults.isAutoBackup());
        comboTheme.setValue(defaults.getTheme());

        txtShiftMorningStart.setText(defaults.getShiftMorningStart());
        txtShiftMorningEnd.setText(defaults.getShiftMorningEnd());
        txtShiftGeneralStart.setText(defaults.getShiftGeneralStart());
        txtShiftGeneralEnd.setText(defaults.getShiftGeneralEnd());
        txtShiftEveningStart.setText(defaults.getShiftEveningStart());
        txtShiftEveningEnd.setText(defaults.getShiftEveningEnd());
        txtShiftNightStart.setText(defaults.getShiftNightStart());
        txtShiftNightEnd.setText(defaults.getShiftNightEnd());
    }
}
