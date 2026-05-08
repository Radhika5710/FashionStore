package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fashionstore.dao.ReviewDAO;
import com.fashionstore.model.Review;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAOImpl implements ReviewDAO {

    private static final Logger logger = LoggerFactory.getLogger(ReviewDAOImpl.class);

    @Override
    public boolean addReview(Review review) {
        String sql = "INSERT INTO reviews (user_id, product_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, review.getUserId());
            ps.setInt(2, review.getProductId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
            
            return ps.executeUpdate() > 0;
            
        } catch (Exception e) {
            logger.error("ReviewDAOImpl.addReview Error: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public List<Review> getReviewsByProductId(int productId) {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT r.review_id, r.user_id, r.product_id, r.rating, r.comment, r.created_at, u.full_name as user_name " +
                     "FROM reviews r JOIN users u ON r.user_id = u.user_id " +
                     "WHERE r.product_id = ? ORDER BY r.created_at DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Review r = new Review();
                r.setReviewId(rs.getInt("review_id"));
                r.setUserId(rs.getInt("user_id"));
                r.setProductId(rs.getInt("product_id"));
                r.setRating(rs.getInt("rating"));
                r.setComment(rs.getString("comment"));
                r.setCreatedAt(rs.getTimestamp("created_at"));
                r.setUserName(rs.getString("user_name"));
                list.add(r);
            }
        } catch (Exception e) {
            logger.error("ReviewDAOImpl.getReviewsByProductId Error: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public double getAverageRating(int productId) {
        String sql = "SELECT AVG(rating) as avg_rating FROM reviews WHERE product_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }
        } catch (Exception e) {
            logger.error("ReviewDAOImpl.getAverageRating Error: {}", e.getMessage());
        }
        return 0.0;
    }

    @Override
    public int getReviewCount(int productId) {
        String sql = "SELECT COUNT(*) as count FROM reviews WHERE product_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            logger.error("ReviewDAOImpl.getReviewCount Error: {}", e.getMessage());
        }
        return 0;
    }
}
