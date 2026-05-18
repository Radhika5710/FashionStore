package com.fashionstore.serviceimpl;

import com.fashionstore.dao.ProductReviewDAO;
import com.fashionstore.model.ProductReview;
import com.fashionstore.service.ProductReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ProductReviewServiceImpl - MVC Service Layer Implementation
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL product review business logic in service layer
 * - ALL validation in service layer
 * - Controllers only handle request/response
 * - DAO layer only handles database access
 */
public class ProductReviewServiceImpl implements ProductReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductReviewServiceImpl.class);
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MIN_COMMENT_LENGTH = 10;
    private static final int MAX_COMMENT_LENGTH = 1000;
    
    private final ProductReviewDAO reviewDAO;

    public ProductReviewServiceImpl() {
        // Default constructor - DAO will be set via setter injection
        this.reviewDAO = null;
    }

    public ProductReviewServiceImpl(ProductReviewDAO reviewDAO) {
        this.reviewDAO = reviewDAO;
    }

    public void setReviewDAO(ProductReviewDAO reviewDAO) {
        if (this.reviewDAO == null) {
            try {
                java.lang.reflect.Field field = ProductReviewServiceImpl.class.getDeclaredField("reviewDAO");
                field.setAccessible(true);
                field.set(this, reviewDAO);
            } catch (Exception e) {
                logger.error("Failed to set reviewDAO", e);
            }
        }
    }
    
    @Override
    public boolean createReview(ProductReview review) {
        if (!validateReview(review)) {
            return false;
        }
        
        return reviewDAO.createReview(review);
    }
    
    @Override
    public List<ProductReview> getReviewsByProductId(int productId) {
        return reviewDAO.getReviewsByProductId(productId);
    }
    
    @Override
    public ProductReview getReviewById(int reviewId) {
        return reviewDAO.getReviewById(reviewId);
    }
    
    @Override
    public boolean updateReview(ProductReview review) {
        if (!validateReview(review)) {
            return false;
        }
        
        return reviewDAO.updateReview(review);
    }
    
    @Override
    public boolean deleteReview(int reviewId, int userId) {
        // Business logic: Check ownership before deletion
        ProductReview review = reviewDAO.getReviewById(reviewId);
        if (review == null) {
            return false;
        }
        if (review.getUserId() != userId) {
            logger.warn("User {} attempted to delete review {} owned by user {}", userId, reviewId, review.getUserId());
            return false;
        }
        
        return reviewDAO.deleteReview(reviewId);
    }
    
    @Override
    public List<ProductReview> getReviewsByUserId(int userId) {
        return reviewDAO.getReviewsByUserId(userId);
    }
    
    @Override
    public double getAverageRating(int productId) {
        List<ProductReview> reviews = reviewDAO.getReviewsByProductId(productId);
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        
        double sum = 0;
        for (ProductReview review : reviews) {
            sum += review.getRating();
        }
        
        return sum / reviews.size();
    }
    
    @Override
    public boolean validateReview(ProductReview review) {
        if (review == null) {
            logger.error("Review is null");
            return false;
        }
        
        if (review.getProductId() <= 0) {
            logger.error("Invalid product ID: {}", review.getProductId());
            return false;
        }
        
        if (review.getUserId() <= 0) {
            logger.error("Invalid user ID: {}", review.getUserId());
            return false;
        }
        
        if (review.getRating() < MIN_RATING || review.getRating() > MAX_RATING) {
            logger.error("Invalid rating: {}. Must be between {} and {}", review.getRating(), MIN_RATING, MAX_RATING);
            return false;
        }
        
        String comment = review.getComment();
        if (comment == null || comment.trim().isEmpty()) {
            logger.error("Comment is empty");
            return false;
        }
        
        if (comment.length() < MIN_COMMENT_LENGTH || comment.length() > MAX_COMMENT_LENGTH) {
            logger.error("Comment length invalid: {}. Must be between {} and {}", comment.length(), MIN_COMMENT_LENGTH, MAX_COMMENT_LENGTH);
            return false;
        }
        
        return true;
    }
}
