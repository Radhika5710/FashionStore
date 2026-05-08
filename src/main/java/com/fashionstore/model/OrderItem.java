package com.fashionstore.model;

public class OrderItem {

    private int orderItemId;
    private int orderId;
    private int productId;
    private String sizeLabel; // ✅ Architecture Upgrade
    private int quantity;
    private double price; // ✅ IMPORTANT (this was missing)

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getSizeLabel() {
        return sizeLabel;
    }

    public void setSizeLabel(String sizeLabel) {
        this.sizeLabel = sizeLabel;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // ✅ FIXED METHOD
    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}