package com.till.model;

public class SalesRecord {
    private final String productName;
    private final int quantitySold;
    private final double revenue;

    public SalesRecord(String productName, int quantitySold, double revenue) {
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public String getProductName() { return productName; }
    public int getQuantitySold() { return quantitySold; }
    public double getRevenue() { return revenue; }
}