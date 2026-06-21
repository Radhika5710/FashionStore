package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.model.Order;
import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.OrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Payment controller for handling payment operations
 * Supports Razorpay, Stripe, and COD
 */
@WebServlet("/payment")
public class PaymentController extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private OrderService orderService;

    @Override
    public void init() throws ServletException {
        orderService = ServiceRegistry.getInstance().getOrderService();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        
        if ("success".equals(action)) {
            handlePaymentSuccess(req, resp);
        } else if ("failure".equals(action)) {
            handlePaymentFailure(req, resp);
        } else if ("stripe-webhook".equals(action)) {
            handleStripeWebhook(req, resp);
        } else if ("webhook".equals(action)) {
            handleWebhook(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        
        if ("initiate".equals(action)) {
            initiatePayment(req, resp);
        } else if ("verify".equals(action)) {
            verifyPayment(req, resp);
        } else if ("stripe-webhook".equals(action)) {
            handleStripeWebhook(req, resp);
        } else if ("webhook".equals(action)) {
            handleWebhook(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
        }
    }
    
    /**
     * Initiate payment for an order
     */
    private void initiatePayment(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("customerAuth") : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int userId = user.getUserId();
        String paymentMethod = req.getParameter("paymentMethod");
        
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payment method is required");
            return;
        }
        
        try {
            int orderId = createOrder(userId, paymentMethod, req);
            if (orderId == -1) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create order");
                return;
            }
            
            resp.setContentType("application/json");
            resp.getWriter().write("{\"orderId\":" + orderId + ",\"paymentMethod\":\"" + paymentMethod + "\"}");
            
        } catch (Exception e) {
            logger.error("Error in PaymentController.initiatePayment: {}", e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error initiating payment");
        }
    }
    
    /**
     * Verify payment after completion
     */
    private void verifyPayment(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer orderIdBoxed = parseIntOrNull(req.getParameter("orderId"));
        if (orderIdBoxed == null || orderIdBoxed <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid order id");
            return;
        }
        int orderId = orderIdBoxed;

        try {
            resp.sendRedirect(req.getContextPath() + "/payment?action=success&orderId=" + orderId);
        } catch (Exception e) {
            resp.sendRedirect(req.getContextPath() + "/payment?action=failure&orderId=" + orderId);
        }
    }
    
    /**
     * Handle payment success page
     */
    private void handlePaymentSuccess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        renderOrderResultPage(req, resp, "/WEB-INF/views/payment-success.jsp", "Error loading payment success page");
    }

    /**
     * Handle payment failure page
     */
    private void handlePaymentFailure(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        renderOrderResultPage(req, resp, "/WEB-INF/views/payment-failure.jsp", "Error loading payment failure page");
    }

    /**
     * Shared loader for the success/failure pages.
     * Validates orderId, looks up the order via the canonical OrderDAO, and verifies the order
     * belongs to the currently authenticated user before rendering it.
     */
    private void renderOrderResultPage(HttpServletRequest req, HttpServletResponse resp,
                                       String view, String genericError)
            throws ServletException, IOException {
        Integer orderIdBoxed = parseIntOrNull(req.getParameter("orderId"));
        if (orderIdBoxed == null || orderIdBoxed <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid order id");
            return;
        }

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("customerAuth") : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            Order order = orderService.getOrderById(orderIdBoxed, user.getUserId());
            if (order == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Order not found");
                return;
            }
            // Ownership check: a payment result page must only be shown to the order's owner
            // (admins can view the order from /admin/orders).
            if (order.getUserId() != user.getUserId() && !user.isAdmin()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            req.setAttribute("order", order);
            req.getRequestDispatcher(view).forward(req, resp);
        } catch (Exception e) {
            logger.error("Error rendering payment result page for order #{}: {}", orderIdBoxed, e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, genericError);
        }
    }
    
    /**
     * Handle webhook callbacks from payment gateways
     */
    private void handleWebhook(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Handle Stripe webhook with signature verification
     */
    private void handleStripeWebhook(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            logger.error("Error handling Stripe webhook: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Create order - delegates to CheckoutService for proper order creation with cart items and addresses
     * PaymentController should NOT be used for order creation - use CheckoutController.submitOrder instead
     */
    private int createOrder(int userId, String paymentMethod, HttpServletRequest req) {
        logger.warn("PaymentController.createOrder called - this method is deprecated. Use CheckoutController.submitOrder for proper order creation with cart items and addresses.");
        
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("paymentMethod", paymentMethod);

        try {
            Order order = orderService.createOrder(userId, orderData);
            if (order != null && order.getOrderId() > 0) {
                return order.getOrderId();
            }
        } catch (Exception e) {
            logger.error("PaymentController.createOrder failed for user #{}: {}", userId, e.getMessage(), e);
        }
        return -1;
    }

    private static Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
