package com.till.model;

import javafx.beans.property.*;
import java.util.Locale;

public class Product {

    private final StringProperty id       = new SimpleStringProperty();
    private final StringProperty name     = new SimpleStringProperty();
    private final DoubleProperty price    = new SimpleDoubleProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final StringProperty barcode  = new SimpleStringProperty();
    private final BooleanProperty weighted = new SimpleBooleanProperty(false);
    private final StringProperty unit = new SimpleStringProperty("each");
    private final StringProperty allergens = new SimpleStringProperty("");

    private final IntegerProperty stock      = new SimpleIntegerProperty(0);
    private final IntegerProperty stockToAdd = new SimpleIntegerProperty(0);
    private final IntegerProperty lowStockThreshold = new SimpleIntegerProperty(10);


    public Product() {
        // default / empty constructor
    }

    public Product(String id, String name, double price) {
        this(id, name, price, "Uncategorized", 0);
    }

    public Product(String id, String name, double price, String category) {
        this(id, name, price, category, 0);
    }

    public Product(String id, String name, double price, String category, int stock) {
        setId(id);
        setName(name);
        setPrice(price);
        setCategory(category != null ? category : "Uncategorized");
        setStock(stock);
        setStockToAdd(0);
        setBarcode("");
    }

    // Getters & Setters
    public String getId()          { return id.get(); }
    public void setId(String id)   { this.id.set(id); }

    public String getName()        { return name.get(); }
    public void setName(String n)  { this.name.set(n); }

    public double getPrice()       { return price.get(); }
    public void setPrice(double p) { this.price.set(p); }

    public String getCategory()    { return category.get(); }
    public void setCategory(String c) { this.category.set(c != null ? c : "Uncategorized"); }

    public String getBarcode()             { return barcode.get(); }
    public void setBarcode(String barcode) { this.barcode.set(barcode != null ? barcode : ""); }

    public boolean isWeighted() { return weighted.get(); }
    public void setWeighted(boolean value) { this.weighted.set(value); }

    public String getUnit() { return unit.get(); }
    public void setUnit(String value) { this.unit.set(value != null ? value : "each"); }

    public String getAllergens() { return allergens.get(); }
    public void setAllergens(String value) { this.allergens.set(value != null ? value : ""); }

    public int getStock()          { return stock.get(); }
    public void setStock(int s)    { this.stock.set(s); }

    public int getStockToAdd()     { return stockToAdd.get(); }
    public void setStockToAdd(int value) { this.stockToAdd.set(value); }
    public int getLowStockThreshold() { return lowStockThreshold.get();}
    public void setLowStockThreshold(int value) { this.lowStockThreshold.set(value); }

    // Property methods for TableView / PropertyValueFactory
    public StringProperty idProperty()          { return id; }
    public StringProperty nameProperty()        { return name; }
    public DoubleProperty priceProperty()       { return price; }
    public StringProperty categoryProperty()    { return category; }
    public StringProperty barcodeProperty()     { return barcode; }
    public BooleanProperty weightedProperty() { return weighted; }
    public StringProperty unitProperty() { return unit; }
    public StringProperty allergensProperty() { return allergens; }
    public IntegerProperty stockProperty()      { return stock; }
    public IntegerProperty stockToAddProperty() { return stockToAdd; }
    public IntegerProperty lowStockThresholdProperty() { return lowStockThreshold; }

    @Override
    public String toString() {
        return getName() + " (£" + String.format(Locale.UK, "%.2f", getPrice()) + ", stock: " + getStock() + ")";
    }
}