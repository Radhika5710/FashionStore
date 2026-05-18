package com.fashionstore.service;

import com.fashionstore.dao.ProductDAO;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple search service - delegates to ProductDAO
 * No fake enterprise features, autocomplete, fuzzy search, or analytics
 */
public class SearchService {
    
    private static final Logger LOGGER = Logger.getLogger(SearchService.class.getName());
    private ProductDAO productDAO;
    
    public SearchService() {
        this.productDAO = new ProductDAOImpl();
    }
    
    /**
     * Search products by keyword
     */
    public List<Product> search(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            ProductQuery productQuery = new ProductQuery();
            productQuery.setSearch(query.toLowerCase().trim());
            productQuery.setOffset(0);
            productQuery.setLimit(limit > 0 ? limit : 20);
            productQuery.setActiveOnly(true);
            return productDAO.getProducts(productQuery);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in search", e);
            return new ArrayList<>();
        }
    }
}
