package com.worktracker.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_logs")
public class ShiftLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "break_time_seconds")
    private Long breakTimeSeconds = 0L;

    @Column(name = "shift_type", nullable = false)
    private String shiftType; // Morning, General, Evening, Night

    @Column(name = "total_hours")
    private Double totalHours = 0.0; // Total calculated shift hours (logout - login - break)

    public ShiftLog() {}

    public ShiftLog(LocalDateTime loginTime, String shiftType) {
        this.loginTime = loginTime;
        this.shiftType = shiftType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public Long getBreakTimeSeconds() {
        return breakTimeSeconds;
    }

    public void setBreakTimeSeconds(Long breakTimeSeconds) {
        this.breakTimeSeconds = breakTimeSeconds;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Double totalHours) {
        this.totalHours = totalHours;
    }
}
