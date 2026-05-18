package com.fashionstore.controller.api;

import com.fashionstore.controller.ApiResponse;
import com.fashionstore.model.Category;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.CategoryService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

/**
 * Modular API controller for category management in admin dashboard
 * Handles: GET /api/admin/categories, POST /api/admin/categories, PUT /api/admin/categories/{id}, DELETE /api/admin/categories/{id}
 */
@WebServlet("/api/admin/categories/*")
public class AdminCategoryApiController extends AdminApiBaseController {

    private static final long serialVersionUID = 1L;

    private CategoryService categoryService;

    @Override
    public void init() {
        super.init();
        categoryService = ServiceRegistry.getInstance().getCategoryService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/admin/categories - List all categories
                List<Category> categories = categoryService.getAllCategories();
                writeApiResponse(response, 200, ApiResponse.success("Categories retrieved successfully", Map.of(
                    "categories", categories,
                    "count", categories.size()
                )));
                return;
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!isTrustedStateChangingRequest(request)) {
            writeApiResponse(response, 403, ApiResponse.error("Blocked by origin policy"));
            return;
        }
        if (!ensureAdmin(request, response)) return;
        
        try {
            // POST /api/admin/categories - Create new category
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                Map<String, Object> body = readJsonBody(request);
                if (!validateParams(response, body, "name")) return;
                
                Category category = new Category();
                category.setCategoryName(strParam(body, "name"));
                category.setDescription(strParam(body, "description"));
                category.setActive(true);
                int newId = categoryService.addCategory(category);
                if (newId > 0) {
                    writeApiResponse(response, 201, ApiResponse.success("Category created successfully", Map.of("categoryId", newId)));
                } else {
                    writeApiResponse(response, 400, ApiResponse.error("Failed to create category"));
                }
                return;
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!isTrustedStateChangingRequest(request)) {
            writeApiResponse(response, 403, ApiResponse.error("Blocked by origin policy"));
            return;
        }
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && !pathInfo.equals("/")) {
                String[] segments = pathInfo.split("/");
                if (segments.length == 2) {
                    try {
                        int categoryId = Integer.parseInt(segments[1]);
                        Category existing = categoryService.getCategoryById(categoryId);
                        if (existing == null) {
                            writeApiResponse(response, 404, ApiResponse.error("Category not found"));
                            return;
                        }
                        
                        Map<String, Object> body = readJsonBody(request);
                        if (!validateParams(response, body, "name")) return;
                        
                        existing.setCategoryName(strParam(body, "name"));
                        existing.setDescription(strParam(body, "description"));
                        boolean success = categoryService.updateCategory(existing);
                        if (success) {
                            writeApiResponse(response, 200, ApiResponse.success("Category updated successfully", null));
                        } else {
                            writeApiResponse(response, 400, ApiResponse.error("Failed to update category"));
                        }
                    } catch (NumberFormatException e) {
                        writeApiResponse(response, 400, ApiResponse.error("Invalid category ID"));
                    }
                    return;
                }
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!isTrustedStateChangingRequest(request)) {
            writeApiResponse(response, 403, ApiResponse.error("Blocked by origin policy"));
            return;
        }
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && !pathInfo.equals("/")) {
                String[] segments = pathInfo.split("/");
                if (segments.length == 2) {
                    try {
                        int categoryId = Integer.parseInt(segments[1]);
                        boolean success = categoryService.deleteCategory(categoryId);
                        if (success) {
                            writeApiResponse(response, 200, ApiResponse.success("Category deleted successfully", null));
                        } else {
                            writeApiResponse(response, 400, ApiResponse.error("Failed to delete category"));
                        }
                    } catch (NumberFormatException e) {
                        writeApiResponse(response, 400, ApiResponse.error("Invalid category ID"));
                    }
                    return;
                }
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        applyCors(request, response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
