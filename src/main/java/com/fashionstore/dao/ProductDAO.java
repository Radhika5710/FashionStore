package com.fashionstore.dao;

import com.fashionstore.model.Product;
import com.fashionstore.model.ProductQuery;
import java.util.List;

public interface ProductDAO {

    // Basic CRUD operations
    List<Product> getAllProducts();
    Product getProductById(int productId);
    List<Product> getFilteredProducts(int maxPrice, String[] sizes);
    List<Product> searchProducts(String query);
    List<Product> getProducts(String search, Integer maxPrice, String[] sizes, String sortBy, int offset, int limit);
    List<Product> getProducts(String search, Integer maxPrice, String[] sizes, String brand, String sortBy, int offset, int limit);
    int countProducts(String search, Integer maxPrice, String[] sizes);
    int countProducts(String search, Integer maxPrice, String[] sizes, String brand);
    List<Product> getProducts(ProductQuery query);
    int countProducts(ProductQuery query);
    int addProduct(Product product);
    boolean updateProduct(Product product);
    boolean deleteProduct(int productId);

    // Enhanced operations for optimized implementation
    List<Product> getAllProducts(int page, int limit, String search, String categoryId);
    List<Product> getProductsByCategory(int categoryId, int page, int limit);
    List<Product> getFeaturedProducts(int limit);
    List<Product> getTopSellingProducts(int limit);
    List<Product> searchProducts(String query, int page, int limit);
    boolean updateStock(int productId, int quantity);
    int getProductCount(String search, String categoryId);
    int getLowStockProductCount(int threshold);
}
