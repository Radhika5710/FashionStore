-- ============================================================
-- FASHIONSTORE COMPLETE DATABASE SCHEMA
-- Single-file setup for fresh installations
-- Merged from: schema.sql + all database/migration_*.sql files
-- Created: May 8, 2026
-- ============================================================

DROP DATABASE IF EXISTS fashionstore;
CREATE DATABASE fashionstore;
USE fashionstore;

-- ============================================================
-- 1. CORE TABLES
-- ============================================================

-- USERS
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(15),
    password VARCHAR(255) NOT NULL,
    gender VARCHAR(10),
    address TEXT,
    role VARCHAR(20) DEFAULT 'customer',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User accounts with role-based access control';

-- CATEGORIES
CREATE TABLE categories (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_category_id INT DEFAULT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_category_id) REFERENCES categories(category_id) ON DELETE SET NULL,
    UNIQUE KEY uq_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PRODUCTS
CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    discount_percent DECIMAL(5,2) DEFAULT 0,
    image_url VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    is_new BOOLEAN DEFAULT FALSE,
    is_sale BOOLEAN DEFAULT FALSE,
    is_trending BOOLEAN DEFAULT FALSE,
    brand VARCHAR(100),
    stock_quantity INT DEFAULT 0,
    category_id INT NOT NULL,
    popular_score DECIMAL(5,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_product_price_positive CHECK (price >= 0),
    CONSTRAINT chk_product_stock_non_negative CHECK (stock_quantity >= 0),
    CONSTRAINT chk_product_discount_valid CHECK (discount_percent >= 0 AND discount_percent <= 100),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_products_category_active_price (category_id, active, price),
    INDEX idx_products_name (product_name),
    INDEX idx_products_brand (brand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product catalog with inventory tracking';

-- PRODUCT SIZES
CREATE TABLE product_sizes (
    product_size_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    size_label VARCHAR(20) NOT NULL,
    stock_quantity INT DEFAULT 0,
    sku_code VARCHAR(50),
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id),
    INDEX idx_size_label (size_label),
    UNIQUE KEY idx_product_size (product_id, size_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. SHOPPING TABLES
-- ============================================================

-- CART ITEMS
CREATE TABLE cart_items (
    cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    size_label VARCHAR(10),
    quantity INT DEFAULT 1,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_cart_user_product_size UNIQUE (user_id, product_id, size_label),
    CONSTRAINT chk_cart_quantity_positive CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- WISHLIST
CREATE TABLE wishlist_items (
    wishlist_item_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SAVED ITEMS (Save for Later)
CREATE TABLE saved_items (
    saved_item_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    size_label VARCHAR(10),
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    UNIQUE KEY unique_saved_item (user_id, product_id, size_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 3. ORDER TABLES
-- ============================================================

-- ADDRESSES
CREATE TABLE addresses (
    address_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    address_type ENUM('billing', 'shipping', 'both') DEFAULT 'both',
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_addresses (user_id),
    INDEX idx_user_default (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ORDERS
CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    billing_address_id INT,
    shipping_address_id INT,
    payment_method_id INT,
    coupon_id INT,
    full_name VARCHAR(100),
    address TEXT,
    city VARCHAR(50),
    state VARCHAR(50),
    zip VARCHAR(10),
    phone VARCHAR(15),
    payment_method VARCHAR(20),
    status VARCHAR(20) DEFAULT 'PLACED',
    shipping_status VARCHAR(50) DEFAULT 'PROCESSING',
    tracking_number VARCHAR(100),
    estimated_delivery DATE,
    actual_delivery DATE,
    notes TEXT,
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(10,2) DEFAULT 0.00,
    shipping_fee DECIMAL(10,2) DEFAULT 0.00,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_status ENUM('pending', 'processing', 'completed', 'failed', 'refunded') DEFAULT 'pending',
    payment_gateway VARCHAR(50),
    transaction_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (billing_address_id) REFERENCES addresses(address_id) ON DELETE SET NULL,
    FOREIGN KEY (shipping_address_id) REFERENCES addresses(address_id) ON DELETE SET NULL,
    CONSTRAINT chk_order_total_positive CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Order transactions with status tracking';

-- ORDER ITEMS
CREATE TABLE order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    size_label VARCHAR(10),
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_order_item_price_positive CHECK (price >= 0),
    CONSTRAINT chk_order_item_quantity_positive CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Individual items within orders';

-- ORDER STATUS HISTORY
CREATE TABLE order_status_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. PAYMENT TABLES
-- ============================================================

-- PAYMENTS
CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    status VARCHAR(50) DEFAULT 'PENDING',
    gateway_response TEXT,
    payment_signature VARCHAR(255),
    webhook_id VARCHAR(255),
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_status (status),
    INDEX idx_verified (verified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Payment transactions with verification';

-- PAYMENT METHODS
CREATE TABLE payment_methods (
    payment_method_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    method_type ENUM('credit_card', 'debit_card', 'upi', 'net_banking', 'wallet') NOT NULL,
    provider VARCHAR(50) NOT NULL,
    method_alias VARCHAR(100),
    last_four VARCHAR(4),
    expiry_month INT,
    expiry_year INT,
    card_brand VARCHAR(50),
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    gateway_token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_payment_methods (user_id),
    INDEX idx_user_default (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PAYMENT TRANSACTIONS
CREATE TABLE payment_transactions (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    payment_method_id INT,
    gateway_transaction_id VARCHAR(255) NOT NULL,
    gateway VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    status ENUM('pending', 'processing', 'completed', 'failed', 'cancelled', 'refunded') NOT NULL,
    payment_method_type VARCHAR(50),
    gateway_response TEXT,
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(payment_method_id) ON DELETE SET NULL,
    INDEX idx_transaction_order (order_id),
    INDEX idx_transaction_gateway (gateway, status),
    INDEX idx_transaction_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PASSWORD RESET TOKENS
CREATE TABLE password_reset_tokens (
    token_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. COUPON & DISCOUNT TABLES
-- ============================================================

-- COUPONS
CREATE TABLE coupons (
    coupon_id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    discount_type ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    minimum_order_amount DECIMAL(10,2) DEFAULT 0,
    maximum_discount_amount DECIMAL(10,2) DEFAULT NULL,
    usage_limit INT DEFAULT NULL,
    usage_count INT DEFAULT 0,
    user_usage_limit INT DEFAULT 0,
    valid_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_coupon_code (code),
    INDEX idx_coupon_active (is_active, valid_from, valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- COUPON USAGE
CREATE TABLE coupon_usage (
    coupon_usage_id INT PRIMARY KEY AUTO_INCREMENT,
    coupon_id INT NOT NULL,
    user_id INT NOT NULL,
    order_id INT NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    UNIQUE KEY unique_coupon_user_order (coupon_id, user_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 6. REVIEWS & RATINGS
-- ============================================================

CREATE TABLE reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    product_id INT,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 7. SEARCH & ANALYTICS TABLES
-- ============================================================

-- SEARCH HISTORY
CREATE TABLE search_history (
    search_history_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    search_query VARCHAR(255) NOT NULL,
    search_type VARCHAR(50) DEFAULT 'KEYWORD',
    filters_applied TEXT,
    results_count INT DEFAULT 0,
    clicked_product_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (clicked_product_id) REFERENCES products(product_id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_search_query (search_query),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- RECENTLY VIEWED
CREATE TABLE recently_viewed (
    recently_viewed_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    UNIQUE KEY idx_user_product (user_id, product_id),
    INDEX idx_viewed_at (viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SEARCH ANALYTICS
CREATE TABLE search_analytics (
    analytics_id INT AUTO_INCREMENT PRIMARY KEY,
    search_query VARCHAR(255) NOT NULL,
    search_count INT DEFAULT 0,
    result_count_avg DECIMAL(10, 2) DEFAULT 0.00,
    click_through_rate DECIMAL(5, 2) DEFAULT 0.00,
    last_searched TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY idx_search_query (search_query),
    INDEX idx_search_count (search_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PRODUCT ATTRIBUTES
CREATE TABLE product_attributes (
    attribute_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    attribute_name VARCHAR(50) NOT NULL,
    attribute_value VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id),
    INDEX idx_attribute_name_value (attribute_name, attribute_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PRODUCT RECOMMENDATIONS
CREATE TABLE product_recommendations (
    recommendation_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    recommended_product_id INT NOT NULL,
    recommendation_type VARCHAR(50) NOT NULL,
    score DECIMAL(5, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (recommended_product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id),
    INDEX idx_recommendation_type (recommendation_type),
    INDEX idx_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 8. SHIPPING & TAX TABLES
-- ============================================================

-- SHIPPING ZONES
CREATE TABLE shipping_zones (
    zone_id INT PRIMARY KEY AUTO_INCREMENT,
    zone_name VARCHAR(100) NOT NULL,
    countries TEXT NOT NULL,
    base_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    free_shipping_threshold DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SHIPPING RATES
CREATE TABLE shipping_rates (
    rate_id INT PRIMARY KEY AUTO_INCREMENT,
    zone_id INT NOT NULL,
    weight_min DECIMAL(8,2) DEFAULT 0.00,
    weight_max DECIMAL(8,2),
    rate DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES shipping_zones(zone_id) ON DELETE CASCADE,
    INDEX idx_zone_rates (zone_id, weight_min)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TAX RATES
CREATE TABLE tax_rates (
    tax_id INT PRIMARY KEY AUTO_INCREMENT,
    country VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    city VARCHAR(100),
    postal_code_prefix VARCHAR(10),
    tax_rate DECIMAL(5,4) NOT NULL,
    tax_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tax_location (country, state, city),
    INDEX idx_tax_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 9. AUDIT & LOG TABLES
-- ============================================================

-- EMAIL LOGS
CREATE TABLE email_logs (
    email_log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    email_type VARCHAR(50) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    error_message TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_email_type (email_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- EMAIL NOTIFICATIONS
CREATE TABLE email_notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    order_id INT,
    notification_type ENUM('order_confirmation', 'order_shipped', 'order_delivered', 'payment_failed', 'refund_processed', 'coupon_applied', 'password_reset') NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    status ENUM('pending', 'sent', 'failed', 'bounced') DEFAULT 'pending',
    sent_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE SET NULL,
    INDEX idx_email_user (user_id),
    INDEX idx_email_status (status),
    INDEX idx_email_type (notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 10. REFUND & INVOICE TABLES
-- ============================================================

-- REFUNDS
CREATE TABLE refunds (
    refund_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    refund_amount DECIMAL(10,2) NOT NULL,
    refund_reason VARCHAR(255) NOT NULL,
    refund_type ENUM('full', 'partial') NOT NULL,
    refund_status ENUM('requested', 'approved', 'processing', 'completed', 'rejected') DEFAULT 'requested',
    refund_method VARCHAR(50),
    gateway_refund_id VARCHAR(255),
    admin_notes TEXT,
    user_notes TEXT,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_refund_order (order_id),
    INDEX idx_refund_status (refund_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- INVOICES
CREATE TABLE invoices (
    invoice_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP,
    subtotal DECIMAL(10,2) NOT NULL,
    tax_amount DECIMAL(10,2) DEFAULT 0.00,
    shipping_fee DECIMAL(10,2) DEFAULT 0.00,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('draft', 'sent', 'paid', 'overdue', 'cancelled') DEFAULT 'draft',
    pdf_path VARCHAR(255),
    sent_at TIMESTAMP,
    paid_at TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_invoice_order (order_id),
    INDEX idx_invoice_number (invoice_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 11. INDEXES
-- ============================================================

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_cart_user ON cart_items(user_id);
CREATE INDEX idx_order_user ON orders(user_id);

CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand);
CREATE INDEX idx_products_new ON products(is_new);
CREATE INDEX idx_products_sale ON products(is_sale);
CREATE INDEX idx_products_trending ON products(is_trending);
CREATE INDEX idx_products_active_stock ON products(active, stock_quantity);
CREATE INDEX idx_products_price_range ON products(price, active);
CREATE INDEX idx_products_created_active ON products(created_at DESC, active);
CREATE INDEX idx_products_trending_score ON products(is_trending DESC, popular_score DESC);

CREATE INDEX idx_orders_status_date ON orders(status, created_at DESC);
CREATE INDEX idx_orders_user_date ON orders(user_id, created_at DESC);

CREATE INDEX idx_product_sizes_product ON product_sizes(product_id, size_label);
CREATE INDEX idx_addresses_user_default ON addresses(user_id, is_default);
CREATE INDEX idx_coupons_code_active ON coupons(code, is_active);

-- FULLTEXT indexes for search
CREATE FULLTEXT INDEX idx_product_name_fulltext ON products(product_name);
CREATE FULLTEXT INDEX idx_product_description_fulltext ON products(description);
CREATE FULLTEXT INDEX idx_product_search ON products(product_name, description, brand);

-- ============================================================
-- 12. VIEWS
-- ============================================================

CREATE OR REPLACE VIEW v_trending_products AS
SELECT p.*
FROM products p
WHERE p.active = TRUE
ORDER BY p.popular_score DESC, p.created_at DESC
LIMIT 50;

CREATE OR REPLACE VIEW v_low_stock_products AS
SELECT
    p.product_id,
    p.product_name,
    p.stock_quantity,
    p.brand,
    p.price,
    CASE
        WHEN p.stock_quantity = 0 THEN 'OUT_OF_STOCK'
        WHEN p.stock_quantity <= 5 THEN 'CRITICAL'
        WHEN p.stock_quantity <= 10 THEN 'LOW'
        ELSE 'NORMAL'
    END as stock_status
FROM products p
WHERE p.active = TRUE AND p.stock_quantity <= 10
ORDER BY p.stock_quantity ASC;

CREATE OR REPLACE VIEW v_pending_orders AS
SELECT
    o.order_id,
    o.user_id,
    u.full_name,
    o.total_amount,
    o.status,
    o.shipping_status,
    o.created_at as order_date,
    TIMESTAMPDIFF(HOUR, o.created_at, NOW()) as hours_pending
FROM orders o
JOIN users u ON o.user_id = u.user_id
WHERE o.status IN ('PENDING', 'PROCESSING')
ORDER BY o.created_at ASC;

CREATE OR REPLACE VIEW v_daily_sales_summary AS
SELECT
    DATE(o.created_at) as sale_date,
    COUNT(*) as order_count,
    SUM(o.total_amount) as total_revenue,
    AVG(o.total_amount) as avg_order_value,
    COUNT(DISTINCT o.user_id) as unique_customers
FROM orders o
WHERE o.status = 'COMPLETED'
GROUP BY DATE(o.created_at)
ORDER BY sale_date DESC;

CREATE OR REPLACE VIEW v_abandoned_carts AS
SELECT
    c.user_id,
    u.full_name,
    u.email,
    COUNT(*) as item_count,
    SUM(c.quantity * p.price) as cart_value,
    MAX(c.added_at) as last_activity
FROM cart_items c
JOIN users u ON c.user_id = u.user_id
JOIN products p ON c.product_id = p.product_id
WHERE c.added_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY c.user_id, u.full_name, u.email
HAVING item_count > 0
ORDER BY last_activity DESC;

-- ============================================================
-- 13. TRIGGERS
-- ============================================================

DELIMITER //
CREATE TRIGGER trg_product_update_timestamp
BEFORE UPDATE ON products
FOR EACH ROW
BEGIN
    SET NEW.updated_at = NOW();
END//

CREATE TRIGGER trg_prevent_duplicate_pending_orders
BEFORE INSERT ON orders
FOR EACH ROW
BEGIN
    DECLARE pending_count INT;
    SELECT COUNT(*) INTO pending_count
    FROM orders
    WHERE user_id = NEW.user_id
    AND status = 'PENDING'
    AND created_at > DATE_SUB(NOW(), INTERVAL 5 MINUTE);
    IF pending_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Another order is already being processed';
    END IF;
END//
DELIMITER ;

-- ============================================================
-- 14. SEED DATA
-- ============================================================

-- Admin Account (Password: admin123)
INSERT INTO users (full_name, email, password, phone, address, role, is_active, created_at) VALUES
('Admin User', 'admin@fashionstore.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrU1Xc0TgKp/tN4tMHcpLw7J3Z0D3G', '9876543210', 'Admin Office, FashionStore HQ', 'admin', TRUE, NOW());

-- Demo Customer Accounts (Password: demo123)
INSERT INTO users (full_name, email, password, phone, address, role, created_at) VALUES
('Sarah Johnson', 'sarah.demo@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrU1Xc0TgKp/tN4tMHcpLw7J3Z0D3G', '9876543211', '123 Fashion Street, Mumbai, Maharashtra 400001', 'customer', NOW()),
('Rahul Sharma', 'rahul.demo@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrU1Xc0TgKp/tN4tMHcpLw7J3Z0D3G', '9876543212', '456 Style Avenue, Delhi, 110001', 'customer', NOW()),
('Priya Patel', 'priya.demo@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrU1Xc0TgKp/tN4tMHcpLw7J3Z0D3G', '9876543213', '789 Trend Boulevard, Bangalore, 560001', 'customer', NOW()),
('Arjun Kumar', 'arjun.demo@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrU1Xc0TgKp/tN4tMHcpLw7J3Z0D3G', '9876543214', '321 Vogue Lane, Hyderabad, 500001', 'customer', NOW());

-- Categories
INSERT INTO categories (category_id, category_name, description, parent_category_id, is_active) VALUES
(1, 'Men', 'Menswear and essentials', NULL, TRUE),
(2, 'Women', 'Womenswear and essentials', NULL, TRUE),
(3, 'Footwear', 'Shoes, sneakers, boots and more', NULL, TRUE),
(4, 'Accessories', 'Bags, belts, jewelry and more', NULL, TRUE);
UPDATE categories SET category_name = TRIM(category_name), description = TRIM(description);
ALTER TABLE categories AUTO_INCREMENT = 5;

-- Product Catalog
INSERT INTO products (product_name, description, price, discount_percent, image_url, category_id, brand, active, is_new, is_sale, is_trending, popular_score, created_at) VALUES
('Classic Navy Tailored Blazer', 'Premium wool blend blazer with structured shoulders and peak lapels for formal and business wear.', 8999.00, 15.00, 'https://images.unsplash.com/photo-1594938298603-c8148c4dae35?auto=format&fit=crop&w=1000&q=80', 1, 'Zara Man', TRUE, TRUE, FALSE, TRUE, 9.2, NOW()),
('Slim Fit White Oxford Shirt', 'Crisp cotton oxford shirt with button-down collar and a clean slim silhouette.', 2499.00, 0.00, 'https://images.unsplash.com/photo-1603252109303-2751441dd157?auto=format&fit=crop&w=1000&q=80', 1, 'H&M', TRUE, FALSE, FALSE, TRUE, 8.4, NOW()),
('Olive Bomber Jacket', 'Contemporary nylon bomber jacket with ribbed collar, zip front and quilted lining.', 4999.00, 25.00, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=1000&q=80', 1, 'Nike', TRUE, TRUE, TRUE, TRUE, 8.8, NOW()),
('Slim Fit Denim Jeans', 'Stretch denim jeans with a modern slim fit and premium indigo wash.', 2199.00, 12.00, 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=1000&q=80', 1, 'Levis', TRUE, FALSE, TRUE, FALSE, 7.9, NOW()),
('Floral Midi Summer Dress', 'Elegant floral midi dress with V neckline and A-line movement for warm weather occasions.', 4499.00, 20.00, 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=1000&q=80', 2, 'H&M', TRUE, TRUE, TRUE, TRUE, 9.6, NOW()),
('High Waist Office Trousers', 'Tailored high-waist trousers with stretch comfort and a polished straight-leg profile.', 2399.00, 14.00, 'https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?auto=format&fit=crop&w=1000&q=80', 2, 'Mango', TRUE, FALSE, TRUE, FALSE, 8.1, NOW()),
('Satin Wrap Blouse', 'Soft satin wrap blouse with gathered cuffs and an easy evening-to-office finish.', 2799.00, 10.00, 'https://images.unsplash.com/photo-1551489186-cf8726f514f8?auto=format&fit=crop&w=1000&q=80', 2, 'Only', TRUE, TRUE, FALSE, TRUE, 8.6, NOW()),
('Pleated Knit Skirt', 'Midi-length pleated knit skirt with comfortable waistband and refined drape.', 3199.00, 18.00, 'https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?auto=format&fit=crop&w=1000&q=80', 2, 'Vero Moda', TRUE, FALSE, TRUE, FALSE, 7.7, NOW()),
('Black Leather Derby Shoes', 'Genuine leather derby shoes with cushioned insole and durable rubber outsole.', 5999.00, 10.00, 'https://images.unsplash.com/photo-1614252369475-531eba835eb1?auto=format&fit=crop&w=1000&q=80', 3, 'Clarks', TRUE, FALSE, FALSE, TRUE, 8.9, NOW()),
('White Court Sneakers', 'Minimal leather sneakers with padded collar and everyday cupsole construction.', 4299.00, 8.00, 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=1000&q=80', 3, 'Adidas', TRUE, TRUE, FALSE, TRUE, 9.1, NOW()),
('Tan Chelsea Boots', 'Polished suede Chelsea boots with elastic side panels and stacked heel.', 6799.00, 16.00, 'https://images.unsplash.com/photo-1608256246200-53e635b5b65f?auto=format&fit=crop&w=1000&q=80', 3, 'Aldo', TRUE, FALSE, TRUE, FALSE, 8.0, NOW()),
('Cushioned Running Shoes', 'Lightweight running shoes with breathable mesh upper and responsive cushioning.', 5499.00, 12.00, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1000&q=80', 3, 'Puma', TRUE, TRUE, TRUE, TRUE, 9.4, NOW()),
('Structured Leather Tote Bag', 'Full-grain leather tote with laptop sleeve, magnetic closure and reinforced handles.', 6999.00, 18.00, 'https://images.unsplash.com/photo-1590874103328-eac38a683ce7?auto=format&fit=crop&w=1000&q=80', 4, 'Caprese', TRUE, TRUE, TRUE, TRUE, 8.7, NOW()),
('Minimal Gold Hoop Earrings', 'Polished gold-tone hoops with lightweight secure clasp for everyday styling.', 1299.00, 5.00, 'https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?auto=format&fit=crop&w=1000&q=80', 4, 'Accessorize', TRUE, FALSE, FALSE, TRUE, 7.8, NOW()),
('Reversible Leather Belt', 'Black and tan reversible leather belt with brushed metal buckle.', 1899.00, 10.00, 'https://images.unsplash.com/photo-1624222247344-550fb60583dc?auto=format&fit=crop&w=1000&q=80', 4, 'Tommy Hilfiger', TRUE, FALSE, TRUE, FALSE, 7.5, NOW()),
('Polarized Square Sunglasses', 'UV-protected polarized sunglasses with square acetate frame and travel case.', 2499.00, 12.00, 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=1000&q=80', 4, 'Ray-Ban', TRUE, TRUE, FALSE, TRUE, 8.5, NOW());

-- Product Sizes
INSERT INTO product_sizes (product_id, size_label, stock_quantity, sku_code, is_available) VALUES
(1,'S',18,'ZMN-BLZ-S',true),(1,'M',28,'ZMN-BLZ-M',true),(1,'L',22,'ZMN-BLZ-L',true),(1,'XL',12,'ZMN-BLZ-XL',true),
(2,'S',20,'HNM-SHT-S',true),(2,'M',34,'HNM-SHT-M',true),(2,'L',24,'HNM-SHT-L',true),(2,'XL',14,'HNM-SHT-XL',true),
(3,'S',14,'NKE-BMB-S',true),(3,'M',24,'NKE-BMB-M',true),(3,'L',18,'NKE-BMB-L',true),(3,'XL',10,'NKE-BMB-XL',true),
(4,'S',16,'LVS-DNM-S',true),(4,'M',30,'LVS-DNM-M',true),(4,'L',26,'LVS-DNM-L',true),(4,'XL',15,'LVS-DNM-XL',true),
(5,'S',19,'HNM-DRS-S',true),(5,'M',25,'HNM-DRS-M',true),(5,'L',17,'HNM-DRS-L',true),(5,'XL',8,'HNM-DRS-XL',true),
(6,'S',15,'MNG-TRS-S',true),(6,'M',21,'MNG-TRS-M',true),(6,'L',18,'MNG-TRS-L',true),(6,'XL',9,'MNG-TRS-XL',true),
(7,'S',20,'ONLY-BLS-S',true),(7,'M',26,'ONLY-BLS-M',true),(7,'L',18,'ONLY-BLS-L',true),(7,'XL',8,'ONLY-BLS-XL',true),
(8,'S',12,'VRM-SKT-S',true),(8,'M',19,'VRM-SKT-M',true),(8,'L',15,'VRM-SKT-L',true),(8,'XL',7,'VRM-SKT-XL',true),
(9,'7',10,'CLK-DRB-7',true),(9,'8',18,'CLK-DRB-8',true),(9,'9',20,'CLK-DRB-9',true),(9,'10',12,'CLK-DRB-10',true),
(10,'7',13,'ADS-SNK-7',true),(10,'8',24,'ADS-SNK-8',true),(10,'9',22,'ADS-SNK-9',true),(10,'10',15,'ADS-SNK-10',true),
(11,'7',8,'ALD-CHL-7',true),(11,'8',14,'ALD-CHL-8',true),(11,'9',16,'ALD-CHL-9',true),(11,'10',9,'ALD-CHL-10',true),
(12,'7',18,'PMA-RUN-7',true),(12,'8',28,'PMA-RUN-8',true),(12,'9',26,'PMA-RUN-9',true),(12,'10',18,'PMA-RUN-10',true),
(13,'OS',32,'CAP-TOT-OS',true),
(14,'OS',45,'ACC-HOP-OS',true),
(15,'OS',28,'TMH-BLT-OS',true),
(16,'OS',24,'RBN-SUN-OS',true);

UPDATE products p
SET stock_quantity = (
    SELECT COALESCE(SUM(ps.stock_quantity), 0)
    FROM product_sizes ps
    WHERE ps.product_id = p.product_id
);

-- Coupons
INSERT INTO coupons (code, description, discount_type, discount_value, minimum_order_amount, usage_limit, valid_from, valid_until) VALUES
('WELCOME10', 'Welcome discount for new customers', 'PERCENTAGE', 10.00, 100.00, 1000, NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR)),
('FLAT50', 'Flat Rs.50 off on orders above Rs.500', 'FIXED_AMOUNT', 50.00, 500.00, 500, NOW(), DATE_ADD(NOW(), INTERVAL 6 MONTH)),
('FREESHIP', 'Free shipping on all orders', 'FIXED_AMOUNT', 40.00, 0.00, 2000, NOW(), DATE_ADD(NOW(), INTERVAL 3 MONTH));

-- Shipping Zones
INSERT INTO shipping_zones (zone_name, countries, base_fee, free_shipping_threshold) VALUES
('North India', '["IN-DL", "IN-HR", "IN-PB", "IN-UP", "IN-UT", "IN-CH"]', 50.00, 500.00),
('South India', '["IN-TN", "IN-KA", "IN-KL", "IN-AP", "IN-TS"]', 60.00, 600.00),
('East India', '["IN-WB", "IN-OR", "IN-JH", "IN-AS", "IN-BR"]', 70.00, 700.00),
('West India', '["IN-MH", "IN-GJ", "IN-RJ", "IN-MP", "IN-GA"]', 55.00, 550.00),
('Metro Cities', '["IN-DL", "IN-MH", "IN-KA", "IN-WB"]', 40.00, 400.00);

-- Tax Rates
INSERT INTO tax_rates (country, state, tax_rate, tax_name) VALUES
('India', NULL, 0.1800, 'GST'),
('United States', NULL, 0.0800, 'Sales Tax'),
('United Kingdom', NULL, 0.2000, 'VAT'),
('Canada', NULL, 0.0500, 'GST'),
('Australia', NULL, 0.1000, 'GST');

-- Sample Search Analytics
INSERT INTO search_analytics (search_query, search_count, result_count_avg, click_through_rate, last_searched) VALUES
('blazer', 156, 8.5, 45.2, NOW()),
('dress', 234, 12.3, 52.8, NOW()),
('shoes', 189, 15.2, 38.5, NOW()),
('jeans', 145, 6.8, 41.3, NOW()),
('men', 267, 24.5, 35.2, NOW()),
('women', 298, 28.3, 42.1, NOW()),
('sale', 234, 32.1, 58.9, NOW());

-- ============================================================
-- 15. COMPLETION
-- ============================================================

SELECT 'FashionStore database schema created successfully!' AS status;
