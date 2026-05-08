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
import jakarta.servlet.http.*;

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
        List<CartItem> cartItems = cartDAO.getCartItemsByUserId(userId);

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
                    session.setAttribute("error", "Insufficient stock for " + c.getProductName() + 
                        " (Size: " + c.getSizeLabel() + "). Please update your cart.");
                    response.sendRedirect(request.getContextPath() + "/cart");
                    return;
                }
            }

            // Step 2: Create order header.
            int orderId = createOrderInTransaction(con, userId, total, request);
            if (orderId <= 0) {
                con.rollback();
                session.setAttribute("error", "Could not place order. Please check your shipping details.");
                response.sendRedirect(request.getContextPath() + "/checkout");
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
            session.setAttribute("error", "An unexpected error occurred while placing your order: " + txEx.getMessage());
            response.sendRedirect(request.getContextPath() + "/cart");
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
        String sql = "INSERT INTO orders (user_id, total_amount, full_name, address, city, state, zip, phone, payment_method, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
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

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDouble(2, total);
            ps.setString(3, fullName);
            ps.setString(4, address);
            ps.setString(5, city);
            ps.setString(6, state);
            ps.setString(7, zip);
            ps.setString(8, phone);
            ps.setString(9, paymentMethod != null ? paymentMethod : "COD");
            ps.setString(10, "Pending");
            
            int rows = ps.executeUpdate();
            if (rows == 0) return 0;
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private void addOrderItemInTransaction(Connection con, int orderId, CartItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, size_label, quantity, price) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, item.getProductId());
            ps.setString(3, item.getSizeLabel());
            ps.setInt(4, item.getQuantity());
            ps.setDouble(5, item.getPrice());
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
