package com.worktracker;

import com.worktracker.controller.MainController;
import com.worktracker.service.DatabaseService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private MainController mainController;

    @Override
    public void start(Stage stage) {
        try {
            logger.info("Starting Work Tracker Desktop Application...");

            // 1. Initialize SQLite Database Schema & Seed Defaults
            DatabaseService.initializeDatabase();

            // 2. Load Main FXML Shell Layout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            mainController = loader.getController();

            // 3. Setup Scene
            Scene scene = new Scene(root);
            
            // Apply current theme stylesheet
            mainController.applyCurrentTheme();

            stage.setScene(scene);
            stage.setTitle("WorkTracker - Desktop Workspace");
            stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/fxml/main.fxml"))); // fallback fallback or icon
            
            // Clean shutdown listener
            stage.setOnCloseRequest(event -> {
                logger.info("Application close requested. Shutting down...");
                if (mainController != null) {
                    mainController.onShutdown();
                }
            });

            stage.show();
            logger.info("Application window displayed successfully.");
        } catch (Exception e) {
            logger.error("Critical error starting application", e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
