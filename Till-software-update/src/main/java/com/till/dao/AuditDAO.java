package com.till.dao;

import com.till.database.DatabaseConnection;
import com.till.model.AuditEventRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuditDAO {
    private static final Logger LOGGER = Logger.getLogger(AuditDAO.class.getName());

    public void logEvent(String eventType, String details, String actor) {
        String sql = """
            INSERT INTO audit_events (event_type, details, actor)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventType);
            ps.setString(2, details);
            ps.setString(3, actor);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to log audit event", e);
        }
    }

    public List<AuditEventRecord> getRecentEvents(int limit) {
        String sql = """
            SELECT id, event_type, details, actor, created_at
            FROM audit_events
            ORDER BY id DESC
            LIMIT ?
        """;
        List<AuditEventRecord> events = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(new AuditEventRecord(
                            rs.getLong("id"),
                            rs.getString("event_type"),
                            rs.getString("details"),
                            rs.getString("actor"),
                            rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to read audit events", e);
        }
        return events;
    }
}
