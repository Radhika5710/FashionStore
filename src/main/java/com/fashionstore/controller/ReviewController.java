package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.ReviewDAO;
import com.fashionstore.daoimpl.ReviewDAOImpl;
import com.fashionstore.model.Review;
import com.fashionstore.model.User;
import com.google.gson.Gson;

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
    private ReviewDAO reviewDAO;

    @Override
    public void init() {
        reviewDAO = new ReviewDAOImpl();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> map = new HashMap<>();

        if (user == null) {
            map.put("success", false);
            map.put("message", "Please log in to leave a review");
            map.put("redirect", request.getContextPath() + "/login");
            response.getWriter().write(new Gson().toJson(map));
            return;
        }

        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            int rating = Integer.parseInt(request.getParameter("rating"));
            String comment = request.getParameter("comment");

            Review review = new Review();
            review.setUserId(user.getUserId());
            review.setProductId(productId);
            review.setRating(rating);
            review.setComment(comment);

            boolean added = reviewDAO.addReview(review);

            if (added) {
                map.put("success", true);
                map.put("message", "Review submitted successfully");
            } else {
                map.put("success", false);
                map.put("message", "Failed to submit review");
            }
        } catch (Exception e) {
            logger.error("Error in ReviewController.doPost: {}", e.getMessage(), e);
            map.put("success", false);
            map.put("message", "An error occurred");
        }

        response.getWriter().write(new Gson().toJson(map));
    }
}
