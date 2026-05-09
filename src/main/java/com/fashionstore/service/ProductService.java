package com.fashionstore.service;

import com.fashionstore.dao.ProductDAO;
import com.fashionstore.dao.ProductSizeDAO;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.daoimpl.ProductSizeDAOImpl;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductQuery;
import com.fashionstore.model.ProductSize;

import java.util.List;

public class ProductService {

    private final ProductDAO productDAO;
    private final ProductSizeDAO sizeDAO;

    public ProductService() {
        this.productDAO = new ProductDAOImpl();
        this.sizeDAO = new ProductSizeDAOImpl();
    }

    public Product getProductById(int productId) {
        return productDAO.getProductById(productId);
    }

    public List<Product> getAllProducts() {
        return productDAO.getAllProducts();
    }

    public List<Product> getFeaturedProducts(int limit) {
        return productDAO.getFeaturedProducts(limit);
    }

    public List<Product> getProductsByCategory(int categoryId, int page, int limit) {
        return productDAO.getProductsByCategory(categoryId, page, limit);
    }

    public List<Product> searchProducts(String query) {
        return productDAO.searchProducts(query);
    }

    public List<Product> getFilteredProducts(int maxPrice, String[] sizes) {
        return productDAO.getFilteredProducts(maxPrice, sizes);
    }

    public List<Product> getProducts(ProductQuery query) {
        return productDAO.getProducts(query);
    }

    public int countProducts(ProductQuery query) {
        return productDAO.countProducts(query);
    }

    public boolean updateStock(int productId, int quantity) {
        return productDAO.updateStock(productId, quantity);
    }

    public List<ProductSize> getProductSizes(int productId) {
        return sizeDAO.getSizesByProductId(productId);
    }
}
