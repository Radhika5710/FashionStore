package com.fashionstore.controller.api;

import com.fashionstore.controller.ApiResponse;
import com.fashionstore.model.Coupon;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.CouponService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Modular API controller for coupon management in admin dashboard
 * Handles: GET /api/admin/coupons, POST /api/admin/coupons, PUT /api/admin/coupons/{id}, DELETE /api/admin/coupons/{id}
 */
@WebServlet("/api/admin/coupons/*")
public class AdminCouponApiController extends AdminApiBaseController {

    private static final long serialVersionUID = 1L;

    private CouponService couponService;

    @Override
    public void init() {
        super.init();
        couponService = ServiceRegistry.getInstance().getCouponService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/admin/coupons - List all coupons
                List<Coupon> coupons = couponService.getAllCoupons();
                writeApiResponse(response, 200, ApiResponse.success("Coupons retrieved successfully", Map.of(
                    "coupons", coupons,
                    "count", coupons.size()
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
            // POST /api/admin/coupons - Create new coupon
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                Map<String, Object> body = readJsonBody(request);
                if (!validateParams(response, body, "code", "discountValue")) return;
                
                Coupon coupon = bodyToCoupon(body);
                boolean success = couponService.createCoupon(coupon);
                if (success) {
                    writeApiResponse(response, 201, ApiResponse.success("Coupon created successfully", null));
                } else {
                    writeApiResponse(response, 400, ApiResponse.error("Failed to create coupon"));
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
                        int couponId = Integer.parseInt(segments[1]);
                        Coupon existing = couponService.getCouponById(couponId);
                        if (existing == null) {
                            writeApiResponse(response, 404, ApiResponse.error("Coupon not found"));
                            return;
                        }
                        
                        Map<String, Object> body = readJsonBody(request);
                        if (!validateParams(response, body, "code", "discountValue")) return;
                        
                        Coupon coupon = bodyToCoupon(body);
                        coupon.setCouponId(couponId);
                        boolean success = couponService.updateCoupon(coupon);
                        if (success) {
                            writeApiResponse(response, 200, ApiResponse.success("Coupon updated successfully", null));
                        } else {
                            writeApiResponse(response, 400, ApiResponse.error("Failed to update coupon"));
                        }
                    } catch (NumberFormatException e) {
                        writeApiResponse(response, 400, ApiResponse.error("Invalid coupon ID"));
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
                        int couponId = Integer.parseInt(segments[1]);
                        boolean success = couponService.deleteCoupon(couponId);
                        if (success) {
                            writeApiResponse(response, 200, ApiResponse.success("Coupon deleted successfully", null));
                        } else {
                            writeApiResponse(response, 400, ApiResponse.error("Failed to delete coupon"));
                        }
                    } catch (NumberFormatException e) {
                        writeApiResponse(response, 400, ApiResponse.error("Invalid coupon ID"));
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

    private Coupon bodyToCoupon(Map<String, Object> body) {
        Coupon c = new Coupon();
        c.setCode(strParam(body, "code"));
        c.setDescription(strParam(body, "description"));
        String dt = strParam(body, "discountType");
        c.setDiscountType(dt.isEmpty() ? "percentage" : dt);
        c.setDiscountValue(parseDouble(body.get("discountValue"), 0.0));
        c.setMinimumOrderAmount(parseDouble(body.get("minOrder"), 0.0));
        c.setMaximumDiscountAmount(null);
        Object maxUses = body.get("maxUses");
        Integer usageLimit = maxUses != null ? parseIntFromObject(maxUses, 0) : null;
        c.setUsageLimit(usageLimit);
        c.setUserUsageLimit(1);
        c.setUsageCount(0);
        c.setActive(true);

        try {
            String expires = strParam(body, "expiresAt");
            if (!expires.isEmpty()) {
                java.time.LocalDate ld = java.time.LocalDate.parse(expires);
                c.setValidUntil(Timestamp.valueOf(ld.atTime(23, 59, 59)));
            } else {
                c.setValidUntil(new Timestamp(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
            }
        } catch (Exception e) {
            c.setValidUntil(new Timestamp(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
        }
        c.setValidFrom(new Timestamp(System.currentTimeMillis()));
        return c;
    }
}
