package com.fashionstore.service;

import com.fashionstore.model.Product;
import com.fashionstore.model.ProductSize;

import java.util.List;

/**
 * Service interface for inventory management and business logic
 * Handles stock validation, low stock alerts, and inventory updates
 */
public interface InventoryService {
    
    /**
     * Check product availability
     */
    boolean isProductAvailable(int productId, String size, int quantity);
    
    /**
     * Get current stock level
     */
    int getCurrentStock(int productId, String size);
    
    /**
     * Update stock levels
     */
    boolean updateStockLevel(int productId, String size, int newQuantity);
    
    /**
     * Reserve stock for order
     */
    boolean reserveStock(int productId, String size, int quantity);
    
    /**
     * Release reserved stock
     */
    boolean releaseReservedStock(int productId, String size, int quantity);
    
    /**
     * Get low stock products
     */
    List<Product> getLowStockProducts(int threshold);
    
    /**
     * Get out of stock products
     */
    List<Product> getOutOfStockProducts();
    
    /**
     * Validate stock before order
     */
    boolean validateStockForOrder(List<ProductSize> items);
    
    /**
     * Process inventory after order
     */
    boolean processInventoryAfterOrder(List<ProductSize> items);
    
    /**
     * Process inventory after order with connection for transaction support
     * This version is used within a transaction to ensure atomicity
     */
    boolean processInventoryAfterOrder(List<ProductSize> items, java.sql.Connection conn);
    
    /**
     * Get inventory report
     */
    List<Product> getInventoryReport();
    
    /**
     * Check if product needs restocking
     */
    boolean needsRestocking(int productId, String size);
    
    /**
     * Batch update inventory
     */
    boolean batchUpdateInventory(List<ProductSize> updates);
}
