package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.WishlistDAO;
import com.fashionstore.daoimpl.WishlistDAOImpl;
import com.fashionstore.model.User;
import com.fashionstore.model.WishlistItem;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/wishlist")
public class WishlistController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(WishlistController.class);
    private WishlistDAO wishlistDAO;

    @Override
    public void init() {
        wishlistDAO = new WishlistDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            List<WishlistItem> wishlistItems = wishlistDAO.getWishlistByUserId(user.getUserId());
            request.setAttribute("wishlistItems", wishlistItems);
            request.getRequestDispatcher("/WEB-INF/views/wishlist.jsp").forward(request, response);
        } catch (Exception e) {
            logger.error("Error in WishlistController.doGet for user {}: {}", user.getUserId(), e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> map = new HashMap<>();
        Gson gson = new Gson();
        
        try {
            HttpSession session = request.getSession(false);
            User user = (session != null) ? (User) session.getAttribute("user") : null;
            
            if (user == null) {
                map.put("success", false);
                map.put("message", "Please login to continue.");
                map.put("redirect", request.getContextPath() + "/login");
                response.getWriter().write(gson.toJson(map));
                return;
            }

            String action = request.getParameter("action");
            String productIdStr = request.getParameter("productId");
            
            if (productIdStr == null || productIdStr.isBlank()) {
                map.put("success", false);
                map.put("message", "Product ID missing");
                response.getWriter().write(gson.toJson(map));
                return;
            }

            int productId;
            try {
                productId = Integer.parseInt(productIdStr);
            } catch (NumberFormatException e) {
                map.put("success", false);
                map.put("message", "Invalid Product ID");
                response.getWriter().write(gson.toJson(map));
                return;
            }

            if ("toggle".equals(action)) {
                boolean inWishlist = wishlistDAO.isProductInWishlist(user.getUserId(), productId);
                if (inWishlist) {
                    wishlistDAO.removeWishlistItem(user.getUserId(), productId);
                    map.put("success", true);
                    map.put("isFavorite", false);
                    map.put("message", "Removed from wishlist");
                } else {
                    wishlistDAO.addWishlistItem(user.getUserId(), productId);
                    map.put("success", true);
                    map.put("isFavorite", true);
                    map.put("message", "Added to wishlist");
                }
            } else if ("remove".equals(action)) {
                wishlistDAO.removeWishlistItem(user.getUserId(), productId);
                map.put("success", true);
                map.put("isFavorite", false);
                map.put("message", "Removed from wishlist");
            } else {
                map.put("success", false);
                map.put("message", "Invalid action");
            }

            response.getWriter().write(gson.toJson(map));
        } catch (Exception e) {
            logger.error("Error in WishlistController.doPost: {}", e.getMessage(), e);
            map.put("success", false);
            map.put("message", "Failed to process wishlist action: " + e.getMessage());
            map.put("status", "error");
            response.getWriter().write(gson.toJson(map));
        }
    }
}
