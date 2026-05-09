package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.CartDAO;
import com.fashionstore.daoimpl.CartDAOImpl;
import com.fashionstore.model.CartItem;
import com.fashionstore.model.User;
import com.fashionstore.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@WebServlet("/checkout")
public class CheckoutController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    private CartDAO cartDAO;

    @Override
    public void init() {
        cartDAO = new CartDAOImpl();
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

        int userId = user.getUserId();
        try {
            List<CartItem> cartItems = cartDAO.getCartItemsByUserId(userId);

            if (cartItems == null || cartItems.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/cart");
                return;
            }

            double total = 0;
            for (CartItem item : cartItems) {
                total += item.getPrice() * item.getQuantity();
            }

            request.setAttribute("cartItems", cartItems);
            request.setAttribute("cartTotal", total);
            
            // Generate/retrieve CSRF token for the form
            String csrfToken = (String) session.getAttribute("csrf_token");
            request.setAttribute("csrfToken", csrfToken);

            request.getRequestDispatcher("/WEB-INF/views/checkout.jsp")
                   .forward(request, response);
        } catch (Exception e) {
            logger.error("Error in CheckoutController.doGet: {}", e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        int userId = user.getUserId();
        List<CartItem> cartItems;
        try {
            cartItems = cartDAO.getCartItemsByUserId(userId);
        } catch (Exception e) {
            logger.error("Could not load cart for user #{} during checkout: {}", userId, e.getMessage(), e);
            session.setAttribute("error", "We could not load your cart. Please try again.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        if (cartItems == null || cartItems.isEmpty()) {
            session.setAttribute("error", "Your cart is empty.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getPrice() * item.getQuantity();
        }

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            // Step 1: Atomically reduce stock for each cart item.
            for (CartItem c : cartItems) {
                boolean stockReduced = reduceStockInTransaction(con, c);
                if (!stockReduced) {
                    con.rollback();
                    request.setAttribute("error", "Insufficient stock for " + c.getProductName() + " (Size: " + c.getSizeLabel() + "). Please update your cart.");
                    request.setAttribute("cartItems", cartItems);
                    request.setAttribute("cartTotal", total);
                    
                    request.setAttribute("fullName", request.getParameter("fullName"));
                    request.setAttribute("address", request.getParameter("address"));
                    request.setAttribute("city", request.getParameter("city"));
                    request.setAttribute("state", request.getParameter("state"));
                    request.setAttribute("zip", request.getParameter("zip"));
                    request.setAttribute("phone", request.getParameter("phone"));
                    
                    String csrfToken = (String) session.getAttribute("csrf_token");
                    request.setAttribute("csrfToken", csrfToken);
                    
                    request.getRequestDispatcher("/WEB-INF/views/checkout.jsp").forward(request, response);
                    return;
                }
            }

            // Step 2: Create order header.
            int orderId = createOrderInTransaction(con, userId, total, request);
            if (orderId <= 0) {
                con.rollback();
                request.setAttribute("error", "Could not place order. Please check your shipping details.");
                request.setAttribute("cartItems", cartItems);
                request.setAttribute("cartTotal", total);
                
                request.setAttribute("fullName", request.getParameter("fullName"));
                request.setAttribute("address", request.getParameter("address"));
                request.setAttribute("city", request.getParameter("city"));
                request.setAttribute("state", request.getParameter("state"));
                request.setAttribute("zip", request.getParameter("zip"));
                request.setAttribute("phone", request.getParameter("phone"));
                
                String csrfToken = (String) session.getAttribute("csrf_token");
                request.setAttribute("csrfToken", csrfToken);
                
                request.getRequestDispatcher("/WEB-INF/views/checkout.jsp").forward(request, response);
                return;
            }

            // Step 3: Insert order items.
            for (CartItem c : cartItems) {
                addOrderItemInTransaction(con, orderId, c);
            }

            // Step 4: Clear cart.
            clearCartInTransaction(con, userId);

            con.commit();
            logger.info("Order #{} placed successfully for user #{}", orderId, userId);
            
            // Clean up session cart cache if it exists
            session.removeAttribute("cartItems");
            
            response.sendRedirect(request.getContextPath() + "/success");

        } catch (Exception txEx) {
            logger.error("Checkout process failed for user #{}: {}", userId, txEx.getMessage(), txEx);
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {}
            }
            
            // Phase 7: preserve checkout state, preserve cart items, show inline error
            request.setAttribute("error", "Could not place order: " + txEx.getMessage());
            request.setAttribute("cartItems", cartItems);
            request.setAttribute("cartTotal", total);
            
            // Preserve form inputs
            request.setAttribute("fullName", request.getParameter("fullName"));
            request.setAttribute("address", request.getParameter("address"));
            request.setAttribute("city", request.getParameter("city"));
            request.setAttribute("state", request.getParameter("state"));
            request.setAttribute("zip", request.getParameter("zip"));
            request.setAttribute("phone", request.getParameter("phone"));
            
            String csrfToken = (String) session.getAttribute("csrf_token");
            request.setAttribute("csrfToken", csrfToken);
            
            request.getRequestDispatcher("/WEB-INF/views/checkout.jsp").forward(request, response);
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private boolean reduceStockInTransaction(Connection con, CartItem item) throws SQLException {
        String sql = "UPDATE product_sizes " +
                     "SET stock_quantity = stock_quantity - ?, " +
                     "    is_available = CASE WHEN (stock_quantity - ?) > 0 THEN 1 ELSE 0 END " +
                     "WHERE product_id = ? AND size_label = ? AND stock_quantity >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, item.getQuantity());
            ps.setInt(2, item.getQuantity());
            ps.setInt(3, item.getProductId());
            ps.setString(4, item.getSizeLabel());
            ps.setInt(5, item.getQuantity());
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    private int createOrderInTransaction(Connection con, int userId, double total, HttpServletRequest request) throws SQLException {
        // Populate both subtotal and total_amount so the orders row matches the
        // financial truth shown on the checkout page. Shipping/tax/discount keep
        // their schema defaults of 0.00 until those features are wired in.
        String sql = "INSERT INTO orders " +
                     "(user_id, subtotal, total_amount, full_name, address, city, state, zip, phone, payment_method, status, payment_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String fullName = request.getParameter("fullName");
        String address = request.getParameter("address");
        String city = request.getParameter("city");
        String state = request.getParameter("state");
        String zip = request.getParameter("zip");
        String phone = request.getParameter("phone");
        String paymentMethod = request.getParameter("paymentMethod");

        if (fullName == null || address == null || city == null || state == null || zip == null || phone == null) {
            logger.warn("Missing shipping details for user #{}", userId);
            return 0;
        }

        java.math.BigDecimal totalDec = java.math.BigDecimal.valueOf(total)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setBigDecimal(2, totalDec);
            ps.setBigDecimal(3, totalDec);
            ps.setString(4, fullName);
            ps.setString(5, address);
            ps.setString(6, city);
            ps.setString(7, state);
            ps.setString(8, zip);
            ps.setString(9, phone);
            ps.setString(10, paymentMethod != null ? paymentMethod : "COD");
            ps.setString(11, "Pending");
            ps.setString(12, "pending");
            
            int rows = ps.executeUpdate();
            if (rows == 0) return 0;
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private void addOrderItemInTransaction(Connection con, int orderId, CartItem item) throws SQLException {
        // The order_items schema declares three NOT NULL financial columns:
        //   price        (legacy unit-price snapshot, kept for back-compat)
        //   unit_price   (unit-price snapshot at time of purchase)
        //   total_price  (unit_price * quantity, for fast reporting)
        // All three must be populated explicitly using BigDecimal to preserve
        // currency precision; otherwise MySQL rejects the row with
        // "Field 'unit_price' doesn't have a default value".
        String sql = "INSERT INTO order_items " +
                     "(order_id, product_id, size_label, quantity, price, unit_price, total_price) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        java.math.BigDecimal unitPrice = java.math.BigDecimal.valueOf(item.getPrice())
                .setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal totalPrice = unitPrice.multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, item.getProductId());
            ps.setString(3, item.getSizeLabel());
            ps.setInt(4, item.getQuantity());
            ps.setBigDecimal(5, unitPrice);
            ps.setBigDecimal(6, unitPrice);
            ps.setBigDecimal(7, totalPrice);
            ps.executeUpdate();
        }
    }

    private void clearCartInTransaction(Connection con, int userId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
