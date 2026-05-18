package com.fashionstore.dao;

import com.fashionstore.model.ProductReview;
import java.util.List;

public interface ProductReviewDAO {
    boolean createReview(ProductReview review);
    List<ProductReview> getReviewsByProductId(int productId);
    ProductReview getReviewById(int reviewId);
    boolean updateReview(ProductReview review);
    boolean deleteReview(int reviewId);
    List<ProductReview> getReviewsByUserId(int userId);
    double getAverageRating(int productId);
}
