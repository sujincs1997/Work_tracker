package com.worktracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "app_settings")
public class AppSettings {

    @Id
    private Long id = 1L; // Single row for system settings

    @Column(nullable = false)
    private String theme = "Light"; // Light or Dark

    @Column(name = "database_location")
    private String databaseLocation = "worktracker.db";

    @Column(name = "backup_folder")
    private String backupFolder = "backups";

    @Column(name = "auto_backup", nullable = false)
    private boolean autoBackup = true;

    @Column(name = "export_folder")
    private String exportFolder = "exports";

    @Column(name = "shift_morning_start")
    private String shiftMorningStart = "06:00";

    @Column(name = "shift_morning_end")
    private String shiftMorningEnd = "14:00";

    @Column(name = "shift_general_start")
    private String shiftGeneralStart = "09:00";

    @Column(name = "shift_general_end")
    private String shiftGeneralEnd = "18:00";

    @Column(name = "shift_evening_start")
    private String shiftEveningStart = "14:00";

    @Column(name = "shift_evening_end")
    private String shiftEveningEnd = "22:00";

    @Column(name = "shift_night_start")
    private String shiftNightStart = "22:00";

    @Column(name = "shift_night_end")
    private String shiftNightEnd = "06:00";

    public AppSettings() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getDatabaseLocation() {
        return databaseLocation;
    }

    public void setDatabaseLocation(String databaseLocation) {
        this.databaseLocation = databaseLocation;
    }

    public String getBackupFolder() {
        return backupFolder;
    }

    public void setBackupFolder(String backupFolder) {
        this.backupFolder = backupFolder;
    }

    public boolean isAutoBackup() {
        return autoBackup;
    }

    public void setAutoBackup(boolean autoBackup) {
        this.autoBackup = autoBackup;
    }

    public String getExportFolder() {
        return exportFolder;
    }

    public void setExportFolder(String exportFolder) {
        this.exportFolder = exportFolder;
    }

    public String getShiftMorningStart() {
        return shiftMorningStart;
    }

    public void setShiftMorningStart(String shiftMorningStart) {
        this.shiftMorningStart = shiftMorningStart;
    }

    public String getShiftMorningEnd() {
        return shiftMorningEnd;
    }

    public void setShiftMorningEnd(String shiftMorningEnd) {
        this.shiftMorningEnd = shiftMorningEnd;
    }

    public String getShiftGeneralStart() {
        return shiftGeneralStart;
    }

    public void setShiftGeneralStart(String shiftGeneralStart) {
        this.shiftGeneralStart = shiftGeneralStart;
    }

    public String getShiftGeneralEnd() {
        return shiftGeneralEnd;
    }

    public void setShiftGeneralEnd(String shiftGeneralEnd) {
        this.shiftGeneralEnd = shiftGeneralEnd;
    }

    public String getShiftEveningStart() {
        return shiftEveningStart;
    }

    public void setShiftEveningStart(String shiftEveningStart) {
        this.shiftEveningStart = shiftEveningStart;
    }

    public String getShiftEveningEnd() {
        return shiftEveningEnd;
    }

    public void setShiftEveningEnd(String shiftEveningEnd) {
        this.shiftEveningEnd = shiftEveningEnd;
    }

    public String getShiftNightStart() {
        return shiftNightStart;
    }

    public void setShiftNightStart(String shiftNightStart) {
        this.shiftNightStart = shiftNightStart;
    }

    public String getShiftNightEnd() {
        return shiftNightEnd;
    }

    public void setShiftNightEnd(String shiftNightEnd) {
        this.shiftNightEnd = shiftNightEnd;
    }
}
