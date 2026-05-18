package com.fashionstore.service;

import com.fashionstore.model.Order;
import com.fashionstore.model.OrderItem;

import java.util.List;
import java.util.Map;

/**
 * OrderService - MVC Service Layer Interface
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL order business logic in service layer
 * - ALL order creation and validation
 * - ALL payment processing
 * - ALL stock deduction
 * - Backend is single source of truth
 * - Frontend cannot manipulate orders
 * 
 * Order Creation Flow:
 * 1. Validate cart items and availability
 * 2. Calculate totals (subtotal, tax, shipping, discount)
 * 3. Validate payment information
 * 4. Create order record
 * 5. Deduct stock from inventory
 * 6. Process payment
 * 7. Clear cart
 * 8. Return order confirmation
 * 
 * All operations are atomic and transactional.
 * Rollback on any failure to maintain data consistency.
 */
public interface OrderService {
    
    /**
     * Create new order with business logic validation
     * 
     * Backend validates:
     * - Cart items exist and are available
     * - Quantities are valid
     * - Prices match backend prices
     * - Payment information is valid
     * - Address is valid
     * 
     * Frontend cannot manipulate order data.
     */
    Order createOrder(int userId, Map<String, Object> orderData);
    
    /**
     * Get order by ID with business rules
     */
    Order getOrderById(int orderId, int requestingUserId);
    
    /**
     * Get orders for user
     */
    List<Order> getOrdersForUser(int userId);
    
    /**
     * Get all orders (admin)
     */
    List<Order> getAllOrders();
    
    /**
     * Get recent orders
     */
    List<Order> getRecentOrders(int limit);
    
    /**
     * Update order status with business validation
     */
    boolean updateOrderStatus(int orderId, String newStatus, int requestingUserId);
    
    /**
     * Process order payment
     */
    boolean processOrderPayment(int orderId, String paymentMethod, double amount);
    
    /**
     * Cancel order with business rules
     */
    boolean cancelOrder(int orderId, int requestingUserId);
    
    /**
     * Refund order with business rules
     */
    boolean refundOrder(int orderId, int requestingUserId);
    
    /**
     * Get order items
     */
    List<OrderItem> getOrderItems(int orderId);
    
    /**
     * Calculate order total
     */
    double calculateOrderTotal(int orderId);
    
    /**
     * Validate order for processing
     */
    boolean validateOrderForProcessing(int orderId);
    
    /**
     * Get order statistics
     */
    Map<String, Object> getOrderStatistics();
    
    /**
     * Batch load order items for multiple orders
     */
    void batchLoadOrderItems(List<Order> orders);
    
    /**
     * Get orders by status
     */
    List<Order> getOrdersByStatus(String status);
    
    /**
     * Get total revenue
     */
    double getTotalRevenue();
    
    /**
     * Get total order count
     */
    int getTotalOrderCount();
}
