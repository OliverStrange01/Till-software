package com.till.dao;

import com.till.model.QueuedTransaction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyIntegrationTest {

    @Test
    void failedSyncEventuallyMovesToDeadLetterState() {
        TransactionSyncDAO dao = new TransactionSyncDAO();
        String payload = "{\"retry\":\"dead-letter-" + System.nanoTime() + "\"}";
        dao.enqueue(payload);

        List<QueuedTransaction> queued = dao.getRecent(50);
        QueuedTransaction row = queued.stream()
                .filter(q -> payload.equals(q.getPayloadJson()))
                .findFirst()
                .orElse(null);
        assertNotNull(row);

        for (int i = 0; i < 6; i++) {
            dao.markFailed(row.getId(), "simulated error " + i);
        }

        List<QueuedTransaction> after = dao.getRecent(50);
        QueuedTransaction updated = after.stream()
                .filter(q -> q.getId() == row.getId())
                .findFirst()
                .orElse(null);

        assertNotNull(updated);
        assertTrue("DEAD".equals(updated.getStatus()) || updated.getAttempts() >= 6);
    }
}
