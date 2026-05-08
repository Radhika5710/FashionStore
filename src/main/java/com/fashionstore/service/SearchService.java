package com.fashionstore.service;

import com.fashionstore.dao.ProductDAO;
import com.fashionstore.dao.CategoryDAO;
import com.fashionstore.daoimpl.CategoryDAOImpl;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.domain.CategoryType;
import com.fashionstore.model.Category;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Intelligent search service with autocomplete, typo tolerance, and suggestions
 */
public class SearchService {
    
    private static final Logger LOGGER = Logger.getLogger(SearchService.class.getName());
    private ProductDAO productDAO;
    private CategoryDAO categoryDAO;
    
    public SearchService() {
        this.productDAO = new ProductDAOImpl();
        this.categoryDAO = new CategoryDAOImpl();
    }
    
    /**
     * Autocomplete search with suggestions
     */
    public List<Product> autocomplete(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalizedQuery = query.toLowerCase().trim();
        
        try {
            ProductQuery productQuery = new ProductQuery();
            Integer categoryId = resolveCategoryId(normalizedQuery);
            productQuery.setCategoryId(categoryId);
            productQuery.setSearch(categoryId == null ? normalizedQuery : null);
            productQuery.setOffset(0);
            productQuery.setLimit(limit);
            productQuery.setActiveOnly(true);
            return productDAO.getProducts(productQuery);
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in autocomplete search", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Keyword suggestions based on product names
     */
    public List<String> getKeywordSuggestions(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String normalizedQuery = query.toLowerCase().trim();

            ProductQuery productQuery = new ProductQuery();
            Integer categoryId = resolveCategoryId(normalizedQuery);
            productQuery.setCategoryId(categoryId);
            productQuery.setSearch(categoryId == null ? normalizedQuery : null);
            productQuery.setOffset(0);
            productQuery.setLimit(Math.max(limit * 2, limit));
            productQuery.setActiveOnly(true);

            return productDAO.getProducts(productQuery).stream()
                .map(Product::getProductName)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting keyword suggestions", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Category suggestions based on search query
     */
    public List<String> getCategorySuggestions(String query) {
        List<String> categories = new ArrayList<>();
        String normalizedQuery = query != null ? query.toLowerCase().trim() : "";
        
        if (normalizedQuery.isEmpty()) {
            return categories;
        }

        try {
            List<Category> active = categoryDAO.getActiveCategories();
            if (active == null) {
                return categories;
            }

            for (Category c : active) {
                String name = c.getCategoryName();
                if (name != null && (name.toLowerCase().contains(normalizedQuery)
                        || CategoryType.fromName(normalizedQuery).map(type -> type.getDisplayName().equals(name)).orElse(false))) {
                    categories.add(name);
                }
            }
        } catch (Exception e) {
            return categories;
        }

        return categories;
    }
    
    /**
     * Typo-tolerant search using Levenshtein distance
     */
    public List<Product> fuzzySearch(String query, int maxDistance, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String normalizedQuery = query.toLowerCase().trim();

            ProductQuery productQuery = new ProductQuery();
            Integer categoryId = resolveCategoryId(normalizedQuery);
            productQuery.setCategoryId(categoryId);
            productQuery.setSearch(categoryId == null ? normalizedQuery : null);
            productQuery.setOffset(0);
            productQuery.setLimit(Math.max(limit * 3, limit));
            productQuery.setActiveOnly(true);

            return productDAO.getProducts(productQuery).stream()
                .filter(p -> levenshteinDistance(p.getProductName().toLowerCase(), normalizedQuery) <= maxDistance)
                .limit(limit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in fuzzy search", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Advanced search with filters
     */
    public List<Product> advancedSearch(String query, String category, String color, String material, 
                                      String season, String occasion, double minPrice, double maxPrice, 
                                      String sortBy, int limit) {
        try {
            ProductQuery productQuery = new ProductQuery();
            productQuery.setSearch(query);
            productQuery.setCategoryId(resolveCategoryId(category));
            productQuery.setMinPrice(minPrice > 0 ? (int) Math.floor(minPrice) : null);
            productQuery.setMaxPrice(maxPrice > 0 ? (int) Math.ceil(maxPrice) : null);
            productQuery.setSortBy(sortBy);
            productQuery.setOffset(0);
            productQuery.setLimit(limit <= 0 ? 8 : limit);
            productQuery.setActiveOnly(true);

            int effectiveLimit = limit <= 0 ? 8 : limit;

            return productDAO.getProducts(productQuery).stream()
                .filter(p -> color == null || color.isEmpty() || matchesColor(p, color))
                .filter(p -> material == null || material.isEmpty() || matchesMaterial(p, material))
                .filter(p -> season == null || season.isEmpty() || matchesSeason(p, season))
                .filter(p -> occasion == null || occasion.isEmpty() || matchesOccasion(p, occasion))
                .limit(effectiveLimit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in advanced search", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if product matches search query
     */
    private boolean matchesSearch(Product product, String query) {
        String name = product.getProductName().toLowerCase();
        String description = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
        String brand = product.getBrand() != null ? product.getBrand().toLowerCase() : "";
        
        return name.contains(query) || 
               description.contains(query) || 
               brand.contains(query);
    }
    
    /**
     * Check if product matches category
     */
    private boolean matchesCategory(Product product, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        String trimmed = category.trim();
        try {
            int id = Integer.parseInt(trimmed);
            return product.getCategoryId() == id;
        } catch (NumberFormatException ignored) {
        }

        Integer resolvedId = resolveCategoryId(trimmed);
        if (resolvedId != null) {
            return product.getCategoryId() == resolvedId;
        }

        return false;
    }

    private Integer resolveCategoryId(String categoryName) {
        try {
            List<Category> active = categoryDAO.getActiveCategories();
            if (active == null) {
                return null;
            }

            String key = CategoryType.fromName(categoryName).map(CategoryType::getSlug).orElse(CategoryType.normalize(categoryName));
            for (Category c : active) {
                String categoryKey = c.getCategorySlug() != null ? c.getCategorySlug() : c.getCategoryName();
                if (CategoryType.normalize(categoryKey).equals(CategoryType.normalize(key))) {
                    return c.getCategoryId();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if product matches color (simplified - in production use proper color mapping)
     */
    private boolean matchesColor(Product product, String color) {
        String description = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
        String name = product.getProductName().toLowerCase();
        return description.contains(color.toLowerCase()) || name.contains(color.toLowerCase());
    }
    
    /**
     * Check if product matches material
     */
    private boolean matchesMaterial(Product product, String material) {
        String description = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
        return description.contains(material.toLowerCase());
    }
    
    /**
     * Check if product matches season
     */
    private boolean matchesSeason(Product product, String season) {
        String description = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
        String name = product.getProductName().toLowerCase();
        return description.contains(season.toLowerCase()) || name.contains(season.toLowerCase());
    }
    
    /**
     * Check if product matches occasion
     */
    private boolean matchesOccasion(Product product, String occasion) {
        String description = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
        return description.contains(occasion.toLowerCase());
    }
    
    /**
     * Sort products based on criteria
     */
    private int sortProducts(Product p1, Product p2, String sortBy) {
        switch (sortBy) {
            case "price_asc":
                return Double.compare(p1.getPrice(), p2.getPrice());
            case "price_desc":
                return Double.compare(p2.getPrice(), p1.getPrice());
            case "newest":
                return p2.getProductId() - p1.getProductId();
            case "popular":
                return Integer.compare(p2.getStockQuantity(), p1.getStockQuantity()); // Using stock as proxy
            default:
                return 0;
        }
    }
    
    /**
     * Calculate Levenshtein distance for typo tolerance
     */
    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[len1][len2];
    }
}
