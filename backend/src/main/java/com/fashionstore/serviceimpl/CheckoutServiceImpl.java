package com.fashionstore.serviceimpl;

import com.fashionstore.dao.CartDAO;
import com.fashionstore.dao.ProductDAO;
import com.fashionstore.model.*;
import com.fashionstore.service.CartService;
import com.fashionstore.service.CheckoutService;
import com.fashionstore.service.OrderService;
import com.fashionstore.registry.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * CheckoutServiceImpl - MVC Service Layer
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL checkout business logic in service layer
 * - ALL order calculations (subtotal, tax, shipping, discount)
 * - ALL payment validation
 * - ALL address validation
 * - Backend is single source of truth
 * - Frontend cannot manipulate calculations
 * 
 * Calculation Logic:
 * 1. Get cart subtotal from CartService
 * 2. Apply coupon discount if valid
 * 3. Calculate shipping (free above threshold)
 * 4. Calculate tax on subtotal after discount
 * 5. Calculate final total
 * 6. Round all values to 2 decimal places
 * 
 * All calculations are performed on backend.
 * Frontend only displays backend-calculated values.
 */
public class CheckoutServiceImpl implements CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutServiceImpl.class);
    private static final double SHIPPING_THRESHOLD = 500.0; // Free shipping above this amount
    private static final double SHIPPING_COST = 50.0;
    private static final double TAX_RATE = 0.18; // 18% GST

    private final CartService cartService;
    private final CartDAO cartDAO;
    private final ProductDAO productDAO;
    private com.fashionstore.service.AddressService addressService;
    private com.fashionstore.service.CouponService couponService;

    public CheckoutServiceImpl() {
        // Default constructor - dependencies will be set via setter injection
        this.cartService = null;
        this.cartDAO = null;
        this.productDAO = null;
    }

    public CheckoutServiceImpl(CartService cartService, CartDAO cartDAO, ProductDAO productDAO, com.fashionstore.service.AddressService addressService) {
        this.cartService = cartService;
        this.cartDAO = cartDAO;
        this.productDAO = productDAO;
        this.addressService = addressService;
    }

    public void setCartService(CartService cartService) {
        if (this.cartService == null) {
            try {
                java.lang.reflect.Field field = CheckoutServiceImpl.class.getDeclaredField("cartService");
                field.setAccessible(true);
                field.set(this, cartService);
            } catch (Exception e) {
                logger.error("Failed to set cartService", e);
            }
        }
    }

    public void setCartDAO(CartDAO cartDAO) {
        if (this.cartDAO == null) {
            try {
                java.lang.reflect.Field field = CheckoutServiceImpl.class.getDeclaredField("cartDAO");
                field.setAccessible(true);
                field.set(this, cartDAO);
            } catch (Exception e) {
                logger.error("Failed to set cartDAO", e);
            }
        }
    }

    public void setAddressService(com.fashionstore.service.AddressService addressService) {
        if (this.addressService == null) {
            try {
                java.lang.reflect.Field field = CheckoutServiceImpl.class.getDeclaredField("addressService");
                field.setAccessible(true);
                field.set(this, addressService);
            } catch (Exception e) {
                logger.error("Failed to set addressService", e);
            }
        }
    }

    public void setCouponService(com.fashionstore.service.CouponService couponService) {
        if (this.couponService == null) {
            try {
                java.lang.reflect.Field field = CheckoutServiceImpl.class.getDeclaredField("couponService");
                field.setAccessible(true);
                field.set(this, couponService);
            } catch (Exception e) {
                logger.error("Failed to set couponService", e);
            }
        }
    }

    public void setProductDAO(ProductDAO productDAO) {
        if (this.productDAO == null) {
            try {
                java.lang.reflect.Field field = CheckoutServiceImpl.class.getDeclaredField("productDAO");
                field.setAccessible(true);
                field.set(this, productDAO);
            } catch (Exception e) {
                logger.error("Failed to set productDAO", e);
            }
        }
    }

    @Override
    public boolean validateCartForCheckout(int userId) {
        return cartService.validateCartForCheckout(userId);
    }

    @Override
    public List<CartItem> getCheckoutCartItems(int userId) {
        return cartService.getCartItems(userId);
    }

    @Override
    public Map<String, Double> calculateCheckoutTotals(int userId, String couponCode) {
        Map<String, Double> totals = new HashMap<>();
        
        try {
            // Step 1: Get cart subtotal from backend
            // This is the authoritative source - frontend cannot modify
            double subtotal = cartService.calculateCartTotal(userId);
            
            // Step 2: Apply coupon discount if valid
            double discount = 0.0;
            if (couponCode != null && !couponCode.trim().isEmpty() && couponService != null) {
                Coupon coupon = couponService.validateCoupon(couponCode.trim());
                if (coupon != null && couponService.isCouponValidForUser(userId, coupon)) {
                    discount = couponService.calculateDiscount(subtotal, coupon);
                }
            }
            
            // Step 3: Calculate subtotal after discount
            double afterDiscount = Math.max(0, subtotal - discount);
            
            // Step 4: Calculate shipping (backend rule: free above threshold)
            // Frontend cannot change shipping cost
            double shipping = afterDiscount >= SHIPPING_THRESHOLD ? 0.0 : SHIPPING_COST;
            
            // Step 5: Calculate tax (18% GST on subtotal after discount, before shipping)
            // Frontend cannot change tax rate
            double tax = afterDiscount * TAX_RATE;
            
            // Step 6: Calculate final total
            double total = afterDiscount + shipping + tax;
            
            // Step 7: Round all values to 2 decimal places for currency
            // Prevents floating point precision issues
            totals.put("subtotal", Math.round(subtotal * 100.0) / 100.0);
            totals.put("discount", Math.round(discount * 100.0) / 100.0);
            totals.put("afterDiscount", Math.round(afterDiscount * 100.0) / 100.0);
            totals.put("shipping", Math.round(shipping * 100.0) / 100.0);
            totals.put("tax", Math.round(tax * 100.0) / 100.0);
            totals.put("total", Math.round(total * 100.0) / 100.0);
            
        } catch (Exception e) {
            logger.error("Error calculating checkout totals for user {}: {}", userId, e.getMessage(), e);
            // Return default values
            totals.put("subtotal", 0.0);
            totals.put("discount", 0.0);
            totals.put("afterDiscount", 0.0);
            totals.put("shipping", 0.0);
            totals.put("tax", 0.0);
            totals.put("total", 0.0);
        }
        
        return totals;
    }

    @Override
    public boolean validateShippingAddress(Address address) {
        if (address == null) {
            return false;
        }
        
        // Basic validation - Validator class doesn't exist
        return address.getFullName() != null && !address.getFullName().trim().isEmpty()
            && address.getAddressLine1() != null && !address.getAddressLine1().trim().isEmpty()
            && address.getCity() != null && !address.getCity().trim().isEmpty()
            && address.getState() != null && !address.getState().trim().isEmpty()
            && address.getPostalCode() != null && !address.getPostalCode().trim().isEmpty()
            && address.getCountry() != null && !address.getCountry().trim().isEmpty();
    }

    @Override
    public boolean validateBillingAddress(Address address) {
        if (address == null) {
            return false;
        }
        
        // Basic validation - Validator class doesn't exist
        return address.getFullName() != null && !address.getFullName().trim().isEmpty()
            && address.getAddressLine1() != null && !address.getAddressLine1().trim().isEmpty()
            && address.getCity() != null && !address.getCity().trim().isEmpty()
            && address.getState() != null && !address.getState().trim().isEmpty()
            && address.getPostalCode() != null && !address.getPostalCode().trim().isEmpty()
            && address.getCountry() != null && !address.getCountry().trim().isEmpty();
    }

    @Override
    public List<Address> getUserCheckoutAddresses(int userId) {
        if (addressService == null) {
            logger.warn("AddressService not initialized");
            return new ArrayList<>();
        }
        return addressService.getAddressesByUserId(userId);
    }

    @Override
    public Address getDefaultShippingAddress(int userId) {
        if (addressService == null) {
            logger.warn("AddressService not initialized");
            return null;
        }
        return addressService.getDefaultAddress(userId, "shipping");
    }

    @Override
    public Address getDefaultBillingAddress(int userId) {
        if (addressService == null) {
            logger.warn("AddressService not initialized");
            return null;
        }
        return addressService.getDefaultAddress(userId, "billing");
    }

    @Override
    public Order prepareOrderForPayment(int userId, Address shippingAddress, Address billingAddress, String couponCode) {
        try {
            // Validate cart
            if (!validateCartForCheckout(userId)) {
                logger.warn("Cart validation failed for user: {}", userId);
                return null;
            }

            // Validate addresses
            if (!validateShippingAddress(shippingAddress) || !validateBillingAddress(billingAddress)) {
                logger.warn("Address validation failed for user: {}", userId);
                return null;
            }

            // Get cart items
            List<CartItem> cartItems = getCheckoutCartItems(userId);
            if (cartItems.isEmpty()) {
                logger.warn("Empty cart for user: {}", userId);
                return null;
            }

            // Calculate totals
            Map<String, Double> totals = calculateCheckoutTotals(userId, couponCode);

            // Create order
            Order order = new Order();
            order.setUserId(userId);
            order.setFullName(shippingAddress.getFullName());
            order.setAddress(shippingAddress.getAddressLine1() + " " + shippingAddress.getAddressLine2());
            order.setCity(shippingAddress.getCity());
            order.setState(shippingAddress.getState());
            order.setZip(shippingAddress.getPostalCode());
            order.setPhone(shippingAddress.getPhone());
            order.setTotalAmount(totals.get("total"));
            order.setStatus("Pending");
            order.setOrderDate(new java.sql.Timestamp(System.currentTimeMillis()));

            return order;

        } catch (Exception e) {
            logger.error("Error preparing order for payment: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean validateCheckoutData(int userId, Map<String, Object> checkoutData) {
        if (checkoutData == null || checkoutData.isEmpty()) {
            return false;
        }

        try {
            // Validate cart
            if (!validateCartForCheckout(userId)) {
                return false;
            }

            // Validate shipping address
            Object shippingAddr = checkoutData.get("shippingAddress");
            if (shippingAddr instanceof Address) {
                if (!validateShippingAddress((Address) shippingAddr)) {
                    return false;
                }
            }

            // Validate billing address
            Object billingAddr = checkoutData.get("billingAddress");
            if (billingAddr instanceof Address) {
                if (!validateBillingAddress((Address) billingAddr)) {
                    return false;
                }
            }

            // Validate payment method
            String paymentMethod = (String) checkoutData.get("paymentMethod");
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("Error validating checkout data: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean applyCouponToCheckout(int userId, String couponCode) {
        if (couponService == null) {
            logger.warn("CouponService not initialized");
            return false;
        }
        return couponService.applyCoupon(userId, couponCode);
    }

    @Override
    public boolean removeCouponFromCheckout(int userId) {
        if (couponService == null) {
            logger.warn("CouponService not initialized");
            return false;
        }
        return couponService.removeCoupon(userId);
    }

    @Override
    public Order processCheckoutOrder(int userId, Map<String, Object> checkoutData) throws Exception {
        String paymentMethod = (String) checkoutData.get("paymentMethod");
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }

        Map<String, Object> shippingAddressData = (Map<String, Object>) checkoutData.get("shippingAddress");
        if (shippingAddressData == null) {
            throw new IllegalArgumentException("Shipping address is required");
        }

        String[] requiredFields = {"fullName", "addressLine1", "city", "state", "postalCode", "phone"};
        for (String field : requiredFields) {
            if (shippingAddressData.get(field) == null || ((String) shippingAddressData.get(field)).trim().isEmpty()) {
                throw new IllegalArgumentException(field + " is required");
            }
        }

        List<CartItem> cartItems = cartService.getCartItems(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty");
        }

        if (!cartService.validateCartForCheckout(userId)) {
            throw new IllegalArgumentException("Some items in your cart are not available");
        }

        // Inventory validation not available - InventoryService doesn't exist

        String couponCode = (String) checkoutData.get("couponCode");
        Map<String, Double> totals = calculateCheckoutTotals(userId, couponCode);
        if (totals == null || totals.isEmpty()) {
            throw new IllegalStateException("Failed to calculate order totals");
        }

        Double totalAmount = totals.get("total");
        if (totalAmount == null || totalAmount <= 0) {
            throw new IllegalArgumentException("Invalid order total");
        }

        // ProductSize validation not available - InventoryService doesn't exist

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("fullName", shippingAddressData.get("fullName"));
        
        String addressLine1 = (String) shippingAddressData.get("addressLine1");
        String addressLine2 = (String) shippingAddressData.get("addressLine2");
        String fullAddress = addressLine1 + (addressLine2 != null && !addressLine2.isEmpty() ? " " + addressLine2 : "");
        orderData.put("address", fullAddress);
        
        orderData.put("city", shippingAddressData.get("city"));
        orderData.put("state", shippingAddressData.get("state"));
        orderData.put("zip", shippingAddressData.get("postalCode"));
        orderData.put("phone", shippingAddressData.get("phone"));
        orderData.put("paymentMethod", paymentMethod);
        orderData.put("totalAmount", totalAmount);

        List<Map<String, Object>> itemsData = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("productId", item.getProductId());
            itemData.put("quantity", item.getQuantity());
            itemData.put("price", item.getPrice());
            itemData.put("sizeLabel", item.getSizeLabel());
            itemsData.add(itemData);
        }
        orderData.put("items", itemsData);

        OrderService orderService = ServiceRegistry.getInstance().getOrderService();
        Order order = null;
        try {
            order = orderService.createOrder(userId, orderData);
        } catch (Exception e) {
            // Inventory rollback not available - InventoryService doesn't exist
            throw new IllegalStateException("Failed to create order. Please try again.", e);
        }

        if (order == null) {
            // Inventory rollback not available - InventoryService doesn't exist
            throw new IllegalStateException("Failed to create order. Please try again.");
        }

        return order;
    }
}
