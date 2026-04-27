package com.till.config;

import javafx.scene.Scene;
import java.util.HashSet;
import java.util.Set;

public class FontSizeManager {

    // ONE shared instance for the whole app
    private static final FontSizeManager INSTANCE = new FontSizeManager();

    // List of all the screens, so we can update them all at once
    private final Set<Scene> scenes = new HashSet<>();

    // Is large font mode currently on?
    private boolean largeFont = false;

    // The path to our large-font CSS file
    private final String largeCss = getClass()
            .getResource("/css/large-font.css")
            .toExternalForm();

    private FontSizeManager() {}

    public static FontSizeManager getInstance() {
        return INSTANCE;
    }

    // Call this whenever a new screen (Scene) is created
    public void register(Scene scene) {
        scenes.add(scene);
        applyFontSize(scene);
    }

    // Returns true if large font is on
    public boolean isLargeFont() {
        return largeFont;
    }

    // Flip large font on or off, and update every screen
    public void setLargeFont(boolean on) {
        this.largeFont = on;
        for (Scene s : scenes) {
            applyFontSize(s);
        }
    }

    private void applyFontSize(Scene scene) {
        scene.getStylesheets().remove(largeCss);
        if (largeFont) {
            scene.getStylesheets().add(largeCss);
        }
    }
}