package com.till.controller;

import com.till.dao.SalesDAO;
import com.till.model.SalesRecord;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AnalyticsController implements Initializable {

    @FXML private Label totalRevenueLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label topProductLabel;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private PieChart categoryPieChart;
    @FXML private TableView<SalesRecord> salesTable;
    @FXML private TableColumn<SalesRecord, String> productNameCol;
    @FXML private TableColumn<SalesRecord, Integer> qtySoldCol;
    @FXML private TableColumn<SalesRecord, Double> revenueCol;

    private final SalesDAO salesDAO = new SalesDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadData();
    }

    private void setupTable() {
        productNameCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getProductName()));
        qtySoldCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getQuantitySold()));
        revenueCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getRevenue()));
        revenueCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("£%.2f", v));
            }
        });
    }

    private void loadData() {
        List<SalesRecord> records = salesDAO.getTodayBreakdown();
        double totalRevenue      = salesDAO.getTodayTotal();
        int totalTransactions    = salesDAO.getTodayTransactionCount();

        // ── Summary labels ────────────────────────────────────────────────────
        totalRevenueLabel.setText(String.format("£%.2f", totalRevenue));
        totalTransactionsLabel.setText(String.valueOf(totalTransactions));
        topProductLabel.setText(records.isEmpty() ? "N/A" : records.get(0).getProductName());

        // ── Table ─────────────────────────────────────────────────────────────
        salesTable.getItems().setAll(records);

        // ── Bar chart: revenue per product ────────────────────────────────────
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue Today");
        for (SalesRecord r : records) {
            series.getData().add(new XYChart.Data<>(r.getProductName(),
                    Math.round(r.getRevenue() * 100.0) / 100.0));
        }
        revenueChart.getData().add(series);

        // ── Pie chart: revenue by category ────────────────────────────────────
        categoryPieChart.getData().clear();
        records.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> "Unknown",
                        java.util.stream.Collectors.summingDouble(SalesRecord::getRevenue)
                ))
                .forEach((cat, rev) ->
                        categoryPieChart.getData().add(
                                new PieChart.Data(cat, rev)));

        if (records.isEmpty()) {
            Label noData = new Label("No sales recorded today yet.");
            noData.setStyle("-fx-font-size: 14; -fx-text-fill: #757575;");
            revenueChart.setVisible(false);
            categoryPieChart.setVisible(false);
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}