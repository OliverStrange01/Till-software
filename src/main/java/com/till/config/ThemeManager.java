package com.till.config;

import javafx.scene.Scene;
import java.util.HashSet;
import java.util.Set;

public class ThemeManager {

    // ONE shared instance for the whole app (like a "static" class in C#)
    private static final ThemeManager INSTANCE = new ThemeManager();

    // List of all the screens in the app, so we can update them all at once
    private final Set<Scene> scenes = new HashSet<>();

    // Is dark mode currently on?
    private boolean darkMode = false;

    // The path to our dark mode CSS file
    private final String darkCss = getClass()
            .getResource("/css/dark-theme.css")
            .toExternalForm();

    // Private constructor - no one else can make a new ThemeManager
    private ThemeManager() {}

    // How other code gets the one shared instance
    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    // Call this whenever a new screen (Scene) is created
    public void register(Scene scene) {
        scenes.add(scene);
        applyTheme(scene);
    }

    // Returns true if dark mode is on
    public boolean isDarkMode() {
        return darkMode;
    }

    // Flip dark mode on or off, and update every screen
    public void setDarkMode(boolean on) {
        this.darkMode = on;
        for (Scene s : scenes) {
            applyTheme(s);
        }
    }

    // Internal helper: add or remove the CSS file from a screen
    private void applyTheme(Scene scene) {
        scene.getStylesheets().remove(darkCss);
        if (darkMode) {
            scene.getStylesheets().add(darkCss);
        }
    }
}