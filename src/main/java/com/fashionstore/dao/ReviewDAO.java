package com.fashionstore.dao;

import com.fashionstore.model.Review;
import java.util.List;

public interface ReviewDAO {
    boolean addReview(Review review);
    List<Review> getReviewsByProductId(int productId);
    double getAverageRating(int productId);
    int getReviewCount(int productId);
}
