package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.OrderDAO;
import com.fashionstore.dao.ProductDAO;
import com.fashionstore.dao.UserDAO;
import com.fashionstore.daoimpl.OrderDAOImpl;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.daoimpl.UserDAOImpl;
import com.fashionstore.model.Order;
import com.fashionstore.util.SecurityUtil;
import com.fashionstore.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/dashboard")
public class AdminDashboardController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    private OrderDAO orderDAO;
    private ProductDAO productDAO;
    private UserDAO userDAO;

    @Override
    public void init() {
        orderDAO = new OrderDAOImpl();
        productDAO = new ProductDAOImpl();
        userDAO = new UserDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Admin authorization check
        if (!SecurityUtil.requireAdmin(request, response)) {
            return;
        }

        try {
            // Validate admin session
            if (request.getSession(false) == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            
            // Get dashboard statistics with null checks
            int totalOrders = orderDAO.getTotalOrderCount();
            int totalUsers = userDAO.getTotalUserCount();
            int totalProducts = productDAO.countProducts(null, null, null);
            
            // Validate statistics
            totalOrders = Math.max(0, totalOrders);
            totalUsers = Math.max(0, totalUsers);
            totalProducts = Math.max(0, totalProducts);
            
            // Get recent orders with error handling
            List<Order> recentOrders;
            try {
                recentOrders = orderDAO.getRecentOrders(10);
                if (recentOrders == null) {
                    recentOrders = new ArrayList<>();
                }
            } catch (Exception e) {
                logger.warn("Failed to load recent orders: {}", e.getMessage());
                recentOrders = new ArrayList<>();
            }
            
            // Calculate total revenue with validation
            double totalRevenue = 0;
            try {
                totalRevenue = orderDAO.getTotalRevenue();
                if (totalRevenue < 0) {
                    totalRevenue = 0;
                }
            } catch (Exception e) {
                logger.warn("Failed to calculate total revenue: {}", e.getMessage());
                totalRevenue = 0;
            }
            
            request.setAttribute("totalOrders", totalOrders);
            request.setAttribute("totalUsers", totalUsers);
            request.setAttribute("totalProducts", totalProducts);
            request.setAttribute("recentOrders", recentOrders);
            request.setAttribute("totalRevenue", totalRevenue);
            
            request.getRequestDispatcher("/WEB-INF/views/admin-dashboard.jsp")
                   .forward(request, response);
                   
        } catch (Exception e) {
            logger.error("Unexpected error in AdminDashboardController: {}", e.getMessage(), e);
            request.setAttribute("errorTitle", "System Error");
            request.setAttribute("errorMessage", "An unexpected error occurred while loading the dashboard.");
            request.setAttribute("errorDetails", "Please try again later.");
            request.getRequestDispatcher("/WEB-INF/views/admin/error.jsp").forward(request, response);
        }
    }

    private Map<String, Object> getRevenueChartData(int days) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        List<Double> data = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Get revenue data from orders
        List<Order> orders = orderDAO.getOrdersInLastDays(days);
        
        // Group by date
        Map<String, Double> dailyRevenue = new HashMap<>();
        for (Order order : orders) {
            String dateKey = new java.text.SimpleDateFormat("MMM dd").format(order.getOrderDate());
            dailyRevenue.put(dateKey, dailyRevenue.getOrDefault(dateKey, 0.0) + order.getTotalAmount());
        }

        // Fill missing dates with 0
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd");
        for (int i = days - 1; i >= 0; i--) {
            java.util.Date date = new java.util.Date(System.currentTimeMillis() - (i * 24L * 60 * 60 * 1000));
            String dateKey = sdf.format(date);
            labels.add(dateKey);
            data.add(dailyRevenue.getOrDefault(dateKey, 0.0));
        }

        result.put("data", data);
        result.put("labels", labels);
        return result;
    }
}
