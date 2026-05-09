package com.fashionstore.controller;

import com.fashionstore.model.Category;
import com.fashionstore.model.Product;
import com.fashionstore.service.CategoryService;
import com.fashionstore.service.ProductService;
import com.fashionstore.util.DBConnection;
import com.fashionstore.util.NullSafetyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(HomeServlet.class);

    private ProductService productService;
    private CategoryService categoryService;

    @Override
    public void init() {
        productService = new ProductService();
        categoryService = new CategoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // Check database health before proceeding
            if (!DBConnection.isHealthy()) {
                logger.error("Database connection is not healthy, cannot load home page");
                request.setAttribute("error", "Database connection error. Please try again later.");
                request.setAttribute("products", Collections.emptyList());
                request.setAttribute("categories", Collections.emptyList());
                request.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);
                return;
            }

            // Load featured products with null safety
            List<Product> products = NullSafetyUtil.safeGet(() -> {
                List<Product> result = productService.getFeaturedProducts(8);
                return result != null ? result : Collections.emptyList();
            }, Collections.emptyList());

            if (products.isEmpty()) {
                logger.warn("Featured products list is empty or null");
            }

            // Load active categories with null safety
            List<Category> categories = NullSafetyUtil.safeGet(() -> {
                List<Category> result = categoryService.getActiveCategories();
                return result != null ? result : Collections.emptyList();
            }, Collections.emptyList());

            if (categories.isEmpty()) {
                logger.warn("Categories list is empty or null");
            }

            request.setAttribute("products", products);
            request.setAttribute("categories", categories);

            request.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);

        } catch (Exception e) {
            logger.error("Unexpected error in HomeServlet.doGet: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while loading the home page");
        }
    }
}
