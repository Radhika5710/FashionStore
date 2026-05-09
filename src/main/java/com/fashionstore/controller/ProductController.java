package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.CategoryDAO;
import com.fashionstore.dao.ProductDAO;
import com.fashionstore.daoimpl.CategoryDAOImpl;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.domain.CategoryType;
import com.fashionstore.model.Category;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductQuery;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/products")
public class ProductController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private ProductDAO productDAO;
    private CategoryDAO categoryDAO;

    @Override
    public void init() {
        productDAO = new ProductDAOImpl();
        categoryDAO = new CategoryDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String search = request.getParameter("search");
            String tag = request.getParameter("tag");
            String categoryIdStr = request.getParameter("categoryId");
            String categorySlug = request.getParameter("category");
            String minPriceStr = request.getParameter("minPrice");
            String maxPriceStr = request.getParameter("maxPrice");
            String[] sizes = request.getParameterValues("size");
            String brand = request.getParameter("brand");
            String sortBy = request.getParameter("sortBy");
            String pageStr = request.getParameter("page");

            Integer categoryId = parseIntOrNull(categoryIdStr);
            if (categoryId == null && categorySlug != null && !categorySlug.isBlank()) {
                categoryId = resolveCategoryId(categorySlug);
            }
            Integer maxPrice = parsePriceOrNull(maxPriceStr);
            Integer minPrice = parsePriceOrNull(minPriceStr);
            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                int temp = minPrice;
                minPrice = maxPrice;
                maxPrice = temp;
            }
            minPriceStr = minPrice != null ? String.valueOf(minPrice) : "";
            maxPriceStr = maxPrice != null ? String.valueOf(maxPrice) : "";

            if (categoryId == null && (tag == null || tag.isBlank()) && search != null && !search.isBlank()) {
                Category resolved = resolveCategoryFromSearch(search);
                if (resolved != null) {
                    categoryId = resolved.getCategoryId();
                    search = null;
                }
            }

            int page = 1;
            if (pageStr != null && !pageStr.isBlank()) {
                page = Math.max(1, Integer.parseInt(pageStr));
            }
            int pageSize = 8;
            int offset = (page - 1) * pageSize;

            ProductQuery countQuery = new ProductQuery();
            countQuery.setSearch(search);
            countQuery.setCategoryId(categoryId);
            countQuery.setTag(tag);
            countQuery.setMinPrice(minPrice);
            countQuery.setMaxPrice(maxPrice);
            countQuery.setSizes(sizes);
            countQuery.setBrand(brand);
            countQuery.setActiveOnly(true);

            int totalProducts = productDAO.countProducts(countQuery);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalProducts / pageSize));
            if (page > totalPages) {
                page = totalPages;
                offset = (page - 1) * pageSize;
            }

            ProductQuery listQuery = new ProductQuery();
            listQuery.setSearch(search);
            listQuery.setCategoryId(categoryId);
            listQuery.setTag(tag);
            listQuery.setMinPrice(minPrice);
            listQuery.setMaxPrice(maxPrice);
            listQuery.setSizes(sizes);
            listQuery.setBrand(brand);
            listQuery.setSortBy(sortBy);
            listQuery.setOffset(offset);
            listQuery.setLimit(pageSize);
            listQuery.setActiveOnly(true);

            List<Product> products = productDAO.getProducts(listQuery);
            
            // Debug: Log query results
            logger.info("Product query results: search={}, categoryId={}, tag={}, sizes={}, brand={}, totalProducts={}, returnedProducts={}", 
                search, categoryId, tag, sizes != null ? List.of(sizes) : "null", brand, totalProducts, products.size());
            
            // Fallback: If no products returned with activeOnly=true, try without active filter
            if (products.isEmpty() && totalProducts > 0) {
                logger.warn("No active products found but total count is {}, retrying without active filter", totalProducts);
                listQuery.setActiveOnly(false);
                products = productDAO.getProducts(listQuery);
                logger.info("Fallback query returned {} products", products.size());
            }
            
            List<Category> categories = categoryDAO.getActiveCategories();

            request.setAttribute("products", products);
            request.setAttribute("categories", categories);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("sortBy", sortBy);
            request.setAttribute("minPrice", minPriceStr);
            request.setAttribute("maxPrice", maxPriceStr);
            request.setAttribute("search", search);
            request.setAttribute("brand", brand);
            request.setAttribute("categoryId", categoryId);
            request.setAttribute("categorySlug", CategoryType.fromId(categoryId).map(CategoryType::getSlug).orElse(categorySlug));
            request.setAttribute("tag", tag);
            request.setAttribute("selectedSizes", sizes != null ? List.of(sizes) : new ArrayList<>());

            request.getRequestDispatcher("/WEB-INF/views/products.jsp")
                    .forward(request, response);

        } catch (Exception e) {
            logger.error("Error in ProductController.doGet: {}", e.getMessage(), e);
            // Render an empty catalog instead of redirecting back to /products
            // (avoids infinite redirect loop when the underlying error is persistent).
            request.setAttribute("products", new ArrayList<Product>());
            request.setAttribute("categories", new ArrayList<Category>());
            request.setAttribute("currentPage", 1);
            request.setAttribute("totalPages", 1);
            request.setAttribute("errorMessage",
                    "We hit a snag loading the catalog. Please try again in a moment.");
            request.getRequestDispatcher("/WEB-INF/views/products.jsp")
                    .forward(request, response);
        }
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parsePriceOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Category resolveCategoryFromSearch(String search) {
        String key = CategoryType.normalize(search);
        if (key.isEmpty()) {
            return null;
        }

        List<Category> categories = categoryDAO.getActiveCategories();
        for (Category c : categories) {
            if (CategoryType.normalize(c.getCategoryName()).equals(key)) {
                return c;
            }
        }

        return CategoryType.fromName(search)
                .map(type -> findCategoryByKey(categories, type.getSlug()))
                .orElse(null);
    }

    private Integer resolveCategoryId(String value) {
        List<Category> categories = categoryDAO.getActiveCategories();
        Category category = CategoryType.fromName(value)
                .map(type -> findCategoryByKey(categories, type.getSlug()))
                .orElseGet(() -> findCategoryByKey(categories, value));
        return category != null ? category.getCategoryId() : null;
    }

    private Category findCategoryByKey(List<Category> categories, String key) {
        for (Category c : categories) {
            String categorySlug = c.getCategorySlug();
            if (CategoryType.normalize(categorySlug).equals(CategoryType.normalize(key))
                    || CategoryType.normalize(c.getCategoryName()).equals(CategoryType.normalize(key))) {
                return c;
            }
        }
        return null;
    }
}
