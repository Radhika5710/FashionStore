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
            // Get analytics data
            double totalSales = orderDAO.getTotalRevenue();
            int totalUsers = userDAO.getTotalUserCount();
            int totalOrders = orderDAO.getTotalOrderCount();
            int lowStockCount = productDAO.getLowStockProductCount(10); // Products with stock < 10

            // Get recent orders
            List<Order> recentOrders = orderDAO.getRecentOrders(10);

            // Get revenue data for chart (last 30 days)
            Map<String, Object> revenueData = getRevenueChartData(30);

            request.setAttribute("totalSales", String.format("%.0f", totalSales));
            request.setAttribute("totalUsers", totalUsers);
            request.setAttribute("totalOrders", totalOrders);
            request.setAttribute("lowStockCount", lowStockCount);
            request.setAttribute("recentOrders", recentOrders);
            request.setAttribute("revenueData", revenueData.get("data"));
            request.setAttribute("revenueLabels", revenueData.get("labels"));

            request.getRequestDispatcher("/WEB-INF/views/admin-dashboard.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            logger.error("Error in AdminDashboardController.doGet: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error loading dashboard data");
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
