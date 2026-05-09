package com.fashionstore.cache;

public class CacheKey {
    private static final String PREFIX = "fashionstore:";
    private static final String SEPARATOR = ":";
    
    public static String product(int productId) {
        return PREFIX + "product" + SEPARATOR + productId;
    }
    
    public static String products(String key) {
        return PREFIX + "products" + SEPARATOR + key;
    }
    
    public static String category(int categoryId) {
        return PREFIX + "category" + SEPARATOR + categoryId;
    }
    
    public static String categories(String key) {
        return PREFIX + "categories" + SEPARATOR + key;
    }
    
    public static String featuredProducts() {
        return PREFIX + "featured:products";
    }
    
    public static String trendingProducts() {
        return PREFIX + "trending:products";
    }
    
    public static String userCart(int userId) {
        return PREFIX + "cart" + SEPARATOR + userId;
    }
    
    public static String userWishlist(int userId) {
        return PREFIX + "wishlist" + SEPARATOR + userId;
    }
    
    public static String searchResults(String query) {
        return PREFIX + "search" + SEPARATOR + query.hashCode();
    }
    
    public static String recommendations(int productId) {
        return PREFIX + "recommendations" + SEPARATOR + productId;
    }
    
    public static String adminStats() {
        return PREFIX + "admin:stats";
    }
    
    public static String order(int orderId) {
        return PREFIX + "order" + SEPARATOR + orderId;
    }
    
    public static String userOrders(int userId) {
        return PREFIX + "orders" + SEPARATOR + userId;
    }
}
