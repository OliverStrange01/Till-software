package com.till.config;

public final class AppConfig {
    private AppConfig() {
    }

    public static String managerPasswordHash() {
        return read("TILL_MANAGER_PASSWORD_HASH",
                "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9");
    }

    public static String transactionApiUrl() {
        return read("TILL_TX_API_URL", "");
    }

    public static String transactionApiKey() {
        return read("TILL_TX_API_KEY", "");
    }

    public static int transactionSyncBatchSize() {
        String raw = read("TILL_TX_SYNC_BATCH_SIZE", "10");
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return 10;
        }
    }

    public static int transactionSyncMaxAttempts() {
        String raw = read("TILL_TX_SYNC_MAX_ATTEMPTS", "6");
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return 6;
        }
    }

    public static int transactionSyncBaseDelaySeconds() {
        String raw = read("TILL_TX_SYNC_BASE_DELAY_SECONDS", "30");
        try {
            return Math.max(5, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return 30;
        }
    }

    public static boolean seedSampleData() {
        return Boolean.parseBoolean(read("TILL_SEED_SAMPLE_DATA", "false"));
    }

    private static String read(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null ? defaultValue : value.trim();
    }
}
