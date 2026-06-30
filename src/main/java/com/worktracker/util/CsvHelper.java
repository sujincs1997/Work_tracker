package com.worktracker.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.worktracker.model.Task;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CsvHelper {

    public static List<String[]> readCsv(File file) throws IOException, CsvException {
        try (Reader reader = new FileReader(file);
             CSVReader csvReader = new CSVReader(reader)) {
            return csvReader.readAll();
        }
    }

    public static void writeCsv(File file, List<Task> tasks) throws IOException {
        try (Writer writer = new FileWriter(file);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            String[] headers = {
                    "Task ID", "Task Name", "Project", "Client", "Work Order", 
                    "Category", "Priority", "Status", "Completion %", 
                    "Assigned Date", "Due Date", "Notes"
            };
            csvWriter.writeNext(headers);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Task task : tasks) {
                String[] row = {
                        task.getTaskId(),
                        task.getTaskName(),
                        task.getProject(),
                        task.getClient(),
                        task.getWorkOrder() != null ? task.getWorkOrder() : "",
                        task.getWorkCategory(),
                        task.getPriority(),
                        task.getStatus(),
                        String.valueOf(task.getCompletionPercentage()),
                        task.getAssignedDate() != null ? task.getAssignedDate().format(formatter) : "",
                        task.getDueDate() != null ? task.getDueDate().format(formatter) : "",
                        task.getNotes() != null ? task.getNotes() : ""
                };
                csvWriter.writeNext(row);
            }
        }
    }
}
