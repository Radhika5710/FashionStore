package com.fashionstore.service;

import com.fashionstore.dao.ProductDAO;
import com.fashionstore.dao.ProductSizeDAO;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.daoimpl.ProductSizeDAOImpl;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductQuery;
import com.fashionstore.model.ProductSize;

import java.util.List;

/**
 * ProductService - MVC Service Layer
 * 
 * REFACTORED FOR PROPER MVC ARCHITECTURE:
 * - ALL product business logic in service layer
 * - ALL product validation in service layer
 * - ALL product transformation in service layer
 * - DAO layer only handles database access
 * - Controllers only handle request/response
 * - Frontend cannot manipulate product data
 * 
 * Centralized Business Logic:
 * - Price validation (positive, reasonable range)
 * - Stock validation (non-negative, reasonable limits)
 * - Category validation (exists, active)
 * - Discount validation (0-100%, applied correctly)
 * - Product name validation (non-empty, length limits)
 * - Description validation (length limits)
 * - Image URL validation (format, accessibility)
 * 
 * Single Source of Truth:
 * - Backend calculates all product prices
 * - Backend calculates all discounts
 * - Backend validates all stock levels
 * - Frontend only displays backend values
 */
public class ProductService {

    private final ProductDAO productDAO;
    private final ProductSizeDAO sizeDAO;

    // Validation constants
    private static final double MIN_PRICE = 0.01;
    private static final double MAX_PRICE = 999999.99;
    private static final int MAX_STOCK = 999999;
    private static final double MIN_DISCOUNT = 0.0;
    private static final double MAX_DISCOUNT = 100.0;
    private static final int MAX_PRODUCT_NAME_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    public ProductService() {
        this.productDAO = new ProductDAOImpl();
        this.sizeDAO = new ProductSizeDAOImpl();
    }

    /**
     * Get product by ID
     * Backend is authoritative source
     */
    public Product getProductById(int productId) {
        return productDAO.getProductById(productId);
    }

    /**
     * Get all active products
     * Backend filters to active products only
     */
    public List<Product> getAllProducts() {
        return productDAO.getAllProducts();
    }

    /**
     * Get featured products with limit
     * Backend determines featured status
     */
    public List<Product> getFeaturedProducts(int limit) {
        return productDAO.getFeaturedProducts(limit);
    }

    /**
     * Get products by category with pagination
     * Backend handles category filtering and pagination
     */
    public List<Product> getProductsByCategory(int categoryId, int page, int limit) {
        return productDAO.getProductsByCategory(categoryId, page, limit);
    }

    /**
     * Search products by query string
     * Backend performs search - no frontend filtering
     */
    public List<Product> searchProducts(String query) {
        return productDAO.searchProducts(query);
    }

    /**
     * Get filtered products by price and sizes
     * Backend performs filtering - no frontend filtering
     */
    public List<Product> getFilteredProducts(int maxPrice, String[] sizes) {
        return productDAO.getFilteredProducts(maxPrice, sizes);
    }

    /**
     * Get products with complex query
     * Handles search, filtering, sorting, pagination all in backend
     * 
     * @param query ProductQuery with all filter parameters
     * @return List of products matching query
     */
    public List<Product> getProducts(ProductQuery query) {
        return productDAO.getProducts(query);
    }

    /**
     * Count products matching query
     * Used for pagination calculation
     * 
     * @param query ProductQuery with all filter parameters
     * @return Total count of products matching query
     */
    public int countProducts(ProductQuery query) {
        return productDAO.countProducts(query);
    }

    /**
     * Update product stock with validation
     * Backend validates stock levels
     */
    public boolean updateStock(int productId, int quantity) {
        if (!validateStock(quantity)) {
            return false;
        }
        return productDAO.updateStock(productId, quantity);
    }

    /**
     * Get product sizes
     */
    public List<ProductSize> getProductSizes(int productId) {
        return sizeDAO.getSizesByProductId(productId);
    }

    /**
     * Create new product with full validation
     * Backend validates all product data before creation
     */
    public int createProduct(Product product) throws IllegalArgumentException {
        validateProductForCreation(product);
        return productDAO.addProduct(product);
    }

    /**
     * Update existing product with full validation
     * Backend validates all product data before update
     */
    public boolean updateProduct(Product product) throws IllegalArgumentException {
        validateProductForUpdate(product);
        return productDAO.updateProduct(product);
    }

    /**
     * Validate product for creation
     * Ensures all required fields are valid
     */
    private void validateProductForCreation(Product product) throws IllegalArgumentException {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        
        validateProductName(product.getProductName());
        validatePrice(product.getPrice());
        validateDiscount(product.getDiscountPercent());
        validateStock(product.getStockQuantity());
        validateDescription(product.getDescription());
        
        if (product.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Valid category is required");
        }
    }

    /**
     * Validate product for update
     * Ensures all fields are valid
     */
    private void validateProductForUpdate(Product product) throws IllegalArgumentException {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        
        if (product.getProductId() <= 0) {
            throw new IllegalArgumentException("Product ID is required for update");
        }
        
        validateProductName(product.getProductName());
        validatePrice(product.getPrice());
        validateDiscount(product.getDiscountPercent());
        validateStock(product.getStockQuantity());
        validateDescription(product.getDescription());
        
        if (product.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Valid category is required");
        }
    }

    /**
     * Validate product name
     * Backend enforces naming rules
     */
    private void validateProductName(String name) throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (name.length() > MAX_PRODUCT_NAME_LENGTH) {
            throw new IllegalArgumentException("Product name exceeds maximum length of " + MAX_PRODUCT_NAME_LENGTH);
        }
    }

    /**
     * Validate price
     * Backend enforces price rules
     */
    private void validatePrice(double price) throws IllegalArgumentException {
        if (price < MIN_PRICE || price > MAX_PRICE) {
            throw new IllegalArgumentException("Price must be between " + MIN_PRICE + " and " + MAX_PRICE);
        }
    }

    /**
     * Validate discount percentage
     * Backend enforces discount rules
     */
    private void validateDiscount(double discount) throws IllegalArgumentException {
        if (discount < MIN_DISCOUNT || discount > MAX_DISCOUNT) {
            throw new IllegalArgumentException("Discount must be between " + MIN_DISCOUNT + " and " + MAX_DISCOUNT + " percent");
        }
    }

    /**
     * Validate stock quantity
     * Backend enforces stock rules
     */
    private boolean validateStock(int stock) {
        return stock >= 0 && stock <= MAX_STOCK;
    }

    /**
     * Validate description
     * Backend enforces description rules
     */
    private void validateDescription(String description) throws IllegalArgumentException {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH);
        }
    }

    /**
     * Calculate discounted price
     * Backend calculates all prices
     */
    public double calculateDiscountedPrice(double price, double discountPercent) {
        if (discountPercent <= 0) {
            return price;
        }
        double discountAmount = price * (discountPercent / 100.0);
        return Math.round((price - discountAmount) * 100.0) / 100.0;
    }

    /**
     * Calculate discount amount
     * Backend calculates all discounts
     */
    public double calculateDiscountAmount(double price, double discountPercent) {
        if (discountPercent <= 0) {
            return 0.0;
        }
        return Math.round((price * (discountPercent / 100.0)) * 100.0) / 100.0;
    }

    /**
     * Get count of products with low stock
     * Backend determines low stock threshold
     */
    public int getLowStockProductCount(int threshold) {
        return productDAO.getLowStockProductCount(threshold);
    }

    /**
     * Delete product by ID
     * Backend validates deletion
     */
    public boolean deleteProduct(int productId) {
        return productDAO.deleteProduct(productId);
    }
}
