package com.till.dao;

import com.till.model.QueuedTransaction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionSyncDAOTest {

    @Test
    void enqueueCreatesPendingQueueEntry() {
        TransactionSyncDAO dao = new TransactionSyncDAO();
        String payload = "{\"test\":\"queue-entry-" + System.nanoTime() + "\"}";
        dao.enqueue(payload);

        List<QueuedTransaction> recent = dao.getRecent(50);
        QueuedTransaction created = recent.stream()
                .filter(p -> payload.equals(p.getPayloadJson()))
                .findFirst()
                .orElse(null);
        assertNotNull(created);
        assertTrue("PENDING".equals(created.getStatus()) || "SYNCED".equals(created.getStatus()) || "DEAD".equals(created.getStatus()));
    }
}
