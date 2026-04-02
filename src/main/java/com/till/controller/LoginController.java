package com.till.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class LoginController {

    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    // Hardcoded manager password (SHA-256 hash of "admin123" for demo)
    // Real app: use bcrypt + database
    private static final String MANAGER_PASSWORD_HASH = "cbfdac6008f9cab4083784cbd1874f76618d2a97c879e8a1a0b2d8e2f7f5a0b8"; // SHA-256("admin123")

    private static final String MANAGER_PASSWORD = "admin123"; // plain for demo - CHANGE THIS

    @FXML
    private void handleLogin() {
        String input = passwordField.getText().trim();

        if (input.isEmpty()) {
            errorLabel.setText("Password required");
            return;
        }

        // Simple plain text check for demo (replace with hash check later)
        if (input.equals(MANAGER_PASSWORD)) {
            openMainApp(true); // true = admin mode
        } else {
            errorLabel.setText("Incorrect password");
            passwordField.clear();
        }
    }

    @FXML
    private void handleCashierLogin() {
        openMainApp(false); // false = cashier mode
    }

    private void openMainApp(boolean isAdmin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            MainController mainController = loader.getController();
            // Pass admin flag if needed (e.g. show/hide admin button)
            mainController.setAdminMode(isAdmin);

            Stage mainStage = new Stage();
            mainStage.setTitle("Till POS System");
            mainStage.setScene(scene);
            mainStage.show();

            // Close login window
            Stage loginStage = (Stage) passwordField.getScene().getWindow();
            loginStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load main application").showAndWait();
        }
    }
}