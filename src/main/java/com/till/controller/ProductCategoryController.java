package com.till.controller;

import com.till.dao.ProductDAO;
import com.till.model.Product;
import com.till.service.CartService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProductCategoryController {

    @FXML private HBox categoryBar;
    @FXML private Label categoryTitle;
    @FXML private FlowPane productFlowPane;
    @FXML private Button refreshButton;

    private CartService cartService;
    private final ProductDAO productDAO = new ProductDAO();

    private Timeline autoRefreshTimer;
    private String currentCategory = "All";

    public void setCartService(CartService service) {
        this.cartService = service;
        initializeCategories();
        loadProducts();
        startAutoRefresh();           // ← start periodic refresh
    }

    // Called when the controller is initialized or when you want to force refresh
    private void initializeCategories() {
        categoryBar.getChildren().clear();

        // Get real categories from DB
        List<String> dbCategories = productDAO.getAllCategories();

        // Build display list: "All" first + sorted real categories
        List<String> displayCategories = new ArrayList<>();
        displayCategories.add("All");
        displayCategories.addAll(dbCategories);

        // Create buttons
        for (String cat : displayCategories) {
            Button catBtn = new Button(cat);
            catBtn.setPrefSize(100, 40);
            catBtn.setStyle("-fx-font-size: 14; -fx-background-color: #2196F3; -fx-text-fill: white;");

            // Highlight active category
            if (cat.equals(currentCategory)) {
                catBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
            }

            catBtn.setOnAction(e -> {
                currentCategory = cat;
                categoryTitle.setText(cat.equals("All") ? "All Products" : cat + " Products");
                loadProducts();

                // Reset styles & highlight selected
                categoryBar.getChildren().forEach(node -> {
                    if (node instanceof Button b) {
                        b.setStyle("-fx-font-size: 14; -fx-background-color: #2196F3; -fx-text-fill: white;");
                    }
                });
                catBtn.setStyle("-fx-font-size: 14; -fx-background-color: #1976D2; -fx-text-fill: white;");
            });

            categoryBar.getChildren().add(catBtn);
        }
    }

    private void loadProducts() {
        productFlowPane.getChildren().clear();

        List<Product> allProducts = productDAO.getAllProducts();

        List<Product> filtered = "All".equals(currentCategory)
                ? allProducts
                : allProducts.stream()
                .filter(p -> currentCategory.equals(p.getCategory()))
                .collect(Collectors.toList());

        // Sort by name
        filtered.sort(Comparator.comparing(Product::getName));

        for (Product p : filtered) {
            productFlowPane.getChildren().add(createProductButton(p));
        }

        if (filtered.isEmpty()) {
            Label empty = new Label("No products in this category");
            empty.setStyle("-fx-font-size: 16; -fx-text-fill: #757575; -fx-padding: 20;");
            productFlowPane.getChildren().add(empty);
        }
    }

    private StackPane createProductButton(Product p) {
        String text = p.getName() + "\n£" + String.format("%.2f", p.getPrice());
        if (p.getStock() > 0 && p.getStock() <= 5) {
            text += "\nLow: " + p.getStock();
        }

        Button btn = new Button(text);
        btn.setPrefSize(140, 80);
        btn.setWrapText(true);
        btn.setStyle("-fx-font-size: 13; -fx-background-color: #4CAF50; -fx-text-fill: white;");

        if (p.getStock() <= 0) {
            btn.setDisable(true);
            btn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #555555;");
            btn.setText(p.getName() + "\n£" + String.format("%.2f", p.getPrice()) + "\nOut of stock");
        }

        btn.setOnAction(e -> {
            if (cartService != null && p.getStock() > 0) {
                cartService.addItem(p);
            }
        });

        StackPane wrapper = new StackPane(btn);
        wrapper.setPrefSize(140, 80);

        // ── Allergen badge ──────────────────────────────────────────────
        String allergens = p.getAllergens();
        if (allergens != null && !allergens.trim().isEmpty()) {
            // Amber border on the main button
            if (p.getStock() > 0) {
                btn.setStyle(btn.getStyle() + "; -fx-border-color: #e65100; -fx-border-width: 2; -fx-border-radius: 3;");
            }

            // Badge label — sits in the bottom-right corner of the StackPane
            Label badge = new Label("⚠ " + allergens.trim());
            badge.setStyle(
                    "-fx-background-color: #e65100;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 9;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 2 4 2 4;" +
                    "-fx-background-radius: 3;" +
                    "-fx-wrap-text: true;" +
                    "-fx-max-width: 130;"
            );
            badge.setMaxWidth(130);
            badge.setWrapText(true);
            StackPane.setAlignment(badge, Pos.BOTTOM_CENTER);
            StackPane.setMargin(badge, new Insets(0, 0, 3, 0));
            badge.setMouseTransparent(true); // clicks pass through to the button

            // Full allergen list on hover
            badge.setTooltip(new Tooltip("Contains: " + allergens.trim()));
            btn.setTooltip(new Tooltip("⚠ Allergens: " + allergens.trim()));

            wrapper.getChildren().add(badge);
        }

        return wrapper;
    }

    @FXML
    private void onRefresh() {
        initializeCategories();  // reload categories from DB
        loadProducts();          // reload products in current category
        categoryTitle.setText(
                currentCategory.equals("All")
                        ? "All Products – refreshed"
                        : currentCategory + " Products – refreshed"
        );
    }



    // Periodic background refresh.
    private void startAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }

        autoRefreshTimer = new Timeline(
                new KeyFrame(Duration.seconds(30), event -> {
                    // You can add isShowing() check if you have access to Stage
                    refreshView();
                })
        );
        autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimer.play();
    }

    // Public method – can be called from outside (focus listener, modal close, etc.)
    public void refreshView() {
        initializeCategories();
        loadProducts();
    }

    // Call this when closing the window / unloading the controller
    public void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
            autoRefreshTimer = null;
        }
    }
}