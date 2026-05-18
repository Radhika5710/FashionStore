package com.fashionstore.daoimpl;

import com.fashionstore.dao.ProductReviewDAO;
import com.fashionstore.model.ProductReview;
import com.fashionstore.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductReviewDAOImpl implements ProductReviewDAO {
    private static final Logger logger = LoggerFactory.getLogger(ProductReviewDAOImpl.class);

    @Override
    public boolean createReview(ProductReview review) {
        String sql = "INSERT INTO product_reviews (product_id, user_id, rating, title, comment) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, review.getProductId());
            ps.setInt(2, review.getUserId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getTitle());
            ps.setString(5, review.getComment());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating review: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<ProductReview> getReviewsByProductId(int productId) {
        List<ProductReview> reviews = new ArrayList<>();
        String sql = "SELECT review_id, product_id, user_id, rating, title, comment, created_at FROM product_reviews WHERE product_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reviews.add(mapReview(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting reviews: {}", e.getMessage(), e);
        }
        return reviews;
    }

    @Override
    public ProductReview getReviewById(int reviewId) {
        String sql = "SELECT review_id, product_id, user_id, rating, title, comment, created_at FROM product_reviews WHERE review_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapReview(rs);
            }
        } catch (SQLException e) {
            logger.error("Error getting review: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean updateReview(ProductReview review) {
        String sql = "UPDATE product_reviews SET rating = ?, title = ?, comment = ? WHERE review_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, review.getRating());
            ps.setString(2, review.getTitle());
            ps.setString(3, review.getComment());
            ps.setInt(4, review.getReviewId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating review: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteReview(int reviewId) {
        String sql = "DELETE FROM product_reviews WHERE review_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting review: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<ProductReview> getReviewsByUserId(int userId) {
        List<ProductReview> reviews = new ArrayList<>();
        String sql = "SELECT review_id, product_id, user_id, rating, title, comment, created_at FROM product_reviews WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reviews.add(mapReview(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting reviews by user: {}", e.getMessage(), e);
        }
        return reviews;
    }

    @Override
    public double getAverageRating(int productId) {
        String sql = "SELECT AVG(rating) FROM product_reviews WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting average rating: {}", e.getMessage(), e);
        }
        return 0.0;
    }

    private ProductReview mapReview(ResultSet rs) throws SQLException {
        ProductReview review = new ProductReview();
        review.setReviewId(rs.getInt("review_id"));
        review.setProductId(rs.getInt("product_id"));
        review.setUserId(rs.getInt("user_id"));
        review.setRating(rs.getInt("rating"));
        review.setTitle(rs.getString("title"));
        review.setComment(rs.getString("comment"));
        review.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        return review;
    }
}
