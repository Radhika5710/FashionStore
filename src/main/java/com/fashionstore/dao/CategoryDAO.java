package com.fashionstore.dao;

import com.fashionstore.model.Category;
import java.util.List;

public interface CategoryDAO {

    int addCategory(Category category);

    boolean updateCategory(Category category);

    boolean deleteCategory(int categoryId);

    Category getCategoryById(int categoryId);

    List<Category> getAllCategories();

    List<Category> getActiveCategories();
}