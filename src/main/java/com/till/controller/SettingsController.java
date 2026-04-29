package com.till.controller;

import com.till.config.SettingsManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private ToggleButton darkModeToggle;
    @FXML private Label        fontSizeLabel;
    @FXML private Label        statusLabel;

    private final SettingsManager settings = SettingsManager.getInstance();

    private static final int[] SIZES = SettingsManager.FONT_SIZES;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        boolean dark = settings.isDarkMode();
        darkModeToggle.setSelected(dark);
        darkModeToggle.setText(dark ? "On" : "Off");
        updateDarkModeStyle(dark);
        fontSizeLabel.setText(settings.getFontSize() + " px");
    }

    @FXML
    private void handleDarkModeToggle() {
        boolean on = darkModeToggle.isSelected();
        darkModeToggle.setText(on ? "On" : "Off");
        updateDarkModeStyle(on);
        settings.setDarkMode(on);
        flash("Dark mode " + (on ? "enabled" : "disabled"));
    }

    @FXML
    private void increaseFontSize() {
        int current = settings.getFontSize();
        for (int size : SIZES) {
            if (size > current) {
                applyFontSize(size);
                return;
            }
        }
        flash("Already at maximum text size");
    }

    @FXML
    private void decreaseFontSize() {
        int current = settings.getFontSize();
        int prev = -1;
        for (int size : SIZES) {
            if (size < current) prev = size;
        }
        if (prev != -1) {
            applyFontSize(prev);
        } else {
            flash("Already at minimum text size");
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) fontSizeLabel.getScene().getWindow();
        stage.close();
    }

    private void applyFontSize(int px) {
        settings.setFontSize(px);
        fontSizeLabel.setText(px + " px");
        flash("Text size set to " + px + "px");
    }

    private void updateDarkModeStyle(boolean on) {
        if (on) {
            darkModeToggle.setStyle(
                    "-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            darkModeToggle.setStyle(
                    "-fx-background-color: #bdbdbd; -fx-text-fill: #333; -fx-font-weight: bold;");
        }
    }

    private void flash(String msg) {
        statusLabel.setText("✓ " + msg);
    }
}
