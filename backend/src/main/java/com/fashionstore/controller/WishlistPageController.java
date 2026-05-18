package com.fashionstore.controller;

import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.WishlistService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Frontend controller for wishlist page
 * Renders the wishlist UI page
 */
@WebServlet("/wishlist")
public class WishlistPageController extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(WishlistPageController.class);
    private WishlistService wishlistService;

    @Override
    public void init() {
        wishlistService = ServiceRegistry.getInstance().getWishlistService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("customerAuth") : null;

        try {
            // Set request attributes for JSP
            java.util.List<?> wishlistItems = new java.util.ArrayList<>();
            
            if (user != null) {
                // Get wishlist items for authenticated user
                Map<String, Object> wishlistData = wishlistService.getWishlist(user.getUserId());
                Object items = (wishlistData != null) ? wishlistData.get("items") : null;
                if (items instanceof java.util.List<?>) {
                    wishlistItems = (java.util.List<?>) items;
                }
            }
            
            request.setAttribute("wishlistItems", wishlistItems);
            request.setAttribute("isAuthenticated", user != null);
            
            // Forward to JSP
            request.getRequestDispatcher("/WEB-INF/views/wishlist.jsp").forward(request, response);
            
        } catch (Exception e) {
            logger.error("Error loading wishlist page: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load wishlist");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // POST requests should go to API endpoint
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Use /api/wishlist for API requests");
    }
}
