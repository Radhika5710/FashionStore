package com.fashionstore.service;

import com.fashionstore.model.SavedItem;

import java.util.List;

/**
 * SavedItemService - MVC Service Layer Interface
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL saved item operations in service layer
 * - ALL business logic in service layer
 * - Controllers only handle request/response
 * - DAO layer only handles database access
 */
public interface SavedItemService {
    
    /**
     * Get all saved items for a user
     */
    List<SavedItem> getSavedItems(int userId);
    
    /**
     * Save an item for later
     */
    boolean saveItem(SavedItem savedItem);
    
    /**
     * Remove a saved item
     */
    boolean removeSavedItem(int savedItemId, int userId);
    
    /**
     * Get saved item count for a user
     */
    int getSavedItemCount(int userId);
    
    /**
     * Check if product is already saved
     */
    boolean isProductSaved(int userId, int productId, String size);
}
