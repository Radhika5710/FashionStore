package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fashionstore.dao.ProductSizeDAO;
import com.fashionstore.model.ProductSize;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductSizeDAOImpl implements ProductSizeDAO {

    private static final Logger logger = LoggerFactory.getLogger(ProductSizeDAOImpl.class);

    private ProductSize mapSize(ResultSet rs) throws Exception {
        ProductSize size = new ProductSize();
        size.setProductSizeId(rs.getInt("product_size_id"));
        size.setProductId(rs.getInt("product_id"));
        size.setSizeLabel(rs.getString("size_label"));
        size.setStockQuantity(rs.getInt("stock_quantity"));
        size.setSkuCode(rs.getString("sku_code"));
        size.setAvailable(rs.getBoolean("is_available"));
        return size;
    }

    @Override
    public int addProductSize(ProductSize size) {
        String sql = "INSERT INTO product_sizes (product_id, size_label, stock_quantity, sku_code, is_available) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, size.getProductId());
            ps.setString(2, size.getSizeLabel());
            ps.setInt(3, size.getStockQuantity());
            ps.setString(4, size.getSkuCode());
            ps.setBoolean(5, size.isAvailable());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }

        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.addProductSize Error: {}", e.getMessage());
        }

        return 0;
    }

    @Override
    public int updateProductSize(ProductSize size) {
        String sql = "UPDATE product_sizes SET size_label=?, stock_quantity=?, sku_code=?, is_available=? WHERE product_size_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, size.getSizeLabel());
            ps.setInt(2, size.getStockQuantity());
            ps.setString(3, size.getSkuCode());
            ps.setBoolean(4, size.isAvailable());
            ps.setInt(5, size.getProductSizeId());

            return ps.executeUpdate();

        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.updateProductSize Error: {}", e.getMessage());
        }

        return 0;
    }

    @Override
    public int deleteProductSize(int productSizeId) {
        String sql = "DELETE FROM product_sizes WHERE product_size_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, productSizeId);
            return ps.executeUpdate();

        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.deleteProductSize Error: {}", e.getMessage());
        }

        return 0;
    }

    @Override
    public ProductSize getProductSizeById(int productSizeId) {
        String sql = "SELECT * FROM product_sizes WHERE product_size_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, productSizeId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapSize(rs);

        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.getProductSizeById Error: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public List<ProductSize> getSizesByProductId(int productId) {
        List<ProductSize> list = new ArrayList<>();
        String sql = "SELECT * FROM product_sizes WHERE product_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapSize(rs));
            }

        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.getSizesByProductId Error: {}", e.getMessage());
        }

        return list;
    }

    @Override
    public List<ProductSize> getAvailableSizesByProductId(int productId) {
        List<ProductSize> list = new ArrayList<>();
        String sql = "SELECT * FROM product_sizes WHERE product_id=? AND is_available=true AND stock_quantity>0";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapSize(rs));
            }

        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.getAvailableSizesByProductId Error: {}", e.getMessage());
        }

        return list;
    }

    @Override
    public boolean updateStock(int productSizeId, int quantity) {
        String sql = "UPDATE product_sizes SET stock_quantity=?, is_available=? WHERE product_size_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setBoolean(2, quantity > 0);
            ps.setInt(3, productSizeId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.updateStock Error: {}", e.getMessage());
        }

        return false;
    }

    @Override
    public boolean reduceStock(int productSizeId, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        String sql = "UPDATE product_sizes " +
                     "SET stock_quantity = stock_quantity - ?, " +
                     "    is_available = CASE WHEN (stock_quantity - ?) > 0 THEN true ELSE false END " +
                     "WHERE product_size_id = ? AND stock_quantity >= ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setInt(2, quantity);
            ps.setInt(3, productSizeId);
            ps.setInt(4, quantity);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.reduceStock Error: {}", e.getMessage());
        }

        return false;
    }

    @Override
    public boolean reduceStock(int productId, String sizeLabel, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        String sql = "UPDATE product_sizes " +
                     "SET stock_quantity = stock_quantity - ?, " +
                     "    is_available = CASE WHEN (stock_quantity - ?) > 0 THEN true ELSE false END " +
                     "WHERE product_id = ? AND size_label = ? AND stock_quantity >= ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setInt(2, quantity);
            ps.setInt(3, productId);
            ps.setString(4, sizeLabel);
            ps.setInt(5, quantity);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.reduceStock (by product) Error: {}", e.getMessage());
        }

        return false;
    }

    @Override
    public void addOrUpdateSize(ProductSize size) {
        String checkSql = "SELECT product_size_id FROM product_sizes WHERE product_id = ? AND size_label = ?";
        String updateSql = "UPDATE product_sizes SET stock_quantity = ?, is_available = ? WHERE product_size_id = ?";
        String insertSql = "INSERT INTO product_sizes (product_id, size_label, stock_quantity, is_available) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                checkPs.setInt(1, size.getProductId());
                checkPs.setString(2, size.getSizeLabel());

                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                            updatePs.setInt(1, size.getStockQuantity());
                            updatePs.setBoolean(2, size.getStockQuantity() > 0);
                            updatePs.setInt(3, rs.getInt("product_size_id"));
                            updatePs.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
                            insertPs.setInt(1, size.getProductId());
                            insertPs.setString(2, size.getSizeLabel());
                            insertPs.setInt(3, size.getStockQuantity());
                            insertPs.setBoolean(4, size.getStockQuantity() > 0);
                            insertPs.executeUpdate();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("ProductSizeDAOImpl.addOrUpdateSize Error: {}", e.getMessage());
        }
    }
}