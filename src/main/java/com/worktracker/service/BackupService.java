package com.worktracker.service;

import com.worktracker.model.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

public class BackupService {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static final int MAX_BACKUPS = 10;

    public static void performBackup() {
        AppSettings settings = DatabaseService.getSettings();
        if (settings == null || !settings.isAutoBackup()) {
            return;
        }

        String dbPath = settings.getDatabaseLocation();
        String backupFolder = settings.getBackupFolder();

        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            logger.warn("Database file {} does not exist. Cannot perform backup.", dbPath);
            return;
        }

        File backupDir = new File(backupFolder);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(dtf);
        String backupFileName = "worktracker_backup_" + timestamp + ".db";
        File backupFile = new File(backupDir, backupFileName);

        try {
            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Database backup created successfully: {}", backupFile.getAbsolutePath());
            cleanupOldBackups(backupDir);
        } catch (IOException e) {
            logger.error("Failed to back up database", e);
        }
    }

    private static void cleanupOldBackups(File backupDir) {
        File[] files = backupDir.listFiles((dir, name) -> name.startsWith("worktracker_backup_") && name.endsWith(".db"));
        if (files == null || files.length <= MAX_BACKUPS) {
            return;
        }

        // Sort files by last modified time (oldest first)
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        int filesToDelete = files.length - MAX_BACKUPS;
        for (int i = 0; i < filesToDelete; i++) {
            if (files[i].delete()) {
                logger.info("Deleted old backup file: {}", files[i].getName());
            } else {
                logger.warn("Failed to delete old backup file: {}", files[i].getName());
            }
        }
    }
}
