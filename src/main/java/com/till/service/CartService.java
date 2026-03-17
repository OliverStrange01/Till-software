package com.till.service;

import java.util.ArrayList;
import java.util.List;

import com.till.model.OrderItems;
import com.till.model.Products;

public class CartService {

    private final List <OrderItems> items = new ArrayList<>();

    public void addProduct(Products product) {
        for (OrderItems item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.increaseQuantity();
                return;
            }
        }
        items.add(new OrderItems(product));
    }

    public List<OrderItems> getItems() {
        return items;
    }

    public double getTotal() {
        double total = 0;
        
        for (OrderItems item : items) {
            total += item.getSubtotal();
        }

        return total;   
    }

    public double calculateChange(double cashGiven) {
        double total = getTotal();
        
        if (cashGiven < total) {
            return -1; // Not enough cash
        }

        return cashGiven - total; // Change to return
    }

    public void clearCart() {
        items.clear();
    }
}
