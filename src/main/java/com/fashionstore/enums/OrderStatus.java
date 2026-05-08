package com.fashionstore.enums;

/**
 * Order status enum for tracking order lifecycle
 */
public enum OrderStatus {
    PROCESSING("Processing"),
    PACKED("Packed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded");
    
    private final String displayName;
    
    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static OrderStatus fromString(String status) {
        for (OrderStatus os : OrderStatus.values()) {
            if (os.name().equalsIgnoreCase(status)) {
                return os;
            }
        }
        return PROCESSING; // Default
    }
}
