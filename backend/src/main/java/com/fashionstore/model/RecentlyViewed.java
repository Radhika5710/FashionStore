package com.fashionstore.model;

import java.time.LocalDateTime;

public class RecentlyViewed {
    private int id;
    private int userId;
    private int productId;
    private LocalDateTime viewedAt;

    public RecentlyViewed() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
}
