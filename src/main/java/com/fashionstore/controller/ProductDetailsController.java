package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.ProductDAO;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.dao.ReviewDAO;
import com.fashionstore.daoimpl.ReviewDAOImpl;
import com.fashionstore.model.Product;
import com.fashionstore.model.Review;
import com.fashionstore.service.RecommendationService;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/product")
public class ProductDetailsController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ProductDetailsController.class);

    private ProductDAO productDAO;
    private ReviewDAO reviewDAO;
    private RecommendationService recommendationService;

    @Override
    public void init() {
        productDAO = new ProductDAOImpl();
        reviewDAO = new ReviewDAOImpl();
        recommendationService = new RecommendationService();
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

            Product product = productDAO.getProductById(productId);
            if (product == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            request.setAttribute("product", product);

            // Fetch Reviews
            List<Review> reviews = reviewDAO.getReviewsByProductId(productId);
            double avgRating = reviewDAO.getAverageRating(productId);
            int reviewCount = reviewDAO.getReviewCount(productId);

            request.setAttribute("reviews", reviews);
            request.setAttribute("avgRating", avgRating);
            request.setAttribute("reviewCount", reviewCount);
            request.setAttribute("relatedProducts", recommendationService.getRelatedProducts(productId, 4));

            request.getRequestDispatcher("/WEB-INF/views/product-details.jsp")
                    .forward(request, response);

        } catch (Exception e) {
            logger.error("Error in ProductDetailsController.doGet: {}", e.getMessage(), e);
            response.sendRedirect(request.getContextPath() + "/products");
        }
    }
}
