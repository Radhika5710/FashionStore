package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.CartDAO;
import com.fashionstore.dao.CouponDAO;
import com.fashionstore.dao.SavedItemDAO;
import com.fashionstore.daoimpl.CartDAOImpl;
import com.fashionstore.daoimpl.CouponDAOImpl;
import com.fashionstore.daoimpl.SavedItemDAOImpl;
import com.fashionstore.model.CartItem;
import com.fashionstore.model.Coupon;
import com.fashionstore.model.SavedItem;
import com.fashionstore.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

@WebServlet("/cart")
public class CartController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private CartDAO cartDAO;
    private CouponDAO couponDAO;
    private SavedItemDAO savedItemDAO;

    @Override
    public void init() {
        cartDAO = new CartDAOImpl();
        couponDAO = new CouponDAOImpl();
        savedItemDAO = new SavedItemDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        int userId = user.getUserId();
        List<CartItem> cartItems = cartDAO.getCartItemsByUserId(userId);
        
        // Sync session
        session.setAttribute("cartItems", cartItems);

        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getPrice() * item.getQuantity();
        }

        request.setAttribute("cartItems", cartItems);
        request.setAttribute("cartTotal", total);

        request.getRequestDispatcher("/WEB-INF/views/cart.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                if (isAjaxRequest(request)) {
                    sendErrorResponse(response, "Session expired. Please login again.", 401);
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            User user = (User) session.getAttribute("user");
            if (user == null) {
                if (isAjaxRequest(request)) {
                    sendErrorResponse(response, "Please login to continue.", 401);
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            int userId = user.getUserId();
            String action = request.getParameter("action");
            boolean isAjax = isAjaxRequest(request);

            if ("increase".equals(action)) {
                int cartItemId = Integer.parseInt(request.getParameter("cartItemId"));
                int currentQty = Integer.parseInt(request.getParameter("currentQty"));
                int newQty = currentQty + 1;
                cartDAO.updateQuantity(cartItemId, userId, newQty);
                syncSessionCart(session, userId);
                
                if (isAjax) {
                    sendAjaxResponse(response, cartItemId, newQty, userId);
                    return;
                }
            } else if ("decrease".equals(action)) {
                int cartItemId = Integer.parseInt(request.getParameter("cartItemId"));
                int currentQty = Integer.parseInt(request.getParameter("currentQty"));
                int newQty = currentQty - 1;
                cartDAO.updateQuantity(cartItemId, userId, newQty);
                syncSessionCart(session, userId);
                
                if (isAjax) {
                    sendAjaxResponse(response, cartItemId, newQty, userId);
                    return;
                }
            } else if ("remove".equals(action)) {
                int cartItemId = Integer.parseInt(request.getParameter("cartItemId"));
                cartDAO.removeCartItem(cartItemId, userId);
                syncSessionCart(session, userId);
                
                if (isAjax) {
                    sendAjaxResponse(response, cartItemId, 0, userId);
                    return;
                }
            } else if ("add".equals(action)) {
                int productId = Integer.parseInt(request.getParameter("productId"));
                String size = request.getParameter("size");
                int qty = 1;
                try {
                    String q = request.getParameter("quantity");
                    if (q != null) qty = Integer.parseInt(q);
                } catch (Exception ignored) {}

                CartItem item = new CartItem();
                item.setUserId(userId);
                item.setProductId(productId);
                item.setSizeLabel(size != null ? size : "M");
                item.setQuantity(qty);

                cartDAO.addToCart(item);
                syncSessionCart(session, userId);
                
                if (isAjax) {
                    sendFullCartResponse(response, userId);
                    return;
                }
            } else if ("get".equals(action)) {
                syncSessionCart(session, userId);
                if (isAjax) {
                    sendFullCartResponse(response, userId);
                    return;
                }
            } else if ("applyCoupon".equals(action)) {
                String couponCode = request.getParameter("couponCode");
                double cartTotal = Double.parseDouble(request.getParameter("cartTotal"));
                
                if (isAjax) {
                    applyCouponResponse(response, couponCode, cartTotal, userId);
                    return;
                }
            } else if ("saveForLater".equals(action)) {
                int cartItemId = Integer.parseInt(request.getParameter("cartItemId"));
                List<CartItem> currentCartItems = cartDAO.getCartItemsByUserId(userId);
                CartItem cartItem = currentCartItems.stream()
                    .filter(i -> i.getCartItemId() == cartItemId)
                    .findFirst().orElse(null);
                
                if (cartItem != null) {
                    SavedItem savedItem = new SavedItem(userId, cartItem.getProductId(), cartItem.getSizeLabel());
                    savedItemDAO.saveItem(savedItem);
                    cartDAO.removeCartItem(cartItemId, userId);
                    syncSessionCart(session, userId);
                }
                
                if (isAjax) {
                    sendAjaxResponse(response, cartItemId, 0, userId);
                    return;
                }
            }

            response.sendRedirect(request.getContextPath() + "/cart");

        } catch (Exception e) {
            logger.error("Error in CartController: {}", e.getMessage(), e);
            if (isAjaxRequest(request)) {
                try {
                    sendErrorResponse(response, "Failed to process cart action: " + e.getMessage(), 500);
                } catch (IOException ioException) {
                    logger.error("Failed to send error response", ioException);
                }
            } else {
                response.sendRedirect(request.getContextPath() + "/cart?error=true");
            }
        }
    }

    private void syncSessionCart(HttpSession session, int userId) {
        List<CartItem> cartItems = cartDAO.getCartItemsByUserId(userId);
        session.setAttribute("cartItems", cartItems);
    }

    private void sendAjaxResponse(HttpServletResponse response, int cartItemId, int newQty, int userId) throws IOException {
        List<CartItem> cartItems = cartDAO.getCartItemsByUserId(userId);
        double cartTotal = 0;
        double itemTotal = 0;
        int cartCount = 0;
        boolean removed = true;

        for (CartItem item : cartItems) {
            double lineTotal = item.getPrice() * item.getQuantity();
            cartTotal += lineTotal;
            cartCount += item.getQuantity();
            if (item.getCartItemId() == cartItemId) {
                itemTotal = lineTotal;
                removed = false;
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("success", true);
        map.put("status", "success");
        map.put("cartItemId", cartItemId);
        map.put("newQuantity", newQty);
        map.put("itemTotal", itemTotal);
        map.put("cartTotal", cartTotal);
        map.put("cartCount", cartCount);
        map.put("removed", removed);
        map.put("cartItems", cartItems);
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        response.getWriter().write(gson.toJson(map));
    }

    private void sendFullCartResponse(HttpServletResponse response, int userId) throws IOException {
        List<CartItem> cartItems = cartDAO.getCartItemsByUserId(userId);
        double cartTotal = 0;
        int cartCount = 0;

        for (CartItem item : cartItems) {
            cartTotal += item.getPrice() * item.getQuantity();
            cartCount += item.getQuantity();
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("success", true);
        map.put("status", "success");
        map.put("cartTotal", cartTotal);
        map.put("cartCount", cartCount);
        map.put("cartItems", cartItems);
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        response.getWriter().write(gson.toJson(map));
    }

    private void applyCouponResponse(HttpServletResponse response, String couponCode, double cartTotal, int userId) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        
        try {
            Coupon coupon = couponDAO.getCouponByCode(couponCode);
            if (coupon == null) {
                map.put("success", false);
                map.put("message", "Invalid coupon code");
            } else if (!coupon.isValid()) {
                map.put("success", false);
                map.put("message", "Coupon is expired or inactive");
            } else if (cartTotal < coupon.getMinimumOrderAmount()) {
                map.put("success", false);
                map.put("message", "Minimum order value of ₹" + coupon.getMinimumOrderAmount() + " required");
            } else {
                double discount = coupon.calculateDiscount(cartTotal);
                double total = cartTotal - discount;
                map.put("success", true);
                map.put("discount", discount);
                map.put("total", total);
                map.put("message", "Coupon applied successfully");
            }
        } catch (Exception e) {
            logger.error("Error validating coupon", e);
            map.put("success", false);
            map.put("message", "Error validating coupon");
        }
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        response.getWriter().write(gson.toJson(map));
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) || 
               (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));
    }

    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);
        
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("success", false);
        map.put("message", message);
        map.put("status", "error");
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        response.getWriter().write(gson.toJson(map));
    }
}