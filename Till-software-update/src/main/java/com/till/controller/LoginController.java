package com.till.controller;

import com.till.dao.AuthDAO;
import com.till.model.UserAccount;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private final AuthDAO authDAO = new AuthDAO();

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        authDAO.ensureDefaultUsers();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String input = passwordField.getText().trim();

        if (username.isEmpty() || input.isEmpty()) {
            errorLabel.setText("Username and password are required");
            return;
        }

        UserAccount account = authDAO.findByUsername(username);
        if (account == null || !authDAO.verifyCredentials(username, input, null)) {
            errorLabel.setText("Incorrect password");
            passwordField.clear();
            return;
        }
        boolean isAdmin = "MANAGER".equalsIgnoreCase(account.getRole());
        openMainApp(isAdmin);
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
            LOGGER.log(Level.SEVERE, "Failed to load main application", e);
            new Alert(Alert.AlertType.ERROR, "Failed to load main application").showAndWait();
        }
    }

}