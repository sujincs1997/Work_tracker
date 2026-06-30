package com.worktracker.controller;

import com.worktracker.model.ShiftLog;
import com.worktracker.service.TaskService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TimeTrackingController {
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingController.class);

    @FXML private Label lblShiftStatusDetail;
    @FXML private ComboBox<String> comboShift;
    @FXML private Spinner<Integer> spinnerBreakTime;
    @FXML private Button btnCheckIn;
    @FXML private Button btnCheckOut;

    @FXML private TableView<ShiftLog> tableShifts;
    @FXML private TableColumn<ShiftLog, Long> colShiftId;
    @FXML private TableColumn<ShiftLog, String> colShiftType;
    @FXML private TableColumn<ShiftLog, String> colLoginTime;
    @FXML private TableColumn<ShiftLog, String> colLogoutTime;
    @FXML private TableColumn<ShiftLog, Long> colBreakTime;
    @FXML private TableColumn<ShiftLog, Double> colWorkedHours;

    private static TimeTrackingController instance;

    public static TimeTrackingController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;

        // Setup Spinners
        SpinnerValueFactory<Integer> breakValFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 480, 30, 5);
        spinnerBreakTime.setValueFactory(breakValFactory);

        // Populate Shift combo
        comboShift.setItems(FXCollections.observableArrayList("General", "Morning", "Evening", "Night"));
        comboShift.setValue("General");

        setupTableColumns();
        loadShiftHistory();
        updateShiftControls();
    }

    private void setupTableColumns() {
        colShiftId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colShiftType.setCellValueFactory(new PropertyValueFactory<>("shiftType"));
        colBreakTime.setCellValueFactory(new PropertyValueFactory<>("breakTimeSeconds"));
        colWorkedHours.setCellValueFactory(new PropertyValueFactory<>("totalHours"));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        colLoginTime.setCellValueFactory(cellData -> {
            if (cellData.getValue().getLoginTime() != null) {
                return new SimpleStringProperty(cellData.getValue().getLoginTime().format(dtf));
            }
            return new SimpleStringProperty("");
        });

        colLogoutTime.setCellValueFactory(cellData -> {
            if (cellData.getValue().getLogoutTime() != null) {
                return new SimpleStringProperty(cellData.getValue().getLogoutTime().format(dtf));
            }
            return new SimpleStringProperty("Currently Checked In");
        });
    }

    public void loadShiftHistory() {
        List<ShiftLog> shifts = TaskService.getRecentShifts();
        tableShifts.setItems(FXCollections.observableArrayList(shifts));
    }

    public void updateShiftControls() {
        ShiftLog active = TaskService.getActiveShift();
        if (active == null) {
            lblShiftStatusDetail.setText("Offline. Please select a shift and log in.");
            btnCheckIn.setDisable(false);
            btnCheckOut.setDisable(true);
            comboShift.setDisable(false);
            spinnerBreakTime.setDisable(true);
        } else {
            lblShiftStatusDetail.setText("Checked In: " + active.getShiftType() + " since " + active.getLoginTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            btnCheckIn.setDisable(true);
            btnCheckOut.setDisable(false);
            comboShift.setValue(active.getShiftType());
            comboShift.setDisable(true);
            spinnerBreakTime.setDisable(false);
        }
    }

    @FXML
    private void handleCheckIn(ActionEvent event) {
        String type = comboShift.getValue();
        if (type == null) type = "General";
        
        TaskService.startShift(type);
        updateShiftControls();
        loadShiftHistory();
        
        // Sync with MainController sidebar status
        if (MainController.getInstance() != null) {
            // MainController initializes and checks shifts, so re-initialize or run logic
            MainController.getInstance().initialize();
        }
    }

    @FXML
    private void handleCheckOut(ActionEvent event) {
        ShiftLog active = TaskService.getActiveShift();
        if (active != null) {
            int breakMins = spinnerBreakTime.getValue();
            TaskService.endShift(active.getId(), (long) breakMins * 60);
            
            updateShiftControls();
            loadShiftHistory();

            // Sync with MainController sidebar status and metrics
            if (MainController.getInstance() != null) {
                MainController.getInstance().initialize();
            }
            if (DashboardController.getInstance() != null) {
                DashboardController.getInstance().refreshMetrics();
            }
        }
    }
}
