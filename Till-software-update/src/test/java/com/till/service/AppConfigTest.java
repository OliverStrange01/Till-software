package com.till.service;

import com.till.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AppConfigTest {

    @Test
    void managerPasswordHashHasExpectedLength() {
        assertEquals(64, AppConfig.managerPasswordHash().length());
    }

    @Test
    void defaultTransactionBatchSizeIsPositive() {
        assertFalse(AppConfig.transactionSyncBatchSize() <= 0);
    }
}
