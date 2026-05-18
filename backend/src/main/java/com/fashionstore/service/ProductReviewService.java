package com.fashionstore.service;

import com.fashionstore.model.ProductReview;

import java.util.List;

/**
 * ProductReviewService - MVC Service Layer Interface
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL product review business logic in service layer
 * - ALL validation in service layer
 * - Controllers only handle request/response
 * - DAO layer only handles database access
 */
public interface ProductReviewService {
    
    /**
     * Create a new product review with validation
     */
    boolean createReview(ProductReview review);
    
    /**
     * Get all reviews for a product
     */
    List<ProductReview> getReviewsByProductId(int productId);
    
    /**
     * Get review by ID
     */
    ProductReview getReviewById(int reviewId);
    
    /**
     * Update review with validation
     */
    boolean updateReview(ProductReview review);
    
    /**
     * Delete review
     */
    boolean deleteReview(int reviewId, int userId);
    
    /**
     * Get reviews by user
     */
    List<ProductReview> getReviewsByUserId(int userId);
    
    /**
     * Get average rating for a product
     */
    double getAverageRating(int productId);
    
    /**
     * Validate review before creation
     */
    boolean validateReview(ProductReview review);
}
