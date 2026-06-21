package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.model.CartItem;
import com.fashionstore.model.SavedItem;
import com.fashionstore.model.User;
import com.fashionstore.security.CSRFProtection;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.CartService;
import com.fashionstore.service.SavedItemService;
import com.fashionstore.util.JsonUtil;
import com.fashionstore.util.ValidationUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * CartController - MVC Architecture
 * 
 * REFACTORED FOR PROPER MVC:
 * - Backend handles ALL cart operations
 * - Backend calculates ALL cart totals
 * - Backend validates ALL cart items
 * - Frontend only provides AJAX triggers
 * - Frontend only displays backend-calculated totals
 * - No frontend cart calculations
 * 
 * Request Flow:
 * GET /cart → Load cart items, calculate totals, display cart.jsp
 * POST /cart?action=add → Add item, recalculate totals, return JSON
 * POST /cart?action=remove → Remove item, recalculate totals, return JSON
 * POST /cart?action=update → Update quantity, recalculate totals, return JSON
 * 
 * Response includes:
 * - Updated cart items
 * - Recalculated cart total
 * - Updated cart item count
 * - Error messages if any
 */
@WebServlet("/cart")
public class CartController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private CartService cartService;
    private SavedItemService savedItemService;

    @Override
    public void init() {
        ServiceRegistry registry = ServiceRegistry.getInstance();
        cartService = registry.getCartService();
        savedItemService = registry.getSavedItemService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        User user = (User) session.getAttribute("customerAuth");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        if (action != null && !action.equals("view") && !action.equals("get")) {
            // Support legacy GET routes by delegating to doPost
            doPost(request, response);
            return;
        }

        int userId = user.getUserId();
        List<CartItem> cartItems = cartService.getCartItems(userId);
        
        // Sync session
        session.setAttribute("cartItems", cartItems);

        double total = cartService.calculateCartTotal(userId);

        request.setAttribute("cartItems", cartItems);
        request.setAttribute("cartTotal", total);

        // Ensure CSRF token is available in request attributes for JSP
        CSRFProtection.addTokenToRequest(request);

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

            User user = (User) session.getAttribute("customerAuth");
            if (user == null) {
                if (isAjaxRequest(request)) {
                    sendErrorResponse(response, "Please login to continue.", 401);
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            int userId = user.getUserId();
            boolean isAjax = isAjaxRequest(request);

            // Read action and parameters, supporting both JSON and Form-Encoded
            String action = request.getParameter("action");
            Integer cartItemId = parseIntOrNull(request.getParameter("cartItemId"));
            Integer currentQty = parseIntOrNull(request.getParameter("currentQty"));
            Integer productIdBoxed = parseIntOrNull(request.getParameter("productId"));
            Integer parsedQty = parseIntOrNull(request.getParameter("quantity"));
            String size = request.getParameter("size");
            String couponCode = request.getParameter("couponCode");

            if (request.getContentType() != null && request.getContentType().contains("application/json")) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map<String, Object> payload = mapper.readValue(request.getReader(), java.util.Map.class);
                    if (payload != null) {
                        if (payload.containsKey("action")) action = String.valueOf(payload.get("action"));
                        if (payload.containsKey("cartItemId")) cartItemId = parseIntOrNull(String.valueOf(payload.get("cartItemId")));
                        if (payload.containsKey("currentQty")) currentQty = parseIntOrNull(String.valueOf(payload.get("currentQty")));
                        if (payload.containsKey("productId")) productIdBoxed = parseIntOrNull(String.valueOf(payload.get("productId")));
                        if (payload.containsKey("quantity")) parsedQty = parseIntOrNull(String.valueOf(payload.get("quantity")));
                        if (payload.containsKey("size")) size = String.valueOf(payload.get("size"));
                        if (payload.containsKey("couponCode")) couponCode = String.valueOf(payload.get("couponCode"));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse JSON cart request", e);
                }
            }

            if ("increase".equals(action)) {
                if (cartItemId == null || currentQty == null) {
                    if (isAjax) { sendErrorResponse(response, "Invalid cart parameters", 400); return; }
                    response.sendRedirect(request.getContextPath() + "/cart"); return;
                }
                int newQty = Math.min(currentQty + 1, ValidationUtil.MAX_PRODUCT_QUANTITY_PER_LINE);
                cartService.updateCartItemQuantity(cartItemId, userId, newQty);
                syncSessionCart(session, userId);
                
                if (isAjax) {
                    sendAjaxResponse(response, cartItemId, newQty, userId);
                    return;
                }
            } else if ("decrease".equals(action)) {
                if (cartItemId == null || currentQty == null) {
                    if (isAjax) { sendErrorResponse(response, "Invalid cart parameters", 400); return; }
                    response.sendRedirect(request.getContextPath() + "/cart"); return;
                }
                int newQty = currentQty - 1;
                if (newQty < 1) {
                    cartService.removeCartItem(cartItemId, userId);
                    syncSessionCart(session, userId);
                    if (isAjax) {
                        sendAjaxResponse(response, cartItemId, 0, userId);
                        return;
                    }
                } else {
                    cartService.updateCartItemQuantity(cartItemId, userId, newQty);
                    syncSessionCart(session, userId);
                    if (isAjax) {
                        sendAjaxResponse(response, cartItemId, newQty, userId);
                        return;
                    }
                }
            } else if ("update".equals(action)) {
                if (cartItemId == null || currentQty == null) {
                    if (isAjax) { sendErrorResponse(response, "Invalid cart parameters", 400); return; }
                    response.sendRedirect(request.getContextPath() + "/cart"); return;
                }
                int newQty = ValidationUtil.clampInt(currentQty, 1, ValidationUtil.MAX_PRODUCT_QUANTITY_PER_LINE);
                cartService.updateCartItemQuantity(cartItemId, userId, newQty);
                syncSessionCart(session, userId);
                if (isAjax) {
                    sendAjaxResponse(response, cartItemId, newQty, userId);
                    return;
                }
            } else if ("remove".equals(action)) {
                if (cartItemId == null || cartItemId <= 0) {
                    if (isAjax) { sendErrorResponse(response, "Invalid cart item id", 400); return; }
                    response.sendRedirect(request.getContextPath() + "/cart"); return;
                }

                boolean deleted = cartService.removeCartItem(cartItemId, userId);
                if (!deleted) {
                    logger.info("Cart remove no-op: item #{} not found for user #{}", cartItemId, userId);
                    if (isAjax) {
                        sendErrorResponse(response, "Item not found in your cart", 404);
                        return;
                    }
                    response.sendRedirect(request.getContextPath() + "/cart");
                    return;
                }

                syncSessionCart(session, userId);
                logger.info("Cart item #{} removed for user #{}", cartItemId, userId);

                if (isAjax) {
                    sendAjaxResponse(response, cartItemId, 0, userId);
                    return;
                }
            } else if ("add".equals(action)) {
                if (productIdBoxed == null) {
                    if (isAjax) { sendErrorResponse(response, "Invalid product id", 400); return; }
                    response.sendRedirect(request.getContextPath() + "/cart"); return;
                }
                int productId = productIdBoxed;
                int qty = 1;
                if (parsedQty != null) {
                    qty = ValidationUtil.clampInt(parsedQty, 1, ValidationUtil.MAX_PRODUCT_QUANTITY_PER_LINE);
                }

                CartItem item = new CartItem();
                item.setUserId(userId);
                item.setProductId(productId);
                item.setSizeLabel(size != null ? size : "M");
                item.setQuantity(qty);

                cartService.addToCart(userId, item.getProductId(), item.getSizeLabel(), item.getQuantity());
                syncSessionCart(session, userId);
                
                if (isAjax) {
                    sendFullCartResponse(response, userId);
                    return;
                }
            } else if ("get".equals(action) || "view".equals(action)) {
                syncSessionCart(session, userId);
                if (isAjax) {
                    sendFullCartResponse(response, userId);
                    return;
                }
            } else if ("applyCoupon".equals(action)) {
                couponCode = ValidationUtil.normalizeCouponCode(couponCode);
                List<CartItem> liveCart = cartService.getCartItems(userId);
                double cartTotal = 0;
                for (CartItem ci : liveCart) {
                    cartTotal += ci.getPrice() * ci.getQuantity();
                }

                if (isAjax) {
                    applyCouponResponse(response, couponCode, cartTotal, userId);
                    return;
                }
            } else if ("saveForLater".equals(action)) {
                if (cartItemId == null) {
                    if (isAjax) { sendErrorResponse(response, "Invalid cart item id", 400); return; }
                    response.sendRedirect(request.getContextPath() + "/cart"); return;
                }
                List<CartItem> currentCartItems = cartService.getCartItems(userId);
                final int finalCartItemId = cartItemId;
                CartItem cartItem = currentCartItems.stream()
                    .filter(i -> i.getCartItemId() == finalCartItemId)
                    .findFirst().orElse(null);
                
                if (cartItem != null) {
                    SavedItem savedItem = new SavedItem(userId, cartItem.getProductId(), cartItem.getSizeLabel());
                    savedItemService.saveItem(savedItem);
                    cartService.removeCartItem(cartItemId, userId);
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
        List<CartItem> cartItems = cartService.getCartItems(userId);
        session.setAttribute("cartItems", cartItems);
    }

    private void sendAjaxResponse(HttpServletResponse response, int cartItemId, int newQty, int userId) throws IOException {
        List<CartItem> cartItems = cartService.getCartItems(userId);
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
        
        response.getWriter().write(JsonUtil.toJson(map));
    }

    private void sendFullCartResponse(HttpServletResponse response, int userId) throws IOException {
        List<CartItem> cartItems = cartService.getCartItems(userId);
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
        
        response.getWriter().write(JsonUtil.toJson(map));
    }

    private void applyCouponResponse(HttpServletResponse response, String couponCode, double cartTotal, int userId) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        
        try {
            double totalWithCoupon = cartService.calculateCartTotalWithCoupon(userId, couponCode);
            if (totalWithCoupon < cartTotal) {
                double discount = cartTotal - totalWithCoupon;
                map.put("success", true);
                map.put("discount", discount);
                map.put("total", totalWithCoupon);
                map.put("message", "Coupon applied successfully");
            } else {
                map.put("success", false);
                map.put("message", "Invalid coupon code or not applicable");
            }
        } catch (Exception e) {
            logger.error("Error validating coupon", e);
            map.put("success", false);
            map.put("message", "Error validating coupon");
        }
        
        response.getWriter().write(JsonUtil.toJson(map));
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) || 
               (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));
    }

    /**
     * Parse an integer parameter without throwing. Returns null on missing or
     * malformed input so callers can return a clean HTTP 400 instead of a 500.
     */
    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);
        
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("success", false);
        map.put("message", message);
        map.put("status", "error");
        
        response.getWriter().write(JsonUtil.toJson(map));
    }
}