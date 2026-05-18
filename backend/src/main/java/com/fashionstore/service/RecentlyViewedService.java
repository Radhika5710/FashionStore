package com.fashionstore.service;

import com.fashionstore.model.RecentlyViewed;

import java.util.List;

/**
 * RecentlyViewedService - MVC Service Layer Interface
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL recently viewed business logic in service layer
 * - ALL validation in service layer
 * - Controllers only handle request/response
 * - DAO layer only handles database access
 */
public interface RecentlyViewedService {
    
    /**
     * Add a product to recently viewed
     */
    boolean addRecentlyViewed(int userId, int productId);
    
    /**
     * Get recently viewed products for a user
     */
    List<RecentlyViewed> getRecentlyViewedByUserId(int userId);
    
    /**
     * Get recently viewed products for a user with limit
     */
    List<RecentlyViewed> getRecentlyViewedByUserId(int userId, int limit);
    
    /**
     * Clear recently viewed for a user
     */
    boolean clearRecentlyViewed(int userId);
    
    /**
     * Remove a specific recently viewed item
     */
    boolean removeRecentlyViewed(int userId, int productId);
    
    /**
     * Get recently viewed count for a user
     */
    int getRecentlyViewedCount(int userId);
}
