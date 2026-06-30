package com.worktracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @Column(name = "task_id", nullable = false, unique = true)
    private String taskId; // E.g., TASK-001 or manual ID

    @Column(nullable = false)
    private String project;

    @Column(nullable = false)
    private String client;

    @Column(name = "work_order")
    private String workOrder;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "work_category", nullable = false)
    private String workCategory;

    @Column(nullable = false)
    private String priority; // Low, Medium, High, Critical

    @Column(nullable = false)
    private String status; // Pending, In Progress, Completed, Paused

    @Column(name = "completion_percentage", nullable = false)
    private Double completionPercentage = 0.0;

    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TaskProcess> processes = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Revision> revisions = new ArrayList<>();

    public Task() {}

    public Task(String taskId, String taskName, String project, String client, String workCategory, String priority, String status) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.project = project;
        this.client = client;
        this.workCategory = workCategory;
        this.priority = priority;
        this.status = status;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(String workOrder) {
        this.workOrder = workOrder;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWorkCategory() {
        return workCategory;
    }

    public void setWorkCategory(String workCategory) {
        this.workCategory = workCategory;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<TaskProcess> getProcesses() {
        return processes;
    }

    public void setProcesses(List<TaskProcess> processes) {
        this.processes = processes;
    }

    public List<Revision> getRevisions() {
        return revisions;
    }

    public void setRevisions(List<Revision> revisions) {
        this.revisions = revisions;
    }

    // Helper methods
    public void addProcess(TaskProcess process) {
        processes.add(process);
        process.setTask(this);
    }

    public void removeProcess(TaskProcess process) {
        processes.remove(process);
        process.setTask(null);
    }

    public void addRevision(Revision revision) {
        revisions.add(revision);
        revision.setTask(this);
    }

    public void removeRevision(Revision revision) {
        revisions.remove(revision);
        revision.setTask(null);
    }
}
