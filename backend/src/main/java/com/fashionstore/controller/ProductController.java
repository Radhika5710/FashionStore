package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductQuery;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/products")
public class ProductController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private static final int PAGE_SIZE = 12;

    private ProductService productService;

    @Override
    public void init() {
        productService = ServiceRegistry.getInstance().getProductService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // Extract basic query parameters
            String search = request.getParameter("search");
            String categoryIdStr = request.getParameter("categoryId");
            String sortBy = request.getParameter("sortBy");
            String pageStr = request.getParameter("page");

            // Parse pagination
            int page = parseIntOrNull(pageStr) != null ? parseIntOrNull(pageStr) : 1;
            page = Math.max(1, page);
            int offset = (page - 1) * PAGE_SIZE;

            // Build query
            ProductQuery query = new ProductQuery();
            query.setSearch(search);
            if (categoryIdStr != null && !categoryIdStr.isBlank()) {
                try {
                    query.setCategoryId(Integer.parseInt(categoryIdStr));
                } catch (NumberFormatException ignored) {
                }
            }
            query.setSortBy(sortBy != null ? sortBy : "newest");
            query.setOffset(offset);
            query.setLimit(PAGE_SIZE);
            query.setActiveOnly(true);

            // Get products and count
            List<Product> products = productService.getProducts(query);
            int totalCount = productService.countProducts(query);
            int totalPages = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE;

            // Set request attributes
            request.setAttribute("products", products != null ? products : new ArrayList<>());
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("search", search);
            request.setAttribute("categoryId", categoryIdStr);
            request.setAttribute("sortBy", sortBy);

            request.getRequestDispatcher("/WEB-INF/views/products.jsp")
                    .forward(request, response);

        } catch (Exception e) {
            logger.error("Error in ProductController.doGet: {}", e.getMessage(), e);
            request.setAttribute("products", new ArrayList<>());
            request.setAttribute("currentPage", 1);
            request.setAttribute("totalPages", 1);
            request.setAttribute("errorMessage", "Error loading products. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/products.jsp")
                    .forward(request, response);
        }
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
