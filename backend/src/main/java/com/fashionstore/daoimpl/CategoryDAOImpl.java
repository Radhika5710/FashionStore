package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fashionstore.dao.CategoryDAO;
import com.fashionstore.domain.CategoryType;
import com.fashionstore.model.Category;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAOImpl implements CategoryDAO {

    private static final Logger logger = LoggerFactory.getLogger(CategoryDAOImpl.class);

    private Category mapCategory(ResultSet rs) throws Exception {
        Category category = new Category();
        category.setCategoryId(rs.getInt("category_id"));
        String categoryName = rs.getString("category_name");
        category.setCategoryName(categoryName != null ? categoryName.trim() : null);
        category.setCategorySlug(CategoryType.fromName(categoryName)
                .map(CategoryType::getSlug)
                .orElse(CategoryType.normalize(categoryName)));
        category.setDescription(rs.getString("description"));
        category.setActive(rs.getBoolean("is_active"));
        return category;
    }

    @Override
    public int addCategory(Category category) {
        String sql = "INSERT INTO categories (category_name, description, is_active) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, category.getCategoryName() != null ? category.getCategoryName().trim() : null);
            ps.setString(2, category.getDescription());
            ps.setBoolean(3, category.isActive());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("CategoryDAOImpl.addCategory Error: {}", e.getMessage());
        }

        return 0;
    }

    @Override
    public boolean updateCategory(Category category) {
        String sql = "UPDATE categories SET category_name = ?, description = ?, is_active = ? WHERE category_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, category.getCategoryName() != null ? category.getCategoryName().trim() : null);
            ps.setString(2, category.getDescription());
            ps.setBoolean(3, category.isActive());
            ps.setInt(4, category.getCategoryId());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.error("CategoryDAOImpl.updateCategory Error: {}", e.getMessage());
        }

        return false;
    }

    @Override
    public boolean deleteCategory(int categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.error("CategoryDAOImpl.deleteCategory Error: {}", e.getMessage());
        }

        return false;
    }

    @Override
    public Category getCategoryById(int categoryId) {
        String sql = "SELECT category_id, category_name, description, is_active FROM categories WHERE category_id = ?";

        long startTime = System.currentTimeMillis();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long duration = System.currentTimeMillis() - startTime;
                    if (duration > 200) {
                        logger.warn("Slow query detected: getCategoryById took {}ms for category {}", duration, categoryId);
                    } else {
                        logger.debug("getCategoryById completed in {}ms for category {}", duration, categoryId);
                    }
                    return mapCategory(rs);
                }
            }

        } catch (Exception e) {
            logger.error("CategoryDAOImpl.getCategoryById Error: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT category_id, category_name, description, is_active FROM categories ORDER BY " +
                "CASE LOWER(TRIM(category_name)) " +
                "WHEN 'men' THEN 1 WHEN 'women' THEN 2 WHEN 'footwear' THEN 3 WHEN 'accessories' THEN 4 ELSE 99 END, category_name";

        long startTime = System.currentTimeMillis();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapCategory(rs));
            }

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 200) {
                logger.warn("Slow query detected: getAllCategories took {}ms", duration);
            } else {
                logger.debug("getAllCategories completed in {}ms, fetched {} categories", duration, list.size());
            }

        } catch (Exception e) {
            logger.error("CategoryDAOImpl.getAllCategories Error: {}", e.getMessage());
        }

        return list;
    }

    @Override
    public List<Category> getActiveCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT category_id, category_name, description, is_active FROM categories WHERE is_active = true ORDER BY " +
                "CASE LOWER(TRIM(category_name)) " +
                "WHEN 'men' THEN 1 WHEN 'women' THEN 2 WHEN 'footwear' THEN 3 WHEN 'accessories' THEN 4 ELSE 99 END, category_name";

        long startTime = System.currentTimeMillis();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapCategory(rs));
            }

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 200) {
                logger.warn("Slow query detected: getActiveCategories took {}ms", duration);
            } else {
                logger.debug("getActiveCategories completed in {}ms, fetched {} categories", duration, list.size());
            }

        } catch (Exception e) {
            logger.error("CategoryDAOImpl.getActiveCategories Error: {}", e.getMessage());
        }

        return list;
    }
}
