package com.fashionstore.model;

import java.sql.Timestamp;

public class SavedItem {
    private int savedItemId;
    private int userId;
    private int productId;
    private String sizeLabel;
    private Timestamp savedAt;
    
    // Product details for display (not stored in DB)
    private String productName;
    private String imageUrl;
    private double price;
    
    public SavedItem() {}
    
    public SavedItem(int userId, int productId, String sizeLabel) {
        this.userId = userId;
        this.productId = productId;
        this.sizeLabel = sizeLabel;
    }
    
    // Getters and Setters
    public int getSavedItemId() { return savedItemId; }
    public void setSavedItemId(int savedItemId) { this.savedItemId = savedItemId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public String getSizeLabel() { return sizeLabel; }
    public void setSizeLabel(String sizeLabel) { this.sizeLabel = sizeLabel; }
    
    public Timestamp getSavedAt() { return savedAt; }
    public void setSavedAt(Timestamp savedAt) { this.savedAt = savedAt; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
