package com.worktracker.controller;

import com.worktracker.model.WorkCategory;
import com.worktracker.service.TaskService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoryController {
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @FXML private TextField txtCategoryName;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    @FXML private TableView<WorkCategory> tableCategories;
    @FXML private TableColumn<WorkCategory, Long> colCatId;
    @FXML private TableColumn<WorkCategory, String> colCatName;
    @FXML private TableColumn<WorkCategory, String> colCatType;

    private WorkCategory selectedCategory;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadCategories();

        tableCategories.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedCategory = newVal;
                txtCategoryName.setText(newVal.getName());
                btnSave.setText("💾 Update Category");
            }
        });
    }

    private void setupTableColumns() {
        colCatId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCatName.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        colCatType.setCellValueFactory(cellData -> {
            boolean isDefault = cellData.getValue().isDefault();
            return new SimpleStringProperty(isDefault ? "Default System" : "Custom User");
        });
    }

    private void loadCategories() {
        tableCategories.setItems(FXCollections.observableArrayList(TaskService.getCategories()));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String name = txtCategoryName.getText().trim();
        if (name.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Category name cannot be empty.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        WorkCategory category = selectedCategory;
        if (category == null) {
            category = new WorkCategory();
            category.setDefault(false); // custom category
        } else if (category.isDefault()) {
            // Cannot modify name of system defaults
            Alert alert = new Alert(Alert.AlertType.ERROR, "Default system categories cannot be modified.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        category.setName(name);
        try {
            TaskService.saveCategory(category);
            txtCategoryName.clear();
            selectedCategory = null;
            btnSave.setText("💾 Save Category");
            loadCategories();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Category saved successfully!", ButtonType.OK);
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save category. Name might already exist.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        txtCategoryName.clear();
        selectedCategory = null;
        btnSave.setText("💾 Save Category");
        tableCategories.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        WorkCategory category = tableCategories.getSelectionModel().getSelectedItem();
        if (category != null) {
            if (category.isDefault()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Default system categories cannot be deleted.", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete category: " + category.getName() + "?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    TaskService.deleteCategory(category.getId());
                    loadCategories();
                    handleClear(null);
                }
            });
        }
    }
}
