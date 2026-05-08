package com.fashionstore.service;

import com.fashionstore.dao.OrderDAO;
import com.fashionstore.dao.ProductDAO;
import com.fashionstore.daoimpl.OrderDAOImpl;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.model.Order;
import com.fashionstore.model.OrderItem;
import com.fashionstore.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Recommendation service for intelligent product suggestions
 */
public class RecommendationService {
    
    private static final Logger LOGGER = Logger.getLogger(RecommendationService.class.getName());
    private ProductDAO productDAO;
    private OrderDAO orderDAO;
    
    public RecommendationService() {
        this.productDAO = new ProductDAOImpl();
        this.orderDAO = new OrderDAOImpl();
    }
    
    /**
     * Get related products based on category and brand
     */
    public List<Product> getRelatedProducts(int productId, int limit) {
        try {
            Product currentProduct = productDAO.getProductById(productId);
            if (currentProduct == null) {
                return new ArrayList<>();
            }
            
            List<Product> allProducts = productDAO.getAllProducts();
            
            return allProducts.stream()
                .filter(p -> p.getProductId() != productId)
                .filter(p -> p.isActive())
                .filter(p -> isRelatedProduct(p, currentProduct))
                .limit(limit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting related products", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get products that customers also bought (collaborative filtering)
     */
    public List<Product> getCustomersAlsoBought(int productId, int limit) {
        try {
            // Find orders that contain this product
            List<Order> ordersWithProduct = orderDAO.getAllOrders();
            List<Integer> productIdsInSameOrders = new ArrayList<>();
            
            for (Order order : ordersWithProduct) {
                for (OrderItem item : order.getItems()) {
                    if (item.getProductId() == productId) {
                        // Add all other products from this order
                        for (OrderItem otherItem : order.getItems()) {
                            if (otherItem.getProductId() != productId) {
                                productIdsInSameOrders.add(otherItem.getProductId());
                            }
                        }
                    }
                }
            }
            
            // Count frequency and get most common
            Map<Integer, Integer> frequencyMap = new HashMap<>();
            for (int pid : productIdsInSameOrders) {
                frequencyMap.put(pid, frequencyMap.getOrDefault(pid, 0) + 1);
            }
            
            // Sort by frequency and return top products
            return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> productDAO.getProductById(entry.getKey()))
                .filter(Objects::nonNull)
                .filter(Product::isActive)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting customers also bought", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get trending products (based on sales or views)
     */
    public List<Product> getTrendingProducts(int limit) {
        try {
            List<Product> allProducts = productDAO.getAllProducts();
            
            return allProducts.stream()
                .filter(Product::isActive)
                .filter(Product::isTrending)
                .sorted(Comparator.comparing(Product::getProductId).reversed())
                .limit(limit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting trending products", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recently viewed products (from session or database)
     */
    public List<Product> getRecentlyViewed(List<Integer> recentlyViewedIds, int limit) {
        if (recentlyViewedIds == null || recentlyViewedIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // Reverse to get most recent first
            List<Integer> reversedIds = new ArrayList<>(recentlyViewedIds);
            Collections.reverse(reversedIds);
            
            return reversedIds.stream()
                .limit(limit)
                .map(productDAO::getProductById)
                .filter(Objects::nonNull)
                .filter(Product::isActive)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting recently viewed", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get personalized recommendations based on user history
     */
    public List<Product> getPersonalizedRecommendations(int userId, int limit) {
        try {
            // Get user's order history
            List<Order> userOrders = orderDAO.getOrdersByUserId(userId);
            Map<Integer, Integer> categoryPreferences = new HashMap<>();
            Map<String, Integer> brandPreferences = new HashMap<>();
            
            // Analyze purchase history
            for (Order order : userOrders) {
                for (OrderItem item : order.getItems()) {
                    Product product = productDAO.getProductById(item.getProductId());
                    if (product != null) {
                        categoryPreferences.put(product.getCategoryId(), categoryPreferences.getOrDefault(product.getCategoryId(), 0) + 1);
                        
                        // Count brand preferences
                        if (product.getBrand() != null) {
                            brandPreferences.put(product.getBrand(), brandPreferences.getOrDefault(product.getBrand(), 0) + 1);
                        }
                    }
                }
            }
            
            // Get all products and score them based on preferences
            List<Product> allProducts = productDAO.getAllProducts();
            Map<Product, Double> productScores = new HashMap<>();
            
            for (Product product : allProducts) {
                if (!product.isActive()) continue;
                
                double score = 0.0;
                
                // Category match score
                for (Map.Entry<Integer, Integer> entry : categoryPreferences.entrySet()) {
                    if (product.getCategoryId() == entry.getKey()) {
                        score += entry.getValue() * 2.0;
                    }
                }
                
                // Brand match score
                if (product.getBrand() != null && brandPreferences.containsKey(product.getBrand())) {
                    score += brandPreferences.get(product.getBrand()) * 3.0;
                }
                
                // Trending bonus
                if (product.isTrending()) {
                    score += 1.0;
                }
                
                // New product bonus
                if (product.isNew()) {
                    score += 0.5;
                }
                
                if (score > 0) {
                    productScores.put(product, score);
                }
            }
            
            // Return top scored products
            return productScores.entrySet().stream()
                .sorted(Map.Entry.<Product, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting personalized recommendations", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if two products are related (same brand or similar category)
     */
    private boolean isRelatedProduct(Product p1, Product p2) {
        // Same brand
        if (p1.getBrand() != null && p2.getBrand() != null && 
            p1.getBrand().equalsIgnoreCase(p2.getBrand())) {
            return true;
        }
        
        // Similar price range (within 20%)
        double avgPrice = (p1.getPrice() + p2.getPrice()) / 2;
        double priceDiff = Math.abs(p1.getPrice() - p2.getPrice());
        if (priceDiff / avgPrice < 0.2) {
            return true;
        }
        
        if (p1.getCategoryId() > 0 && p1.getCategoryId() == p2.getCategoryId()) {
            return true;
        }
        
        return false;
    }
}
