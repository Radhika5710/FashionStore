-- ====================================================
-- FASHIONSTORE DATABASE PERFORMANCE OPTIMIZATION
-- ====================================================
-- This script contains optimized SQL queries, indexes, and performance improvements
-- for production scalability while maintaining current architecture

-- 1. COMPREHENSIVE INDEXING STRATEGY
-- ====================================================

-- Core table indexes for high-performance queries
CREATE INDEX IF NOT EXISTS idx_users_email_active ON users(email, is_active);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_full_name ON users(full_name);

-- Products optimization indexes
CREATE INDEX IF NOT EXISTS idx_products_category_active_price ON products(category_id, is_active, price);
CREATE INDEX IF NOT EXISTS idx_products_name_search ON products(product_name);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at);
CREATE INDEX IF NOT EXISTS idx_products_stock_quantity ON products(stock_quantity);

-- Orders optimization indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_status_date ON orders(user_id, order_status, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_created_at_status ON orders(created_at, order_status);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status ON orders(payment_status);
CREATE INDEX IF NOT EXISTS idx_orders_total_amount ON orders(total_amount);

-- Order items optimization
CREATE INDEX IF NOT EXISTS idx_order_items_order_product ON order_items(order_id, product_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_order_items_quantity_price ON order_items(quantity, unit_price);

-- Cart optimization indexes
CREATE INDEX IF NOT EXISTS idx_cart_user_session ON cart_items(user_id, session_id);
CREATE INDEX IF NOT EXISTS idx_cart_created_at ON cart_items(created_at);
CREATE INDEX IF NOT EXISTS idx_cart_product_quantity ON cart_items(product_id, quantity);

-- Categories optimization
CREATE INDEX IF NOT EXISTS idx_categories_parent_active ON categories(parent_category_id, is_active);
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(category_name);

-- Reviews optimization indexes
CREATE INDEX IF NOT EXISTS idx_reviews_product_rating ON reviews(product_id, rating);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews(created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_user_product ON reviews(user_id, product_id);

-- Wishlist optimization indexes
CREATE INDEX IF NOT EXISTS idx_wishlist_user_product ON wishlist(user_id, product_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_created_at ON wishlist(created_at);

-- Admin tables optimization
CREATE INDEX IF NOT EXISTS idx_admin_users_user_active ON admin_users(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_admin_users_role ON admin_users(role_id);
CREATE INDEX IF NOT EXISTS idx_admin_activity_user_date ON admin_activity_log(admin_user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_analytics_daily_date ON analytics_daily(date);
CREATE INDEX IF NOT EXISTS idx_analytics_hourly_date ON analytics_hourly(date_hour);

-- Composite indexes for complex queries
CREATE INDEX IF NOT EXISTS idx_products_search ON products(is_active, category_id, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_analytics ON orders(created_at, order_status, total_amount);
CREATE INDEX IF NOT EXISTS idx_users_orders ON users(user_id, created_at);

-- 2. PARTITIONING FOR LARGE TABLES
-- ====================================================

-- Partition orders table by month for better performance
-- (This is optional for very large datasets)
/*
ALTER TABLE orders PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202401 VALUES LESS THAN (202402),
    PARTITION p202402 VALUES LESS THAN (202403),
    PARTITION p202403 VALUES LESS THAN (202404),
    -- Add more partitions as needed
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
*/

-- 3. OPTIMIZED VIEWS FOR COMMON QUERIES
-- ====================================================

-- Optimized view for product listings with category names
CREATE OR REPLACE VIEW vw_products_with_category AS
SELECT 
    p.product_id,
    p.product_name,
    p.description,
    p.price,
    p.stock_quantity,
    p.image_url,
    p.is_active,
    p.created_at,
    p.updated_at,
    c.category_id,
    c.category_name,
    c.parent_category_id
FROM products p
LEFT JOIN categories c ON p.category_id = c.category_id
WHERE p.is_active = 1;

-- Optimized view for order details with customer info
CREATE OR REPLACE VIEW vw_order_details AS
SELECT 
    o.order_id,
    o.user_id,
    u.full_name as customer_name,
    u.email as customer_email,
    o.order_status,
    o.payment_status,
    o.total_amount,
    o.shipping_address,
    o.billing_address,
    o.created_at,
    o.updated_at,
    COUNT(oi.order_item_id) as item_count
FROM orders o
INNER JOIN users u ON o.user_id = u.user_id
LEFT JOIN order_items oi ON o.order_id = oi.order_id
GROUP BY o.order_id, o.user_id, u.full_name, u.email, o.order_status, 
         o.payment_status, o.total_amount, o.shipping_address, o.billing_address, 
         o.created_at, o.updated_at;

-- Optimized view for top-selling products
CREATE OR REPLACE VIEW vw_top_products AS
SELECT 
    p.product_id,
    p.product_name,
    p.price,
    p.image_url,
    c.category_name,
    COALESCE(SUM(oi.quantity), 0) as total_sold,
    COALESCE(SUM(oi.total_price), 0) as total_revenue,
    COUNT(DISTINCT o.order_id) as order_count
FROM products p
LEFT JOIN categories c ON p.category_id = c.category_id
LEFT JOIN order_items oi ON p.product_id = oi.product_id
LEFT JOIN orders o ON oi.order_id = o.order_id 
    AND o.order_status NOT IN ('cancelled', 'returned')
WHERE p.is_active = 1
GROUP BY p.product_id, p.product_name, p.price, p.image_url, c.category_name
ORDER BY total_sold DESC;

-- 4. STORED PROCEDURES FOR OPTIMIZED OPERATIONS
-- ====================================================

-- Optimized procedure for getting products with pagination
DELIMITER //
CREATE PROCEDURE sp_get_products_paginated(
    IN p_page INT,
    IN p_limit INT,
    IN p_category_id INT,
    IN p_search VARCHAR(255),
    IN p_sort_by VARCHAR(50),
    IN p_sort_order VARCHAR(10)
)
BEGIN
    DECLARE v_offset INT;
    SET v_offset = (p_page - 1) * p_limit;
    
    SET @sql = CONCAT(
        'SELECT SQL_CALC_FOUND_ROWS 
         p.product_id, p.product_name, p.price, p.stock_quantity, 
         p.image_url, p.is_active, p.created_at,
         c.category_name,
         COALESCE(AVG(r.rating), 0) as avg_rating,
         COUNT(DISTINCT r.review_id) as review_count
         FROM products p
         LEFT JOIN categories c ON p.category_id = c.category_id
         LEFT JOIN reviews r ON p.product_id = r.product_id
         WHERE p.is_active = 1'
    );
    
    IF p_category_id IS NOT NULL AND p_category_id > 0 THEN
        SET @sql = CONCAT(@sql, ' AND p.category_id = ', p_category_id);
    END IF;
    
    IF p_search IS NOT NULL AND p_search != '' THEN
        SET @sql = CONCAT(@sql, ' AND (p.product_name LIKE ''%', p_search, '%'' OR p.description LIKE ''%', p_search, '%'')');
    END IF;
    
    SET @sql = CONCAT(@sql, ' GROUP BY p.product_id, p.product_name, p.price, p.stock_quantity, p.image_url, p.is_active, p.created_at, c.category_name');
    
    IF p_sort_by IS NOT NULL AND p_sort_by != '' THEN
        SET @sql = CONCAT(@sql, ' ORDER BY ', p_sort_by, ' ', p_sort_order);
    ELSE
        SET @sql = CONCAT(@sql, ' ORDER BY p.created_at DESC');
    END IF;
    
    SET @sql = CONCAT(@sql, ' LIMIT ', p_limit, ' OFFSET ', v_offset);
    
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    
    -- Get total count for pagination
    SELECT FOUND_ROWS() as total_count;
END //
DELIMITER ;

-- Optimized procedure for getting user orders with items
DELIMITER //
CREATE PROCEDURE sp_get_user_orders_with_items(
    IN p_user_id INT,
    IN p_page INT,
    IN p_limit INT
)
BEGIN
    DECLARE v_offset INT;
    SET v_offset = (p_page - 1) * p_limit;
    
    SELECT 
        o.order_id,
        o.order_status,
        o.payment_status,
        o.total_amount,
        o.shipping_address,
        o.created_at,
        o.updated_at,
        COUNT(oi.order_item_id) as item_count,
        GROUP_CONCAT(
            CONCAT(oi.product_id, ':', oi.quantity, ':', oi.unit_price) 
            ORDER BY oi.order_item_id
            SEPARATOR '|'
        ) as items_summary
    FROM orders o
    LEFT JOIN order_items oi ON o.order_id = oi.order_id
    WHERE o.user_id = p_user_id
    GROUP BY o.order_id, o.order_status, o.payment_status, o.total_amount, 
             o.shipping_address, o.created_at, o.updated_at
    ORDER BY o.created_at DESC
    LIMIT p_limit OFFSET v_offset;
END //
DELIMITER ;

-- 5. PERFORMANCE MONITORING QUERIES
-- ====================================================

-- Query to identify slow queries
SELECT 
    DIGEST_TEXT,
    COUNT_STAR,
    AVG_TIMER_WAIT/1000000000 AS avg_time_seconds,
    MAX_TIMER_WAIT/1000000000 AS max_time_seconds,
    SUM_ROWS_EXAMINED/COUNT_STAR AS avg_rows_examined
FROM performance_schema.events_statements_summary_by_digest 
WHERE AVG_TIMER_WAIT/1000000000 > 0.5
ORDER BY AVG_TIMER_WAIT DESC
LIMIT 10;

-- Query to check index usage
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    PACKED,
    NULLABLE,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'fashionstore'
ORDER BY TABLE_NAME, SEQ_IN_INDEX;

-- Query to identify missing indexes
SELECT 
    t.TABLE_NAME,
    t.TABLE_ROWS,
    s.INDEX_NAME,
    s.CARDINALITY
FROM information_schema.TABLES t
LEFT JOIN information_schema.STATISTICS s ON t.TABLE_NAME = s.TABLE_NAME
WHERE t.TABLE_SCHEMA = 'fashionstore'
    AND t.TABLE_ROWS > 1000
    AND s.INDEX_NAME IS NULL
ORDER BY t.TABLE_ROWS DESC;

-- 6. DATABASE MAINTENANCE PROCEDURES
-- ====================================================

-- Procedure to optimize tables
DELIMITER //
CREATE PROCEDURE sp_optimize_tables()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE table_name VARCHAR(255);
    DECLARE cur CURSOR FOR 
        SELECT TABLE_NAME 
        FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = 'fashionstore' 
        AND TABLE_TYPE = 'BASE TABLE';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO table_name;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET @sql = CONCAT('OPTIMIZE TABLE ', table_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SET @sql = CONCAT('ANALYZE TABLE ', table_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
    END LOOP;
    
    CLOSE cur;
END //
DELIMITER ;

-- 7. QUERY OPTIMIZATION EXAMPLES
-- ====================================================

-- Optimized product search query (replaces multiple queries)
SELECT 
    p.product_id,
    p.product_name,
    p.price,
    p.stock_quantity,
    p.image_url,
    c.category_name,
    COALESCE(AVG(r.rating), 0) as avg_rating,
    COUNT(DISTINCT r.review_id) as review_count,
    (SELECT COUNT(*) FROM order_items oi2 
     WHERE oi2.product_id = p.product_id) as order_count
FROM products p
LEFT JOIN categories c ON p.category_id = c.category_id
LEFT JOIN reviews r ON p.product_id = r.product_id
WHERE p.is_active = 1
    AND (p.product_name LIKE CONCAT('%', ?, '%') OR p.description LIKE CONCAT('%', ?, '%'))
    AND (? IS NULL OR p.category_id = ?)
GROUP BY p.product_id, p.product_name, p.price, p.stock_quantity, p.image_url, c.category_name
ORDER BY 
    CASE 
        WHEN ? = 'price' THEN p.price
        WHEN ? = 'name' THEN p.product_name
        WHEN ? = 'created' THEN p.created_at
        ELSE p.created_at
    END
    CASE WHEN ? = 'desc' THEN DESC ELSE END
LIMIT ? OFFSET ?;

-- Optimized order analytics query
SELECT 
    DATE(o.created_at) as order_date,
    COUNT(*) as order_count,
    SUM(o.total_amount) as total_revenue,
    AVG(o.total_amount) as avg_order_value,
    COUNT(DISTINCT o.user_id) as unique_customers,
    SUM(CASE WHEN o.order_status = 'order_delivered' THEN 1 ELSE 0 END) as delivered_orders
FROM orders o
WHERE o.created_at BETWEEN ? AND ?
    AND o.order_status NOT IN ('cancelled')
GROUP BY DATE(o.created_at)
ORDER BY order_date DESC;

-- Optimized customer analytics query
SELECT 
    u.user_id,
    u.full_name,
    u.email,
    COUNT(DISTINCT o.order_id) as total_orders,
    COALESCE(SUM(o.total_amount), 0) as total_spent,
    COALESCE(AVG(o.total_amount), 0) as avg_order_value,
    MAX(o.created_at) as last_order_date,
    DATEDIFF(CURRENT_DATE, u.created_at) as customer_age_days,
    CASE 
        WHEN COUNT(DISTINCT o.order_id) = 0 THEN 'new'
        WHEN COUNT(DISTINCT o.order_id) <= 2 THEN 'occasional'
        WHEN COUNT(DISTINCT o.order_id) <= 5 THEN 'regular'
        ELSE 'vip'
    END as customer_segment
FROM users u
LEFT JOIN orders o ON u.user_id = o.user_id 
    AND o.order_status NOT IN ('cancelled', 'returned')
WHERE u.is_active = 1
GROUP BY u.user_id, u.full_name, u.email, u.created_at
HAVING total_orders > 0 OR total_spent > 0
ORDER BY total_spent DESC;

-- 8. CONFIGURATION RECOMMENDATIONS
-- ====================================================

-- MySQL configuration optimizations for production
/*
-- Add to my.cnf
[mysqld]
# InnoDB optimizations
innodb_buffer_pool_size = 2G  # 70-80% of available RAM
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
innodb_file_per_table = 1

# Query cache (deprecated in MySQL 8.0, use caching layer instead)
# query_cache_type = 1
# query_cache_size = 256M

# Connection settings
max_connections = 500
max_connect_errors = 1000
wait_timeout = 300
interactive_timeout = 300

# Performance schema
performance_schema = ON
performance_schema_max_table_handles = 4000
performance_schema_max_table_instances = 12500

# Slow query log
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 0.5
log_queries_not_using_indexes = 1

# Binary logging for replication
log-bin = mysql-bin
binlog_format = ROW
expire_logs_days = 7
max_binlog_size = 100M

# General query log (disable in production)
# general_log = 0
# general_log_file = /var/log/mysql/general.log
*/

-- 9. BENCHMARKING QUERIES
-- ====================================================

-- Performance benchmark for product queries
SELECT 
    'Product Search Benchmark' as test_name,
    COUNT(*) as total_products,
    AVG(TIMESTAMPDIFF(MICROSECOND, start_time, end_time) / 1000) as avg_time_ms
FROM (
    SELECT 
        p.product_id,
        NOW() as start_time,
        (SELECT NOW()) as end_time
    FROM products p
    WHERE p.is_active = 1
    LIMIT 1000
) benchmark_query;

-- Performance benchmark for order queries
SELECT 
    'Order Analytics Benchmark' as test_name,
    COUNT(*) as total_orders,
    AVG(TIMESTAMPDIFF(MICROSECOND, start_time, end_time) / 1000) as avg_time_ms
FROM (
    SELECT 
        o.order_id,
        NOW() as start_time,
        (SELECT NOW()) as end_time
    FROM orders o
    WHERE o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    LIMIT 1000
) benchmark_query;

-- 10. CLEANUP AND MAINTENANCE
-- ====================================================

-- Clean up old session data
DELETE FROM user_sessions WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- Clean up old cart items (older than 30 days)
DELETE FROM cart_items WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- Update product statistics
UPDATE products p 
SET 
    p.view_count = COALESCE(
        (SELECT COUNT(*) FROM product_views pv 
         WHERE pv.product_id = p.product_id 
         AND pv.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)), 0
    ),
    p.updated_at = NOW()
WHERE p.updated_at < DATE_SUB(NOW(), INTERVAL 1 DAY);

-- Rebuild fragmented tables
OPTIMIZE TABLE orders;
OPTIMIZE TABLE order_items;
OPTIMIZE TABLE products;
OPTIMIZE TABLE cart_items;
