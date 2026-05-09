package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.cache.CacheKey;
import com.fashionstore.cache.CacheService;
import com.fashionstore.dao.ProductDAO;
import com.fashionstore.dao.ProductSizeDAO;
import com.fashionstore.domain.CategoryType;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductQuery;
import com.fashionstore.model.ProductSize;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ProductDAOImpl implements ProductDAO {

    private static final Logger logger = LoggerFactory.getLogger(ProductDAOImpl.class);
    private final ProductSizeDAO sizeDAO = new ProductSizeDAOImpl();
    private final CacheService cacheService = CacheService.getInstance();

    // 🔥 MAP RESULTSET → PRODUCT (without sizes - sizes loaded in batch)
    // Resilient to missing columns: gracefully handles schema evolution
    private Product mapProduct(ResultSet rs) throws Exception {

        Product p = new Product();

        p.setProductId(rs.getInt("product_id"));
        p.setProductName(rs.getString("product_name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getDouble("price"));
        p.setDiscountPercent(rs.getDouble("discount_percent"));
        p.setImageUrl(rs.getString("image_url"));

        // Optional badge/metadata fields: safe mapping with defaults
        // These columns may not exist in older database schemas
        try { p.setNew(rs.getBoolean("is_new")); } catch (SQLException e) { p.setNew(false); }
        try { p.setSale(rs.getBoolean("is_sale")); } catch (SQLException e) { p.setSale(false); }
        try { p.setTrending(rs.getBoolean("is_trending")); } catch (SQLException e) { p.setTrending(false); }
        try { p.setBrand(rs.getString("brand")); } catch (SQLException e) { p.setBrand(null); }
        try { p.setActive(rs.getBoolean("active")); } catch (SQLException e) { p.setActive(true); }
        try { p.setStockQuantity(rs.getInt("stock_quantity")); } catch (SQLException e) { p.setStockQuantity(0); }
        try { p.setCategoryId(rs.getInt("category_id")); } catch (SQLException e) { p.setCategoryId(0); }
        try {
            String categoryName = rs.getString("category_name");
            p.setCategoryName(categoryName != null ? categoryName.trim() : null);
            p.setCategorySlug(CategoryType.fromName(categoryName)
                    .map(CategoryType::getSlug)
                    .orElse(CategoryType.fromId(p.getCategoryId()).map(CategoryType::getSlug).orElse(null)));
        } catch (SQLException e) {
            CategoryType.fromId(p.getCategoryId()).ifPresent(type -> {
                p.setCategoryName(type.getDisplayName());
                p.setCategorySlug(type.getSlug());
            });
        }

        // Timestamp fields
        try { p.setCreatedAt(rs.getTimestamp("created_at")); } catch (SQLException e) { p.setCreatedAt(null); }
        try { p.setUpdatedAt(rs.getTimestamp("updated_at")); } catch (SQLException e) { p.setUpdatedAt(null); }

        return p;
    }

    // 🔥 BATCH LOAD SIZES FOR MULTIPLE PRODUCTS (fixes N+1 query problem)
    private void batchLoadSizes(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        // Collect all product IDs (productId is primitive int, cannot be null)
        List<Integer> productIds = products.stream()
                .map(Product::getProductId)
                .distinct()
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            // No valid product IDs, set empty sizes for all products
            for (Product p : products) {
                p.setSizes(new ArrayList<>());
            }
            return;
        }

        // Fetch all sizes in a single query using IN clause
        String sql = "SELECT * FROM product_sizes WHERE product_id IN (" +
                String.join(",", Collections.nCopies(productIds.size(), "?")) + ") " +
                "ORDER BY product_id, size_label";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Set all product ID parameters
            for (int i = 0; i < productIds.size(); i++) {
                ps.setInt(i + 1, productIds.get(i));
            }

            ResultSet rs = ps.executeQuery();

            // Group sizes by product ID using a Map
            Map<Integer, List<ProductSize>> sizesByProductId = new HashMap<>();
            while (rs.next()) {
                ProductSize size = new ProductSize();
                size.setProductSizeId(rs.getInt("product_size_id"));
                size.setProductId(rs.getInt("product_id"));
                size.setSizeLabel(rs.getString("size_label"));
                size.setStockQuantity(rs.getInt("stock_quantity"));
                size.setSkuCode(rs.getString("sku_code"));
                size.setAvailable(rs.getBoolean("is_available"));

                sizesByProductId
                    .computeIfAbsent(rs.getInt("product_id"), k -> new ArrayList<>())
                    .add(size);
            }

            // Assign sizes to products
            for (Product p : products) {
                p.setSizes(sizesByProductId.getOrDefault(p.getProductId(), new ArrayList<>()));
            }

        } catch (SQLException e) {
            logger.warn("Sizes table might not exist or schema mismatch, skipping batch load: {}", e.getMessage());
            for (Product p : products) {
                p.setSizes(new ArrayList<>());
            }
        } catch (Exception e) {
            logger.error("Error in batchLoadSizes: {}", e.getMessage(), e);
            for (Product p : products) {
                p.setSizes(sizeDAO.getSizesByProductId(p.getProductId()));
            }
        }
    }

    // 🔥 GET ALL PRODUCTS
    @Override
    public List<Product> getAllProducts() {

        List<Product> list = new ArrayList<>();

        String sql = "SELECT p.*, c.category_name FROM products p " +
                "JOIN categories c ON c.category_id = p.category_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapProduct(rs));
            }

            // Batch load sizes to avoid N+1 queries
            batchLoadSizes(list);

        } catch (Exception e) {
            logger.error("Error in getAllProducts: {}", e.getMessage(), e);
        }

        return list;
    }

    // 🔥 GET PRODUCT BY ID
    @Override
    public Product getProductById(int productId) {
        String cacheKey = CacheKey.product(productId);
        
        Product cached = cacheService.get(cacheKey, Product.class);
        if (cached != null) {
            logger.debug("Cache hit for product: {}", productId);
            return cached;
        }

        String sql = "SELECT p.*, c.category_name FROM products p " +
                "JOIN categories c ON c.category_id = p.category_id " +
                "WHERE p.product_id = ? AND c.is_active = TRUE";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Product p = mapProduct(rs);
                p.setSizes(sizeDAO.getSizesByProductId(productId));
                cacheService.put(cacheKey, p, 1, TimeUnit.HOURS);
                logger.debug("Cached product: {}", productId);
                return p;
            }

        } catch (Exception e) {
            logger.error("Error in getProductById for ID {}: {}", productId, e.getMessage(), e);
        }

        return null;
    }

    // 🔥 FILTER PRODUCTS
    @Override
    public List<Product> getFilteredProducts(int maxPrice, String[] sizes) {

        List<Product> list = new ArrayList<>();

        try {

            // Architecture Upgrade: Filtering by sizes now requires a JOIN with product_sizes table
            StringBuilder sql = new StringBuilder(
                    "SELECT DISTINCT p.*, c.category_name FROM products p " +
                    "JOIN categories c ON c.category_id = p.category_id "
            );
            
            if (sizes != null && sizes.length > 0) {
                sql.append("JOIN product_sizes ps ON p.product_id = ps.product_id ");
            }
            
            sql.append("WHERE p.active = TRUE AND c.is_active = TRUE AND p.price <= ?");

            // 🔥 ADD SIZE FILTER IF PRESENT
            if (sizes != null && sizes.length > 0) {
                sql.append(" AND ps.size_label IN (");

                for (int i = 0; i < sizes.length; i++) {
                    sql.append("?");
                    if (i < sizes.length - 1) sql.append(",");
                }

                sql.append(")");
            }

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql.toString())) {
                ps.setInt(1, maxPrice);

                if (sizes != null && sizes.length > 0) {
                    for (int i = 0; i < sizes.length; i++) {
                        ps.setString(i + 2, sizes[i]);
                    }
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapProduct(rs));
                    }
                }
                
                // Batch load sizes to avoid N+1 queries
                batchLoadSizes(list);
            }

        } catch (Exception e) {
            logger.error("Error in getFilteredProducts: {}", e.getMessage(), e);
        }

        return list;
    }

    @Override
    public List<Product> searchProducts(String query) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.category_name FROM products p " +
                "JOIN categories c ON c.category_id = p.category_id " +
                "WHERE p.active = TRUE AND c.is_active = TRUE " +
                "AND (p.product_name LIKE ? OR p.description LIKE ? OR p.brand LIKE ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String searchPattern = "%" + query + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapProduct(rs));
            }

            // Batch load sizes to avoid N+1 queries
            batchLoadSizes(list);

        } catch (Exception e) {
            logger.error("Error in searchProducts for query '{}': {}", query, e.getMessage(), e);
        }

        return list;
    }

    @Override
    public List<Product> getProducts(String search, Integer maxPrice, String[] sizes, String sortBy, int offset, int limit) {
        return getProducts(search, maxPrice, sizes, null, sortBy, offset, limit);
    }
    
    @Override
    public List<Product> getProducts(String search, Integer maxPrice, String[] sizes, String brand, String sortBy, int offset, int limit) {
        ProductQuery query = new ProductQuery();
        query.setSearch(search);
        query.setMaxPrice(maxPrice);
        query.setSizes(sizes);
        query.setBrand(brand);
        query.setSortBy(sortBy);
        query.setOffset(offset);
        query.setLimit(limit);
        query.setActiveOnly(true);
        return getProducts(query);
    }

    @Override
    public int countProducts(String search, Integer maxPrice, String[] sizes) {
        return countProducts(search, maxPrice, sizes, null);
    }
    
    @Override
    public int countProducts(String search, Integer maxPrice, String[] sizes, String brand) {
        ProductQuery query = new ProductQuery();
        query.setSearch(search);
        query.setMaxPrice(maxPrice);
        query.setSizes(sizes);
        query.setBrand(brand);
        query.setActiveOnly(true);
        return countProducts(query);
    }

    @Override
    public List<Product> getProducts(ProductQuery query) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT p.*, c.category_name FROM products p ");
        List<Object> params = new ArrayList<>();

        sql.append("JOIN categories c ON c.category_id = p.category_id ");

        String[] sizes = query != null ? query.getSizes() : null;
        if (sizes != null && sizes.length > 0) {
            sql.append("JOIN product_sizes ps ON p.product_id = ps.product_id ");
        }

        sql.append("WHERE 1=1 ");

        if (query == null || query.isActiveOnly()) {
            sql.append("AND p.active = TRUE ");
        }

        sql.append("AND c.is_active = TRUE ");

        if (query != null && query.getCategoryId() != null) {
            sql.append("AND p.category_id = ? ");
            params.add(query.getCategoryId());
        }

        if (query != null && query.getTag() != null && !query.getTag().isBlank()) {
            String tag = query.getTag().trim().toLowerCase();
            if ("sale".equals(tag) || "deals".equals(tag)) {
                sql.append("AND p.is_sale = TRUE ");
            } else if ("new".equals(tag)) {
                sql.append("AND p.is_new = TRUE ");
            } else if ("trending".equals(tag)) {
                sql.append("AND p.is_trending = TRUE ");
            }
        }

        if (query != null && query.getSearch() != null && !query.getSearch().isBlank()) {
            sql.append("AND (p.product_name LIKE ? OR p.description LIKE ? OR p.brand LIKE ?) ");
            String pattern = "%" + query.getSearch().trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (query != null && query.getMinPrice() != null) {
            sql.append("AND p.price >= ? ");
            params.add(query.getMinPrice());
        }

        if (query != null && query.getMaxPrice() != null) {
            sql.append("AND p.price <= ? ");
            params.add(query.getMaxPrice());
        }

        if (query != null && query.getBrand() != null && !query.getBrand().isBlank()) {
            sql.append("AND p.brand = ? ");
            params.add(query.getBrand().trim());
        }

        if (sizes != null && sizes.length > 0) {
            sql.append("AND ps.is_available = TRUE ");
            sql.append("AND ps.stock_quantity > 0 ");
            sql.append("AND ps.size_label IN (");
            for (int i = 0; i < sizes.length; i++) {
                sql.append("?");
                if (i < sizes.length - 1) {
                    sql.append(",");
                }
                params.add(sizes[i]);
            }
            sql.append(") ");
        }

        String sortBy = query != null ? query.getSortBy() : null;
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY p.price ASC ");
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY p.price DESC ");
        } else if ("name_asc".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY p.product_name ASC ");
        } else if ("popular".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY p.popular_score DESC, p.product_id DESC ");
        } else {
            sql.append("ORDER BY p.product_id DESC ");
        }

        int limit = query != null ? query.getLimit() : 0;
        int offset = query != null ? query.getOffset() : 0;
        if (limit <= 0) {
            limit = 8;
        }
        offset = Math.max(0, offset);

        sql.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProduct(rs));
                }
            }

            batchLoadSizes(list);
        } catch (Exception e) {
            logger.error("Error in getProducts: {}", e.getMessage(), e);
        }

        return list;
    }

    @Override
    public int countProducts(ProductQuery query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT p.product_id) FROM products p ");
        List<Object> params = new ArrayList<>();

        sql.append("JOIN categories c ON c.category_id = p.category_id ");

        String[] sizes = query != null ? query.getSizes() : null;
        if (sizes != null && sizes.length > 0) {
            sql.append("JOIN product_sizes ps ON p.product_id = ps.product_id ");
        }

        sql.append("WHERE 1=1 ");

        if (query == null || query.isActiveOnly()) {
            sql.append("AND p.active = TRUE ");
        }

        sql.append("AND c.is_active = TRUE ");

        if (query != null && query.getCategoryId() != null) {
            sql.append("AND p.category_id = ? ");
            params.add(query.getCategoryId());
        }

        if (query != null && query.getTag() != null && !query.getTag().isBlank()) {
            String tag = query.getTag().trim().toLowerCase();
            if ("sale".equals(tag) || "deals".equals(tag)) {
                sql.append("AND p.is_sale = TRUE ");
            } else if ("new".equals(tag)) {
                sql.append("AND p.is_new = TRUE ");
            } else if ("trending".equals(tag)) {
                sql.append("AND p.is_trending = TRUE ");
            }
        }

        if (query != null && query.getSearch() != null && !query.getSearch().isBlank()) {
            sql.append("AND (p.product_name LIKE ? OR p.description LIKE ? OR p.brand LIKE ?) ");
            String pattern = "%" + query.getSearch().trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (query != null && query.getMinPrice() != null) {
            sql.append("AND p.price >= ? ");
            params.add(query.getMinPrice());
        }

        if (query != null && query.getMaxPrice() != null) {
            sql.append("AND p.price <= ? ");
            params.add(query.getMaxPrice());
        }

        if (query != null && query.getBrand() != null && !query.getBrand().isBlank()) {
            sql.append("AND p.brand = ? ");
            params.add(query.getBrand().trim());
        }

        if (sizes != null && sizes.length > 0) {
            sql.append("AND ps.is_available = TRUE ");
            sql.append("AND ps.stock_quantity > 0 ");
            sql.append("AND ps.size_label IN (");
            for (int i = 0; i < sizes.length; i++) {
                sql.append("?");
                if (i < sizes.length - 1) {
                    sql.append(",");
                }
                params.add(sizes[i]);
            }
            sql.append(") ");
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.error("ProductDAOImpl.countProducts Error: {}", e.getMessage(), e);
        }

        return 0;
    }

    @Override
    public int addProduct(Product product) {
        String sql = "INSERT INTO products (product_name, description, price, discount_percent, image_url, active, is_new, is_sale, is_trending, brand, stock_quantity, category_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getProductName());
            ps.setString(2, product.getDescription());
            ps.setDouble(3, product.getPrice());
            ps.setDouble(4, product.getDiscountPercent());
            ps.setString(5, product.getImageUrl());
            ps.setBoolean(6, product.isActive());
            ps.setBoolean(7, product.isNew());
            ps.setBoolean(8, product.isSale());
            ps.setBoolean(9, product.isTrending());
            ps.setString(10, product.getBrand());
            ps.setInt(11, Math.max(0, product.getStockQuantity()));
            ps.setInt(12, Math.max(0, product.getCategoryId()));
            
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("ProductDAOImpl.addProduct Error: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET product_name=?, description=?, price=?, discount_percent=?, image_url=?, active=?, is_new=?, is_sale=?, is_trending=?, brand=?, stock_quantity=?, category_id=? WHERE product_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, product.getProductName());
            ps.setString(2, product.getDescription());
            ps.setDouble(3, product.getPrice());
            ps.setDouble(4, product.getDiscountPercent());
            ps.setString(5, product.getImageUrl());
            ps.setBoolean(6, product.isActive());
            ps.setBoolean(7, product.isNew());
            ps.setBoolean(8, product.isSale());
            ps.setBoolean(9, product.isTrending());
            ps.setString(10, product.getBrand());
            ps.setInt(11, Math.max(0, product.getStockQuantity()));
            ps.setInt(12, Math.max(0, product.getCategoryId()));
            ps.setInt(13, product.getProductId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("ProductDAOImpl.updateProduct Error: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE product_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("ProductDAOImpl.deleteProduct Error: {}", e.getMessage());
        }
        return false;
    }

    // ── Enhanced / Optimized interface methods ────────────────────────

    private Integer parseCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        try {
            int id = Integer.parseInt(categoryId.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<Product> getAllProducts(int page, int limit, String search, String categoryId) {
        int offset = (page - 1) * limit;
        ProductQuery query = new ProductQuery();
        query.setSearch(search);
        query.setCategoryId(parseCategoryId(categoryId));
        query.setOffset(offset);
        query.setLimit(limit);
        query.setActiveOnly(true);
        return getProducts(query);
    }

    @Override
    public List<Product> getProductsByCategory(int categoryId, int page, int limit) {
        int offset = (page - 1) * limit;
        ProductQuery query = new ProductQuery();
        query.setCategoryId(categoryId);
        query.setOffset(offset);
        query.setLimit(limit);
        query.setActiveOnly(true);
        return getProducts(query);
    }

    @Override
    public List<Product> getFeaturedProducts(int limit) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.category_name FROM products p " +
                "JOIN categories c ON c.category_id = p.category_id " +
                "WHERE p.active = TRUE AND c.is_active = TRUE " +
                "ORDER BY is_trending DESC, popular_score DESC, is_sale DESC, is_new DESC, product_id DESC LIMIT ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProduct(rs));
                }
            }
            // Batch load sizes to avoid N+1 queries
            batchLoadSizes(list);
        } catch (Exception e) {
            logger.error("ProductDAOImpl.getFeaturedProducts Error: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Product> getTopSellingProducts(int limit) {
        // No sales tracking table yet – fall back to featured
        return getFeaturedProducts(limit);
    }

    @Override
    public List<Product> searchProducts(String query, int page, int limit) {
        int offset = (page - 1) * limit;
        ProductQuery q = new ProductQuery();
        q.setSearch(query);
        q.setOffset(offset);
        q.setLimit(limit);
        q.setActiveOnly(true);
        return getProducts(q);
    }

    @Override
    public boolean updateStock(int productId, int quantity) {
        String sql = "UPDATE product_sizes SET stock_quantity = stock_quantity + ? WHERE product_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            boolean result = ps.executeUpdate() > 0;
            if (result) {
                cacheService.remove(CacheKey.product(productId));
                cacheService.invalidatePattern("fashionstore:products:*");
                logger.debug("Invalidated cache for product: {}", productId);
            }
            return result;
        } catch (Exception e) {
            logger.error("ProductDAOImpl.updateStock Error: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public int getProductCount(String search, String categoryId) {
        ProductQuery query = new ProductQuery();
        query.setSearch(search);
        query.setCategoryId(parseCategoryId(categoryId));
        query.setActiveOnly(true);
        return countProducts(query);
    }

    @Override
    public int getLowStockProductCount(int threshold) {
        String sql = "SELECT COUNT(DISTINCT product_id) as count FROM product_sizes WHERE stock_quantity < ? AND is_available = 1";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, threshold);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            logger.error("ProductDAOImpl.getLowStockProductCount Error: {}", e.getMessage());
        }
        return 0;
    }
}
