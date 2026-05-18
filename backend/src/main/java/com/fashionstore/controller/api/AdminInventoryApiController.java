package com.fashionstore.controller.api;

import com.fashionstore.controller.ApiResponse;
import com.fashionstore.model.Product;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.ProductService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

/**
 * Modular API controller for inventory management in admin dashboard
 * Handles: GET /api/admin/inventory, GET /api/admin/inventory/low-stock, PUT /api/admin/inventory/{id}/stock
 */
@WebServlet("/api/admin/inventory/*")
public class AdminInventoryApiController extends AdminApiBaseController {

    private static final long serialVersionUID = 1L;

    private ProductService productService;

    @Override
    public void init() {
        super.init();
        productService = ServiceRegistry.getInstance().getProductService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/admin/inventory - List all products for inventory management
                List<Product> products = productService.getAllProducts();
                writeApiResponse(response, 200, ApiResponse.success("Inventory retrieved successfully", Map.of(
                    "products", products.stream().map(this::publicProduct).toList(),
                    "count", products.size()
                )));
                return;
            }
            
            // GET /api/admin/inventory/low-stock - Get low stock products
            if (pathInfo.equals("/low-stock")) {
                List<Product> products = productService.getAllProducts();
                List<Product> lowStock = products.stream().filter(p -> p.getStockQuantity() <= 5).toList();
                writeApiResponse(response, 200, ApiResponse.success("Low stock products retrieved successfully", Map.of(
                    "products", lowStock.stream().map(this::publicProduct).toList(),
                    "count", lowStock.size()
                )));
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
                if (segments.length == 3 && "stock".equals(segments[2])) {
                    try {
                        int productId = Integer.parseInt(segments[1]);
                        Map<String, Object> body = readJsonBody(request);
                        if (!validateParams(response, body, "stock")) return;
                        
                        int stock = parseIntFromObject(body.get("stock"), 0);
                        boolean success = productService.updateStock(productId, stock);
                        if (success) {
                            writeApiResponse(response, 200, ApiResponse.success("Stock updated successfully", null));
                        } else {
                            writeApiResponse(response, 400, ApiResponse.error("Failed to update stock"));
                        }
                    } catch (NumberFormatException e) {
                        writeApiResponse(response, 400, ApiResponse.error("Invalid product ID"));
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

    private Map<String, Object> publicProduct(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getProductId());
        m.put("name", p.getProductName());
        m.put("description", p.getDescription());
        m.put("price", p.getPrice());
        m.put("discount", p.getDiscountPercent());
        m.put("stock", p.getStockQuantity());
        m.put("imageUrl", p.getImageUrl());
        m.put("category", p.getCategoryName());
        m.put("categoryId", p.getCategoryId());
        m.put("status", p.isActive() ? "active" : "inactive");
        m.put("brand", p.getBrand());
        m.put("isNew", p.isNew());
        m.put("isSale", p.isSale());
        m.put("isTrending", p.isTrending());
        List<String> sizeLabels = new ArrayList<>();
        if (p.getSizes() != null) {
            for (com.fashionstore.model.ProductSize ps : p.getSizes()) sizeLabels.add(ps.getSizeLabel());
        }
        m.put("sizes", sizeLabels);
        return m;
    }
}
