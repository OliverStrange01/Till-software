package com.till.model;

import javafx.beans.property.*;

public class Product {

    private final StringProperty id       = new SimpleStringProperty();
    private final StringProperty name     = new SimpleStringProperty();
    private final DoubleProperty price    = new SimpleDoubleProperty();
    private final StringProperty category = new SimpleStringProperty();

    // Must be properties for TableView binding
    private final IntegerProperty stock      = new SimpleIntegerProperty(0);
    private final IntegerProperty stockToAdd = new SimpleIntegerProperty(0);


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
    }

    // Getters & Setters (classic style – keep them)
    public String getId()          { return id.get(); }
    public void setId(String id)   { this.id.set(id); }

    public String getName()        { return name.get(); }
    public void setName(String name) { this.name.set(name); }

    public double getPrice()       { return price.get(); }
    public void setPrice(double price) { this.price.set(price); }

    public String getCategory()    { return category.get(); }
    public void setCategory(String category) { this.category.set(category); }

    public int getStock()          { return stock.get(); }
    public void setStock(int stock) { this.stock.set(stock); }

    public int getStockToAdd()     { return stockToAdd.get(); }
    public void setStockToAdd(int value) { this.stockToAdd.set(value); }

    // Required: Property methods for PropertyValueFactory
    public StringProperty idProperty()          { return id; }
    public StringProperty nameProperty()        { return name; }
    public DoubleProperty priceProperty()       { return price; }
    public StringProperty categoryProperty()    { return category; }
    public IntegerProperty stockProperty()      { return stock; }
    public IntegerProperty stockToAddProperty() { return stockToAdd; }

    @Override
    public String toString() {
        return getName() + " (£" + String.format("%.2f", getPrice()) + ", stock: " + getStock() + ")";
    }
}