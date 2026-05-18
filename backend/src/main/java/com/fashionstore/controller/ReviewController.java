package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.model.ProductReview;
import com.fashionstore.model.User;
import com.fashionstore.service.ProductReviewService;
import com.fashionstore.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/review")
public class ReviewController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    private ProductReviewService productReviewService;

    @Override
    public void init() {
        productReviewService = ServiceRegistry.getInstance().getProductReviewService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("customerAuth") : null;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> map = new HashMap<>();

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            map.put("success", false);
            map.put("message", "Please log in to leave a review");
            map.put("redirect", request.getContextPath() + "/login");
            response.getWriter().write(JsonUtil.toJson(map));
            return;
        }

        try {
            int productId;
            int rating;
            try {
                String productIdStr = request.getParameter("productId");
                String ratingStr = request.getParameter("rating");
                if (productIdStr == null || ratingStr == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    map.put("success", false);
                    map.put("message", "productId and rating are required");
                    response.getWriter().write(JsonUtil.toJson(map));
                    return;
                }
                productId = Integer.parseInt(productIdStr);
                rating = Integer.parseInt(ratingStr);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                map.put("success", false);
                map.put("message", "Invalid productId or rating format");
                response.getWriter().write(JsonUtil.toJson(map));
                return;
            }
            String comment = request.getParameter("comment");
            if (productId <= 0 || rating < 1 || rating > 5) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                map.put("success", false);
                map.put("message", "Invalid review input");
                response.getWriter().write(JsonUtil.toJson(map));
                return;
            }
            if (comment == null || comment.trim().isEmpty() || comment.length() > 1000) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                map.put("success", false);
                map.put("message", "Comment must be between 1 and 1000 characters");
                response.getWriter().write(JsonUtil.toJson(map));
                return;
            }

            ProductReview review = new ProductReview();
            review.setUserId(user.getUserId());
            review.setProductId(productId);
            review.setRating(rating);
            review.setComment(comment);

            boolean added = productReviewService.createReview(review);

            if (added) {
                map.put("success", true);
                map.put("message", "Review submitted successfully");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                map.put("success", false);
                map.put("message", "Failed to submit review");
            }
        } catch (Exception e) {
            logger.error("Error in ReviewController.doPost: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            map.put("success", false);
            map.put("message", "An error occurred");
        }

        response.getWriter().write(JsonUtil.toJson(map));
    }
}
