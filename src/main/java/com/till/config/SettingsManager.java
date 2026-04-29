package com.till.config;

import com.till.database.DatabaseConnection;
import javafx.scene.Scene;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton that persists UI preferences (dark mode, font size) in the
 * app_settings table and applies them to every registered JavaFX Scene.
 */
public final class SettingsManager {

    private static final Logger LOGGER = Logger.getLogger(SettingsManager.class.getName());

    // ── Singleton ────────────────────────────────────────────────────────────
    private static final SettingsManager INSTANCE = new SettingsManager();
    public static SettingsManager getInstance() { return INSTANCE; }
    private SettingsManager() { ensureTable(); load(); }

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean darkMode  = false;
    private int     fontSize  = 14;   // px — applied as -fx-font-size on root

    // Valid font-size choices shown in the UI
    public static final int[] FONT_SIZES = { 12, 14, 16, 18, 20 };

    // Every open scene that should react to settings changes
    private final List<Scene> registeredScenes = new ArrayList<>();

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isDarkMode()  { return darkMode; }
    public int     getFontSize() { return fontSize; }

    public void setDarkMode(boolean value) {
        darkMode = value;
        save();
        applyToAll();
    }

    public void setFontSize(int px) {
        fontSize = px;
        save();
        applyToAll();
    }

    /** Call once when a new Stage/Scene is created so it picks up current settings. */
    public void register(Scene scene) {
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
        }
        apply(scene);
    }

    /** Remove a scene when its window closes (prevents memory leak). */
    public void unregister(Scene scene) {
        registeredScenes.remove(scene);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void ensureTable() {
        String ddl = """
            CREATE TABLE IF NOT EXISTS app_settings (
                key   TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not create app_settings table", e);
        }
    }

    private void load() {
        String sql = "SELECT key, value FROM app_settings";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String key = rs.getString("key");
                String val = rs.getString("value");
                switch (key) {
                    case "dark_mode"  -> darkMode  = Boolean.parseBoolean(val);
                    case "font_size"  -> {
                        try { fontSize = Integer.parseInt(val); }
                        catch (NumberFormatException ignored) { fontSize = 14; }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not load settings", e);
        }
    }

    private void save() {
        String sql = "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "dark_mode");
            ps.setString(2, String.valueOf(darkMode));
            ps.executeUpdate();

            ps.setString(1, "font_size");
            ps.setString(2, String.valueOf(fontSize));
            ps.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not save settings", e);
        }
    }

    private void applyToAll() {
        for (Scene scene : registeredScenes) {
            apply(scene);
        }
    }

    private void apply(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;

        // Font size — applied inline so it cascades to all children
        scene.getRoot().setStyle("-fx-font-size: " + fontSize + "px;");

        // Dark mode — toggle a stylesheet on the scene
        String darkCss = getClass().getResource("/dark-mode.css") != null
                ? getClass().getResource("/dark-mode.css").toExternalForm()
                : null;

        if (darkCss != null) {
            if (darkMode) {
                if (!scene.getStylesheets().contains(darkCss)) {
                    scene.getStylesheets().add(darkCss);
                }
            } else {
                scene.getStylesheets().remove(darkCss);
            }
        }
    }
}
