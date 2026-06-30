package com.worktracker.util;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.worktracker.model.Task;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfGenerator {

    public static void generateTasksReport(File file, String title, List<Task> tasks) throws IOException {
        Document document = new Document(PageSize.A4.rotate()); // Landscape is better for table width
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Set styles
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(59, 130, 246));
            Font subTitleFont = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

            // Document Header
            Paragraph titlePara = new Paragraph(title, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            document.add(titlePara);

            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Paragraph subtitle = new Paragraph("Generated on " + timeStamp, subTitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Table setup
            PdfPTable table = new PdfPTable(9); // columns
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 2.5f, 1.8f, 1.8f, 1.5f, 1.0f, 1.0f, 1.0f, 1.2f});

            // Table Headers
            String[] headers = {"Task ID", "Task Name", "Project", "Client", "Category", "Priority", "Status", "Comp %", "Assigned"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Paragraph(header, headerFont));
                headerCell.setBackgroundColor(new Color(59, 130, 246));
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setPadding(6);
                table.addCell(headerCell);
            }

            // Table Rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            boolean alternate = false;
            Color altColor = new Color(243, 244, 246);

            for (Task task : tasks) {
                String[] values = {
                        task.getTaskId(),
                        task.getTaskName(),
                        task.getProject(),
                        task.getClient(),
                        task.getWorkCategory(),
                        task.getPriority(),
                        task.getStatus(),
                        String.format("%.1f%%", task.getCompletionPercentage()),
                        task.getAssignedDate() != null ? task.getAssignedDate().format(formatter) : ""
                };

                for (int i = 0; i < values.length; i++) {
                    PdfPCell cell = new PdfPCell(new Paragraph(values[i], cellFont));
                    cell.setPadding(5);
                    if (alternate) {
                        cell.setBackgroundColor(altColor);
                    }
                    if (i == 0 || i >= 5) {
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    }
                    table.addCell(cell);
                }
                alternate = !alternate;
            }

            document.add(table);
        } catch (DocumentException e) {
            throw new IOException("Error generating PDF document", e);
        } finally {
            document.close();
        }
    }
}
