package com.fashionstore.serviceimpl;

import com.fashionstore.dao.ProductDAO;
import com.fashionstore.dao.ProductSizeDAO;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductSize;
import com.fashionstore.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for inventory management
 * Uses ProductSizeDAO to manage stock levels and prevent overselling
 */
public class InventoryServiceImpl implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);
    private ProductDAO productDAO;
    private ProductSizeDAO productSizeDAO;

    public InventoryServiceImpl() {
        // Default constructor - DAOs will be set via setter injection
        this.productDAO = null;
        this.productSizeDAO = null;
    }

    public InventoryServiceImpl(ProductDAO productDAO, ProductSizeDAO productSizeDAO) {
        this.productDAO = productDAO;
        this.productSizeDAO = productSizeDAO;
    }

    public void setProductDAO(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public void setProductSizeDAO(ProductSizeDAO productSizeDAO) {
        this.productSizeDAO = productSizeDAO;
    }

    @Override
    public boolean isProductAvailable(int productId, String size, int quantity) {
        if (productSizeDAO == null) {
            throw new IllegalStateException("ProductSizeDAO not initialized - cannot check product availability");
        }

        try {
            List<ProductSize> sizes = productSizeDAO.getAvailableSizesByProductId(productId);
            for (ProductSize ps : sizes) {
                if (size.equals(ps.getSizeLabel()) && ps.getStockQuantity() >= quantity) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking product availability: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int getCurrentStock(int productId, String size) {
        if (productSizeDAO == null) {
            throw new IllegalStateException("ProductSizeDAO not initialized - cannot get current stock");
        }

        try {
            List<ProductSize> sizes = productSizeDAO.getSizesByProductId(productId);
            for (ProductSize ps : sizes) {
                if (size.equals(ps.getSizeLabel())) {
                    return ps.getStockQuantity();
                }
            }
            return 0;
        } catch (Exception e) {
            logger.error("Error getting current stock: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean updateStockLevel(int productId, String size, int newQuantity) {
        if (productSizeDAO == null) {
            throw new IllegalStateException("ProductSizeDAO not initialized - cannot update stock");
        }

        try {
            List<ProductSize> sizes = productSizeDAO.getSizesByProductId(productId);
            for (ProductSize ps : sizes) {
                if (size.equals(ps.getSizeLabel())) {
                    return productSizeDAO.updateStock(ps.getProductSizeId(), newQuantity);
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error updating stock level: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean reserveStock(int productId, String size, int quantity) {
        // Reserve stock by reducing it atomically
        if (productSizeDAO == null) {
            throw new IllegalStateException("ProductSizeDAO not initialized - cannot reserve stock");
        }

        try {
            return productSizeDAO.reduceStock(productId, size, quantity);
        } catch (Exception e) {
            logger.error("Error reserving stock: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean releaseReservedStock(int productId, String size, int quantity) {
        // Release reserved stock by increasing it
        if (productSizeDAO == null) {
            throw new IllegalStateException("ProductSizeDAO not initialized - cannot release reserved stock");
        }

        try {
            return productSizeDAO.increaseStock(productId, size, quantity);
        } catch (Exception e) {
            logger.error("Error releasing reserved stock: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Product> getLowStockProducts(int threshold) {
        if (productSizeDAO == null || productDAO == null) {
            throw new IllegalStateException("ProductSizeDAO or ProductDAO not initialized - cannot get low stock products");
        }

        List<Product> lowStockProducts = new ArrayList<>();
        try {
            List<Product> allProducts = productDAO.getAllProducts();
            for (Product product : allProducts) {
                List<ProductSize> sizes = productSizeDAO.getSizesByProductId(product.getProductId());
                for (ProductSize ps : sizes) {
                    if (ps.getStockQuantity() <= threshold) {
                        lowStockProducts.add(product);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting low stock products: {}", e.getMessage(), e);
        }
        return lowStockProducts;
    }

    @Override
    public List<Product> getOutOfStockProducts() {
        if (productSizeDAO == null || productDAO == null) {
            throw new IllegalStateException("ProductSizeDAO or ProductDAO not initialized - cannot get out of stock products");
        }

        List<Product> outOfStockProducts = new ArrayList<>();
        try {
            List<Product> allProducts = productDAO.getAllProducts();
            for (Product product : allProducts) {
                List<ProductSize> sizes = productSizeDAO.getSizesByProductId(product.getProductId());
                boolean allOutOfStock = true;
                for (ProductSize ps : sizes) {
                    if (ps.getStockQuantity() > 0) {
                        allOutOfStock = false;
                        break;
                    }
                }
                if (allOutOfStock && !sizes.isEmpty()) {
                    outOfStockProducts.add(product);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting out of stock products: {}", e.getMessage(), e);
        }
        return outOfStockProducts;
    }

    @Override
    public boolean validateStockForOrder(List<ProductSize> items) {
        if (productSizeDAO == null) {
            throw new IllegalStateException("ProductSizeDAO not initialized - cannot validate stock for order");
        }

        try {
            for (ProductSize item : items) {
                if (!isProductAvailable(item.getProductId(), item.getSizeLabel(), item.getStockQuantity())) {
                    logger.warn("Insufficient stock for productId={}, size={}, quantity={}", 
                        item.getProductId(), item.getSizeLabel(), item.getStockQuantity());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error validating stock for order: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean processInventoryAfterOrder(List<ProductSize> items) {
        return processInventoryAfterOrder(items, null);
    }

    /**
     * Process inventory after order with connection for transaction support
     * This version is used within a transaction to ensure atomicity
     */
    @Override
    public boolean processInventoryAfterOrder(List<ProductSize> items, java.sql.Connection conn) {
        if (productSizeDAO == null) {
            throw new IllegalStateException("ProductSizeDAO not initialized - cannot process inventory after order");
        }

        try {
            for (ProductSize item : items) {
                boolean success;
                if (conn != null) {
                    success = productSizeDAO.reduceStock(conn, item.getProductId(), item.getSizeLabel(), item.getStockQuantity());
                } else {
                    success = productSizeDAO.reduceStock(item.getProductId(), item.getSizeLabel(), item.getStockQuantity());
                }
                if (!success) {
                    logger.error("Failed to reduce stock for productId={}, size={}, quantity={}", 
                        item.getProductId(), item.getSizeLabel(), item.getStockQuantity());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error processing inventory after order: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Product> getInventoryReport() {
        if (productDAO == null) {
            throw new IllegalStateException("ProductDAO not initialized - cannot get inventory report");
        }

        try {
            return productDAO.getAllProducts();
        } catch (Exception e) {
            logger.error("Error getting inventory report: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean needsRestocking(int productId, String size) {
        if (productSizeDAO == null) {
            throw new IllegalStateException("ProductSizeDAO not initialized - cannot check restocking need");
        }

        try {
            int currentStock = getCurrentStock(productId, size);
            return currentStock < 10; // Restock threshold
        } catch (Exception e) {
            logger.error("Error checking restocking need: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean batchUpdateInventory(List<ProductSize> updates) {
        if (productSizeDAO == null) {
            throw new IllegalStateException("ProductSizeDAO not initialized - cannot batch update inventory");
        }

        try {
            for (ProductSize update : updates) {
                productSizeDAO.addOrUpdateSize(update);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error in batch inventory update: {}", e.getMessage(), e);
            return false;
        }
    }
}
