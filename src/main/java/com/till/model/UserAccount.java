package com.till.model;

public class UserAccount {
    private final String username;
    private final String role;
    private final String passwordHash;
    private final String passwordSalt;
    private final boolean active;

    public UserAccount(String username, String role, String passwordHash, String passwordSalt, boolean active) {
        this.username = username;
        this.role = role;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.active = active;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public boolean isActive() {
        return active;
    }
}
