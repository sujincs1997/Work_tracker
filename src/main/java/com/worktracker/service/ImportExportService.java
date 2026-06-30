package com.worktracker.service;

import com.worktracker.model.Task;
import com.worktracker.util.CsvHelper;
import com.worktracker.util.ExcelHelper;
import com.worktracker.util.PdfGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportExportService {
    private static final Logger logger = LoggerFactory.getLogger(ImportExportService.class);

    public static List<String[]> previewFile(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".csv")) {
            try {
                return CsvHelper.readCsv(file);
            } catch (Exception e) {
                throw new IOException("Failed to parse CSV: " + e.getMessage(), e);
            }
        } else if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
            return ExcelHelper.readExcel(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format. Please import CSV or Excel.");
        }
    }

    public static int executeImport(List<String[]> dataRows, Map<String, Integer> fieldMapping, 
                                    boolean overwriteDuplicates, boolean skipDuplicates) {
        int count = 0;
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        // First row is usually header, skip if header mapping is used
        // We assume dataRows contains only data, or if it has header we handle it at view side.
        for (String[] row : dataRows) {
            try {
                String taskId = getMappedValue(row, fieldMapping, "taskId");
                String taskName = getMappedValue(row, fieldMapping, "taskName");
                String project = getMappedValue(row, fieldMapping, "project");
                String client = getMappedValue(row, fieldMapping, "client");
                String workOrder = getMappedValue(row, fieldMapping, "workOrder");
                String category = getMappedValue(row, fieldMapping, "workCategory");
                String priority = getMappedValue(row, fieldMapping, "priority");
                String status = getMappedValue(row, fieldMapping, "status");
                String compStr = getMappedValue(row, fieldMapping, "completionPercentage");
                String assignedStr = getMappedValue(row, fieldMapping, "assignedDate");
                String dueStr = getMappedValue(row, fieldMapping, "dueDate");
                String notes = getMappedValue(row, fieldMapping, "notes");

                // Defaults if blank
                if (taskName == null || taskName.trim().isEmpty()) continue; // Required field
                if (project == null || project.trim().isEmpty()) project = "General";
                if (client == null || client.trim().isEmpty()) client = "General";
                if (category == null || category.trim().isEmpty()) category = "Development - New";
                if (priority == null || priority.trim().isEmpty()) priority = "Medium";
                if (status == null || status.trim().isEmpty()) status = "Pending";

                double completion = 0.0;
                if (compStr != null && !compStr.trim().isEmpty()) {
                    try {
                        completion = Double.parseDouble(compStr.replace("%", "").trim());
                    } catch (NumberFormatException e) {
                        // ignore, default to 0.0
                    }
                }

                LocalDate assigned = LocalDate.now();
                if (assignedStr != null && !assignedStr.trim().isEmpty()) {
                    assigned = parseLocalDate(assignedStr, formatters);
                }

                LocalDate due = null;
                if (dueStr != null && !dueStr.trim().isEmpty()) {
                    due = parseLocalDate(dueStr, formatters);
                }

                // Check duplicate
                boolean isDuplicate = false;
                if (taskId != null && !taskId.trim().isEmpty()) {
                    Task existing = TaskService.getTaskById(taskId);
                    if (existing != null) {
                        isDuplicate = true;
                        if (skipDuplicates) {
                            continue; // Skip it
                        }
                    }
                }

                Task task = null;
                if (isDuplicate && overwriteDuplicates) {
                    task = TaskService.getTaskById(taskId);
                }
                
                if (task == null) {
                    task = new Task();
                    task.setTaskId(taskId != null && !taskId.trim().isEmpty() ? taskId : TaskService.generateNextTaskId());
                }

                task.setTaskName(taskName);
                task.setProject(project);
                task.setClient(client);
                task.setWorkOrder(workOrder);
                task.setWorkCategory(category);
                task.setPriority(priority);
                task.setStatus(status);
                task.setCompletionPercentage(completion);
                task.setAssignedDate(assigned);
                task.setDueDate(due);
                task.setNotes(notes);

                TaskService.saveTask(task);
                count++;
            } catch (Exception e) {
                logger.error("Failed to import task row: {}", (Object) row, e);
            }
        }
        return count;
    }

    private static String getMappedValue(String[] row, Map<String, Integer> fieldMapping, String fieldKey) {
        Integer index = fieldMapping.get(fieldKey);
        if (index == null || index < 0 || index >= row.length) {
            return null;
        }
        return row[index];
    }

    private static LocalDate parseLocalDate(String dateStr, DateTimeFormatter[] formatters) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException e) {
                // Try next
            }
        }
        return LocalDate.now(); // Fallback
    }

    public static void exportTasks(File file, List<Task> tasks, String format) throws IOException {
        String fmt = format.toLowerCase();
        if (fmt.equals("excel") || file.getName().endsWith(".xlsx") || file.getName().endsWith(".xls")) {
            ExcelHelper.writeExcel(file, tasks);
        } else if (fmt.equals("csv") || file.getName().endsWith(".csv")) {
            CsvHelper.writeCsv(file, tasks);
        } else if (fmt.equals("pdf") || file.getName().endsWith(".pdf")) {
            PdfGenerator.generateTasksReport(file, "Work Track Tasks Report", tasks);
        } else {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }
}
