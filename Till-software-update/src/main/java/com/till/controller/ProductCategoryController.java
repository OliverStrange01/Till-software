package com.till.controller;

import com.till.dao.ProductDAO;
import com.till.model.Product;
import com.till.service.CartService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.*;
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

    // ── Allergen map ──────────────────────────────────────────────────────────
    private static final Map<String, String> ALLERGENS = new HashMap<>();
    static {
        ALLERGENS.put("Cappuccino",       "Dairy, Caffeine");
        ALLERGENS.put("Espresso",         "Caffeine");
        ALLERGENS.put("Latte",            "Dairy, Caffeine");
        ALLERGENS.put("Croissant",        "Gluten, Dairy, Eggs");
        ALLERGENS.put("Doughnut",         "Gluten, Dairy, Eggs");
        ALLERGENS.put("Baguette",         "Gluten");
        ALLERGENS.put("Cheese",           "Dairy");
        ALLERGENS.put("Cheesecake Slice", "Gluten, Dairy, Eggs");
        ALLERGENS.put("Ice Cream",        "Dairy, Eggs");
        ALLERGENS.put("Brownie",          "Gluten, Dairy, Eggs, Nuts");
        ALLERGENS.put("Chewing Tobacco",  "None");
        ALLERGENS.put("Cigarettes",       "None");
        ALLERGENS.put("Vodka",            "None");
        ALLERGENS.put("Whisky",           "Gluten");
        ALLERGENS.put("Beer",             "Gluten");
        ALLERGENS.put("Wine",             "Sulphites");
        ALLERGENS.put("Crisps",           "Gluten, Dairy");
        ALLERGENS.put("Chocolate Bar",    "Dairy, Nuts, Gluten");
        ALLERGENS.put("Nuts",             "Nuts");
        ALLERGENS.put("Cola",             "None");
        ALLERGENS.put("Water",            "None");
        ALLERGENS.put("Juice",            "None");
    }

    // Age restricted categories
    private static final Set<String> AGE_RESTRICTED_CATEGORIES = new HashSet<>(
            Arrays.asList("Alcohol", "Tobacco")
    );
    private static final Set<String> AGE_RESTRICTED_NAMES = new HashSet<>(
            Arrays.asList("Vodka", "Whisky", "Beer", "Wine",
                    "Cigarettes", "Chewing Tobacco", "Knife")
    );

    public void setCartService(CartService service) {
        this.cartService = service;
        initializeCategories();
        loadProducts();
        startAutoRefresh();
    }

    private void initializeCategories() {
        categoryBar.getChildren().clear();

        List<String> dbCategories = productDAO.getAllCategories();
        List<String> displayCategories = new ArrayList<>();
        displayCategories.add("All");
        displayCategories.addAll(dbCategories);

        for (String cat : displayCategories) {
            Button catBtn = new Button(cat);
            catBtn.setPrefSize(100, 40);
            catBtn.setStyle(cat.equals(currentCategory)
                    ? "-fx-font-size: 14; -fx-background-color: #1976D2; -fx-text-fill: white;"
                    : "-fx-font-size: 14; -fx-background-color: #2196F3; -fx-text-fill: white;");

            catBtn.setOnAction(e -> {
                currentCategory = cat;
                categoryTitle.setText(cat.equals("All") ? "All Products" : cat + " Products");
                loadProducts();
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

        filtered.sort(Comparator.comparing(Product::getName));

        for (Product p : filtered) {
            productFlowPane.getChildren().add(createProductTile(p));
        }

        if (filtered.isEmpty()) {
            Label empty = new Label("No products in this category");
            empty.setStyle("-fx-font-size: 16; -fx-text-fill: #757575; -fx-padding: 20;");
            productFlowPane.getChildren().add(empty);
        }
    }

    private HBox createProductTile(Product p) {
        // ── Main product button ───────────────────────────────────────────────
        String text = p.getName() + "\n£" + String.format("%.2f", p.getPrice());
        if (p.getStock() > 0 && p.getStock() <= 5) {
            text += "\nLow: " + p.getStock();
        }

        Button productBtn = new Button(text);
        productBtn.setPrefSize(130, 80);

        if (p.getStock() <= 0) {
            productBtn.setDisable(true);
            productBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #555555; -fx-font-size: 12;");
            productBtn.setText(p.getName() + "\n£" + String.format("%.2f", p.getPrice()) + "\nOut of stock");
        } else {
            productBtn.setStyle("-fx-font-size: 13; -fx-background-color: #4CAF50; " +
                    "-fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 6 0 0 6; -fx-cursor: hand;");
            productBtn.setWrapText(true);
            productBtn.setOnAction(e -> handleAddToCart(p));
        }

        // ── Allergen info button ──────────────────────────────────────────────
        Button infoBtn = new Button("ⓘ");
        infoBtn.setPrefSize(28, 80);
        infoBtn.setStyle("-fx-background-color: #1e8449; -fx-text-fill: white; " +
                "-fx-font-size: 11; -fx-font-weight: bold; " +
                "-fx-background-radius: 0 6 6 0; -fx-cursor: hand;");
        infoBtn.setOnAction(e -> showAllergenInfo(p));

        HBox tile = new HBox(0, productBtn, infoBtn);
        return tile;
    }

    private void handleAddToCart(Product p) {
        if (cartService == null) return;

        // ── Age verification ──────────────────────────────────────────────────
        boolean ageRestricted = AGE_RESTRICTED_CATEGORIES.contains(p.getCategory())
                || AGE_RESTRICTED_NAMES.contains(p.getName());

        if (ageRestricted) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Age Verification Required");
            alert.setHeaderText("⚠ Age Restricted Product");
            alert.setContentText(
                    "\"" + p.getName() + "\" is an age-restricted item.\n\n" +
                            "Please verify the customer is aged 18 or over before proceeding.\n\n" +
                            "Is the customer 18 or over?"
            );
            alert.getDialogPane().setStyle("-fx-border-color: #e74c3c; -fx-border-width: 3;");

            ButtonType yesBtn = new ButtonType("Yes, 18+", ButtonBar.ButtonData.OK_DONE);
            ButtonType noBtn  = new ButtonType("No, refuse sale", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yesBtn, noBtn);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == noBtn) {
                showInfo("Sale Refused",
                        "Sale of \"" + p.getName() + "\" refused — customer is under 18.");
                return;
            }
        }

        cartService.addItem(p);
    }

    private void showAllergenInfo(Product p) {
        String allergens = ALLERGENS.getOrDefault(p.getName(), "Not listed");
        boolean hasAllergens = !allergens.equalsIgnoreCase("none")
                && !allergens.equalsIgnoreCase("not listed");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Allergen Information");
        alert.setHeaderText(p.getName() + (hasAllergens ? " ⚠ Contains Allergens" : " ✓ No Allergens"));

        String content = hasAllergens
                ? "⚠ ALLERGENS PRESENT:\n\n• " + allergens.replace(", ", "\n• ") +
                "\n\nPlease inform the customer before serving."
                : "✓ No listed allergens.\n\nAlways check with staff if in doubt.";

        alert.setContentText(content);
        alert.getDialogPane().setStyle(hasAllergens
                ? "-fx-border-color: #e74c3c; -fx-border-width: 3;"
                : "-fx-border-color: #27ae60; -fx-border-width: 3;");
        alert.showAndWait();
    }

    @FXML
    private void onRefresh() {
        initializeCategories();
        loadProducts();
        categoryTitle.setText(currentCategory.equals("All")
                ? "All Products – refreshed"
                : currentCategory + " Products – refreshed");
    }

    private void startAutoRefresh() {
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
        autoRefreshTimer = new Timeline(
                new KeyFrame(Duration.seconds(30), event -> refreshView())
        );
        autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimer.play();
    }

    public void refreshView() {
        initializeCategories();
        loadProducts();
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
            autoRefreshTimer = null;
        }
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}