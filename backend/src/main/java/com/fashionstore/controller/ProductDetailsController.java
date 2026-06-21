package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductReview;
import com.fashionstore.service.ProductService;
import com.fashionstore.service.ProductReviewService;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/product")
public class ProductDetailsController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ProductDetailsController.class);

    private ProductService productService;
    private ProductReviewService productReviewService;

    @Override
    public void init() {
        ServiceRegistry registry = ServiceRegistry.getInstance();
        productService = registry.getProductService();
        productReviewService = registry.getProductReviewService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String idStr = request.getParameter("id");
            if (idStr == null || idStr.isBlank()) {
                response.sendRedirect(request.getContextPath() + "/products");
                return;
            }

            int productId;
            try {
                productId = Integer.parseInt(idStr);
            } catch (NumberFormatException nfe) {
                response.sendRedirect(request.getContextPath() + "/products");
                return;
            }

            Product product = productService.getProductById(productId);
            if (product == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Track recently viewed products in session
            jakarta.servlet.http.HttpSession session = request.getSession();
            @SuppressWarnings("unchecked")
            java.util.List<Integer> recentlyViewed = (java.util.List<Integer>) session.getAttribute("recentlyViewed");
            if (recentlyViewed == null) {
                recentlyViewed = new java.util.ArrayList<>();
            }
            // Remove if already exists and add to front
            recentlyViewed.remove(Integer.valueOf(productId));
            recentlyViewed.add(0, productId);
            // Keep only last 20 viewed products. Wrap in a fresh ArrayList because
            // ArrayList#subList returns a non-Serializable view that breaks session
            // replication and aliases the parent list.
            if (recentlyViewed.size() > 20) {
                recentlyViewed = new java.util.ArrayList<>(recentlyViewed.subList(0, 20));
            }
            session.setAttribute("recentlyViewed", recentlyViewed);

            request.setAttribute("product", product);

            // Fetch Reviews
            List<ProductReview> reviews = productReviewService.getReviewsByProductId(productId);
            double avgRating = productReviewService.getAverageRating(productId);
            int reviewCount = reviews != null ? reviews.size() : 0;

            request.setAttribute("reviews", reviews);
            request.setAttribute("avgRating", avgRating);
            request.setAttribute("reviewCount", reviewCount);

            request.getRequestDispatcher("/WEB-INF/views/product-details.jsp")
                    .forward(request, response);

        } catch (Exception e) {
            logger.error("Error in ProductDetailsController.doGet: {}", e.getMessage(), e);
            // Set error attributes and forward to error page instead of redirect
            request.setAttribute("errorTitle", "Product Not Available");
            request.setAttribute("errorMessage", "We're unable to load this product right now. Please try again later.");
            request.setAttribute("errorDetails", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/errors/500.jsp").forward(request, response);
        }
    }
}
