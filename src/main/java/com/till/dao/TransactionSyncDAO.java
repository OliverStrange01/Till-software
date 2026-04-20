package com.till.dao;

import com.till.config.AppConfig;
import com.till.database.DatabaseConnection;
import com.till.model.QueuedTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionSyncDAO {
    private static final Logger LOGGER = Logger.getLogger(TransactionSyncDAO.class.getName());

    public void enqueue(String payloadJson) {
        String sql = """
            INSERT INTO transaction_sync_queue (payload_json, attempts, status, last_error, next_attempt_at)
            VALUES (?, 0, 'PENDING', NULL, CURRENT_TIMESTAMP)
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, payloadJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (isMissingNextAttemptColumn(e)) {
                enqueueLegacy(payloadJson);
                return;
            }
            LOGGER.log(Level.WARNING, "Failed to enqueue transaction payload", e);
        }
    }

    public List<QueuedTransaction> getPending(int limit) {
        String sql = """
            SELECT id, payload_json, attempts, status, last_error, created_at, next_attempt_at
            FROM transaction_sync_queue
            WHERE status = 'PENDING'
              AND datetime(next_attempt_at) <= datetime('now')
            ORDER BY created_at ASC
            LIMIT ?
        """;
        List<QueuedTransaction> queued = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    queued.add(new QueuedTransaction(
                            rs.getLong("id"),
                            rs.getString("payload_json"),
                            rs.getInt("attempts"),
                            rs.getString("status"),
                            rs.getString("last_error"),
                            rs.getString("created_at"),
                            rs.getString("next_attempt_at")
                    ));
                }
            }
        } catch (SQLException e) {
            if (isMissingNextAttemptColumn(e)) {
                return getPendingLegacy(limit);
            }
            LOGGER.log(Level.WARNING, "Failed to fetch pending queued transactions", e);
        }
        return queued;
    }

    public void markSynced(long id) {
        String sql = "UPDATE transaction_sync_queue SET status='SYNCED', updated_at=CURRENT_TIMESTAMP WHERE id = ?";
        updateSimple(sql, id);
    }

    public void markFailed(long id, String error) {
        int maxAttempts = AppConfig.transactionSyncMaxAttempts();
        int baseDelaySeconds = AppConfig.transactionSyncBaseDelaySeconds();
        String sql = """
            UPDATE transaction_sync_queue
            SET attempts = attempts + 1,
                status = CASE WHEN attempts + 1 >= ? THEN 'DEAD' ELSE 'PENDING' END,
                last_error = ?,
                next_attempt_at = CASE
                    WHEN attempts + 1 >= ? THEN next_attempt_at
                    ELSE datetime('now', '+' || (? * (1 << MIN(attempts, 6))) || ' seconds')
                END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maxAttempts);
            ps.setString(2, error);
            ps.setInt(3, maxAttempts);
            ps.setInt(4, baseDelaySeconds);
            ps.setLong(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (isMissingNextAttemptColumn(e)) {
                markFailedLegacy(id, error);
                return;
            }
            LOGGER.log(Level.WARNING, "Failed to mark queued transaction failed", e);
        }
    }

    public List<QueuedTransaction> getRecent(int limit) {
        String sql = """
            SELECT id, payload_json, attempts, status, last_error, created_at, next_attempt_at
            FROM transaction_sync_queue
            ORDER BY id DESC
            LIMIT ?
        """;
        List<QueuedTransaction> queued = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    queued.add(new QueuedTransaction(
                            rs.getLong("id"),
                            rs.getString("payload_json"),
                            rs.getInt("attempts"),
                            rs.getString("status"),
                            rs.getString("last_error"),
                            rs.getString("created_at"),
                            rs.getString("next_attempt_at")
                    ));
                }
            }
        } catch (SQLException e) {
            if (isMissingNextAttemptColumn(e)) {
                return getRecentLegacy(limit);
            }
            LOGGER.log(Level.WARNING, "Failed to fetch recent queued transactions", e);
        }
        return queued;
    }

    private void enqueueLegacy(String payloadJson) {
        String sql = """
            INSERT INTO transaction_sync_queue (payload_json, attempts, status, last_error)
            VALUES (?, 0, 'PENDING', NULL)
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, payloadJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to enqueue transaction payload (legacy)", e);
        }
    }

    private List<QueuedTransaction> getPendingLegacy(int limit) {
        String sql = """
            SELECT id, payload_json, attempts, status, last_error, created_at, NULL as next_attempt_at
            FROM transaction_sync_queue
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT ?
        """;
        return queryQueued(sql, limit);
    }

    private List<QueuedTransaction> getRecentLegacy(int limit) {
        String sql = """
            SELECT id, payload_json, attempts, status, last_error, created_at, NULL as next_attempt_at
            FROM transaction_sync_queue
            ORDER BY id DESC
            LIMIT ?
        """;
        return queryQueued(sql, limit);
    }

    private void markFailedLegacy(long id, String error) {
        int maxAttempts = AppConfig.transactionSyncMaxAttempts();
        String sql = """
            UPDATE transaction_sync_queue
            SET attempts = attempts + 1,
                status = CASE WHEN attempts + 1 >= ? THEN 'DEAD' ELSE 'PENDING' END,
                last_error = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maxAttempts);
            ps.setString(2, error);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to mark queued transaction failed (legacy)", e);
        }
    }

    private List<QueuedTransaction> queryQueued(String sql, int limit) {
        List<QueuedTransaction> queued = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    queued.add(new QueuedTransaction(
                            rs.getLong("id"),
                            rs.getString("payload_json"),
                            rs.getInt("attempts"),
                            rs.getString("status"),
                            rs.getString("last_error"),
                            rs.getString("created_at"),
                            rs.getString("next_attempt_at")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to query queued transactions", e);
        }
        return queued;
    }

    private boolean isMissingNextAttemptColumn(SQLException e) {
        return e.getMessage() != null
                && (e.getMessage().contains("no column named next_attempt_at")
                || e.getMessage().contains("no such column: next_attempt_at"));
    }

    private void updateSimple(String sql, long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to update queued transaction state", e);
        }
    }
}
