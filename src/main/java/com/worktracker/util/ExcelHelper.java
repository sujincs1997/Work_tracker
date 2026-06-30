package com.worktracker.util;

import com.worktracker.model.Task;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExcelHelper {

    public static List<String[]> readExcel(File file) throws IOException {
        List<String[]> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                int lastCellNum = row.getLastCellNum();
                if (lastCellNum <= 0) continue;
                
                String[] rowData = new String[lastCellNum];
                boolean hasData = false;
                for (int i = 0; i < lastCellNum; i++) {
                    Cell cell = row.getCell(i);
                    rowData[i] = formatter.formatCellValue(cell);
                    if (rowData[i] != null && !rowData[i].trim().isEmpty()) {
                        hasData = true;
                    }
                }
                if (hasData) {
                    data.add(rowData);
                }
            }
        }
        return data;
    }

    public static void writeExcel(File file, List<Task> tasks) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tasks");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Task ID", "Task Name", "Project", "Client", "Work Order", 
                    "Category", "Priority", "Status", "Completion %", 
                    "Assigned Date", "Due Date", "Notes"
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Task task : tasks) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(task.getTaskId());
                row.createCell(1).setCellValue(task.getTaskName());
                row.createCell(2).setCellValue(task.getProject());
                row.createCell(3).setCellValue(task.getClient());
                row.createCell(4).setCellValue(task.getWorkOrder() != null ? task.getWorkOrder() : "");
                row.createCell(5).setCellValue(task.getWorkCategory());
                row.createCell(6).setCellValue(task.getPriority());
                row.createCell(7).setCellValue(task.getStatus());
                row.createCell(8).setCellValue(task.getCompletionPercentage());
                row.createCell(9).setCellValue(task.getAssignedDate() != null ? task.getAssignedDate().format(formatter) : "");
                row.createCell(10).setCellValue(task.getDueDate() != null ? task.getDueDate().format(formatter) : "");
                row.createCell(11).setCellValue(task.getNotes() != null ? task.getNotes() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }
}
