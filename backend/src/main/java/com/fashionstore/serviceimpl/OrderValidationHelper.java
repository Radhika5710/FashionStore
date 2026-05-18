package com.fashionstore.serviceimpl;

import com.fashionstore.model.Order;

/**
 * Helper class for order validation logic
 * Extracted from OrderServiceImpl to reduce file size and improve organization
 */
public class OrderValidationHelper {

    private static final String[] VALID_STATUSES = {
        "Pending", "Confirmed", "Processing", "Packing", "Shipped", "Out for Delivery", "Delivered", "Cancelled", "Refunded"
    };
    private static final String[] CANCELLABLE_STATUSES = {"Pending", "Confirmed", "Processing", "Packing"};
    private static final String[] REFUNDABLE_STATUSES = {"Processing", "Packing", "Shipped", "Out for Delivery", "Delivered"};

    /**
     * Validate order status
     */
    public static boolean isValidStatus(String status) {
        for (String validStatus : VALID_STATUSES) {
            if (validStatus.equalsIgnoreCase(status)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate status transition
     */
    public static boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || currentStatus.equalsIgnoreCase(newStatus)) {
            return true;
        }
        
        // Cancel transitions
        if ("Cancelled".equalsIgnoreCase(newStatus)) {
            for (String s : CANCELLABLE_STATUSES) {
                if (s.equalsIgnoreCase(currentStatus)) return true;
            }
            return false;
        }
        
        // Refund transitions
        if ("Refunded".equalsIgnoreCase(newStatus)) {
            for (String s : REFUNDABLE_STATUSES) {
                if (s.equalsIgnoreCase(currentStatus)) return true;
            }
            return false;
        }
        
        // Sequential state machine
        switch (currentStatus.toLowerCase()) {
            case "pending" -> {
                return "confirmed".equalsIgnoreCase(newStatus) || "processing".equalsIgnoreCase(newStatus);
            }
            case "confirmed" -> {
                return "processing".equalsIgnoreCase(newStatus) || "packing".equalsIgnoreCase(newStatus);
            }
            case "processing" -> {
                return "packing".equalsIgnoreCase(newStatus) || "shipped".equalsIgnoreCase(newStatus);
            }
            case "packing" -> {
                return "shipped".equalsIgnoreCase(newStatus);
            }
            case "shipped" -> {
                return "out for delivery".equalsIgnoreCase(newStatus) || "delivered".equalsIgnoreCase(newStatus);
            }
            case "out for delivery" -> {
                return "delivered".equalsIgnoreCase(newStatus);
            }
        }
        return false;
    }

    /**
     * Check if order can be cancelled
     */
    public static boolean canCancelOrder(Order order) {
        for (String cancellableStatus : CANCELLABLE_STATUSES) {
            if (cancellableStatus.equals(order.getStatus())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if order can be refunded
     */
    public static boolean canRefundOrder(Order order) {
        for (String refundableStatus : REFUNDABLE_STATUSES) {
            if (refundableStatus.equals(order.getStatus())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get valid statuses
     */
    public static String[] getValidStatuses() {
        return VALID_STATUSES;
    }

    /**
     * Get cancellable statuses
     */
    public static String[] getCancellableStatuses() {
        return CANCELLABLE_STATUSES;
    }

    /**
     * Get refundable statuses
     */
    public static String[] getRefundableStatuses() {
        return REFUNDABLE_STATUSES;
    }
}
