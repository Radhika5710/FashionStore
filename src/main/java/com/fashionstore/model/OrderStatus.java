package com.fashionstore.model;

import java.sql.Timestamp;

public class OrderStatus {
    private int statusId;
    private int orderId;
    private String status; // order_placed, order_confirmed, order_processing, order_shipped, order_delivered, order_cancelled, order_returned, refund_initiated, refund_completed
    private String notes;
    private String location;
    private Timestamp estimatedDate;
    private Timestamp createdAt;

    // Constructors
    public OrderStatus() {}

    public OrderStatus(int orderId, String status, String notes, String location, Timestamp estimatedDate) {
        this.orderId = orderId;
        this.status = status;
        this.notes = notes;
        this.location = location;
        this.estimatedDate = estimatedDate;
    }

    // Getters and Setters
    public int getStatusId() { return statusId; }
    public void setStatusId(int statusId) { this.statusId = statusId; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Timestamp getEstimatedDate() { return estimatedDate; }
    public void setEstimatedDate(Timestamp estimatedDate) { this.estimatedDate = estimatedDate; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // Utility methods
    public String getDisplayStatus() {
        switch (status) {
            case "order_placed": return "Order Placed";
            case "order_confirmed": return "Order Confirmed";
            case "order_processing": return "Processing";
            case "order_shipped": return "Shipped";
            case "order_delivered": return "Delivered";
            case "order_cancelled": return "Cancelled";
            case "order_returned": return "Returned";
            case "refund_initiated": return "Refund Initiated";
            case "refund_completed": return "Refund Completed";
            default: return status.replace("_", " ").toUpperCase();
        }
    }

    public boolean isCompleted() {
        return "order_delivered".equals(status) || "order_cancelled".equals(status) || "refund_completed".equals(status);
    }

    public boolean isActive() {
        return !"order_cancelled".equals(status) && !"order_returned".equals(status);
    }

    @Override
    public String toString() {
        return "OrderStatus{" +
               "statusId=" + statusId +
               ", orderId=" + orderId +
               ", status='" + status + '\'' +
               ", notes='" + notes + '\'' +
               ", location='" + location + '\'' +
               ", estimatedDate=" + estimatedDate +
               ", createdAt=" + createdAt +
               '}';
    }
}
