package com.till.model;

import javafx.beans.property.*;

public class Product {
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();

    public Product(String id, String name, double price) {
        this.id.set(id);
        this.name.set(name);
        this.price.set(price);
    }

    public String getId() { return id.get(); }
    public void setId(String id) { this.id.set(id); }
    public StringProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    public double getPrice() { return price.get(); }
    public void setPrice(double price) { this.price.set(price); }
    public DoubleProperty priceProperty() { return price; }

    @Override
    public String toString() {
        return getName() + " - $" + String.format("%.2f", getPrice());
    }
}