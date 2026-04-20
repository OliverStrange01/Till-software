package com.till.dao;

import com.till.database.DatabaseConnection;
import com.till.model.UserAccount;
import com.till.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthDAO {
    private static final Logger LOGGER = Logger.getLogger(AuthDAO.class.getName());

    public UserAccount findByUsername(String username) {
        String sql = """
            SELECT username, role, password_hash, password_salt, active
            FROM users
            WHERE LOWER(username) = LOWER(?)
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserAccount(
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getString("password_hash"),
                            rs.getString("password_salt"),
                            rs.getInt("active") == 1
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to find user by username", e);
        }
        return null;
    }

    public boolean verifyCredentials(String username, String password, String requiredRole) {
        UserAccount account = findByUsername(username);
        if (account == null || !account.isActive()) {
            return false;
        }
        if (requiredRole != null && !requiredRole.equalsIgnoreCase(account.getRole())) {
            return false;
        }
        return PasswordHasher.verify(password, account.getPasswordHash(), account.getPasswordSalt());
    }

    public void ensureDefaultUsers() {
        ensureUser("manager", "MANAGER", "admin123");
        ensureUser("cashier", "CASHIER", "cashier123");
    }

    private void ensureUser(String username, String role, String defaultPassword) {
        if (findByUsername(username) != null) {
            return;
        }

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(defaultPassword, salt);
        String sql = """
            INSERT INTO users (username, role, password_hash, password_salt, active)
            VALUES (?, ?, ?, ?, 1)
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase(Locale.UK));
            ps.setString(2, role.toUpperCase(Locale.UK));
            ps.setString(3, hash);
            ps.setString(4, salt);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to ensure default user", e);
        }
    }
}
