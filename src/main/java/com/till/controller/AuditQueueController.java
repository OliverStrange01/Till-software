package com.till.controller;

import com.till.dao.AuditDAO;
import com.till.dao.TransactionSyncDAO;
import com.till.model.AuditEventRecord;
import com.till.model.QueuedTransaction;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

public class AuditQueueController {
    @FXML private TableView<AuditEventRecord> auditTable;
    @FXML private TableColumn<AuditEventRecord, String> auditTimeCol;
    @FXML private TableColumn<AuditEventRecord, String> auditTypeCol;
    @FXML private TableColumn<AuditEventRecord, String> auditActorCol;
    @FXML private TableColumn<AuditEventRecord, String> auditDetailsCol;

    @FXML private TableView<QueuedTransaction> queueTable;
    @FXML private TableColumn<QueuedTransaction, String> queueStatusCol;
    @FXML private TableColumn<QueuedTransaction, String> queueAttemptsCol;
    @FXML private TableColumn<QueuedTransaction, String> queueNextCol;
    @FXML private TableColumn<QueuedTransaction, String> queueErrorCol;

    @FXML private Label statusLabel;

    private final AuditDAO auditDAO = new AuditDAO();
    private final TransactionSyncDAO transactionSyncDAO = new TransactionSyncDAO();

    @FXML
    private void initialize() {
        auditTimeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCreatedAt()));
        auditTypeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEventType()));
        auditActorCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getActor()));
        auditDetailsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDetails()));

        queueStatusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        queueAttemptsCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getAttempts())));
        queueNextCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getNextAttemptAt())));
        queueErrorCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getLastError())));

        refresh();
    }

    @FXML
    private void refresh() {
        auditTable.setItems(FXCollections.observableArrayList(auditDAO.getRecentEvents(200)));
        queueTable.setItems(FXCollections.observableArrayList(transactionSyncDAO.getRecent(200)));
        statusLabel.setText("Loaded latest audit and queue records");
    }
}
