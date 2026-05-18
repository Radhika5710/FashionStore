-- FashionStore Performance Metrics Schema
-- Stores client-side performance metrics and Core Web Vitals for monitoring

-- Performance Metrics Table
CREATE TABLE IF NOT EXISTS performance_metrics (
    metric_id INT AUTO_INCREMENT PRIMARY KEY,
    lcp DECIMAL(10, 2) COMMENT 'Largest Contentful Paint (milliseconds)',
    fid DECIMAL(10, 2) COMMENT 'First Input Delay (milliseconds)',
    cls DECIMAL(10, 4) COMMENT 'Cumulative Layout Shift (unitless)',
    ttfb DECIMAL(10, 2) COMMENT 'Time to First Byte (milliseconds)',
    fcp DECIMAL(10, 2) COMMENT 'First Contentful Paint (milliseconds)',
    page VARCHAR(500) COMMENT 'Page URL where metrics were collected',
    user_agent VARCHAR(500) COMMENT 'Client user agent string',
    ip_address VARCHAR(45) COMMENT 'Client IP address (IPv4 or IPv6)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_metrics_page (page),
    INDEX idx_metrics_created (created_at),
    INDEX idx_metrics_lcp (lcp),
    INDEX idx_metrics_fid (fid),
    INDEX idx_metrics_cls (cls),
    INDEX idx_metrics_ttfb (ttfb),
    INDEX idx_metrics_fcp (fcp),
    
    CONSTRAINT check_lcp CHECK (lcp IS NULL OR lcp >= 0),
    CONSTRAINT check_fid CHECK (fid IS NULL OR fid >= 0),
    CONSTRAINT check_cls CHECK (cls IS NULL OR cls >= 0),
    CONSTRAINT check_ttfb CHECK (ttfb IS NULL OR ttfb >= 0),
    CONSTRAINT check_fcp CHECK (fcp IS NULL OR fcp >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Client-side performance metrics and Core Web Vitals';

-- Performance Metrics Summary (hourly aggregation)
CREATE TABLE IF NOT EXISTS performance_metrics_hourly (
    summary_id INT AUTO_INCREMENT PRIMARY KEY,
    hour_start TIMESTAMP NOT NULL,
    page VARCHAR(500) NOT NULL,
    
    -- Core Web Vitals (average)
    avg_lcp DECIMAL(10, 2),
    avg_fid DECIMAL(10, 2),
    avg_cls DECIMAL(10, 4),
    avg_ttfb DECIMAL(10, 2),
    avg_fcp DECIMAL(10, 2),
    
    -- Core Web Vitals (percentile 75)
    p75_lcp DECIMAL(10, 2),
    p75_fid DECIMAL(10, 2),
    p75_cls DECIMAL(10, 4),
    p75_ttfb DECIMAL(10, 2),
    p75_fcp DECIMAL(10, 2),
    
    -- Metrics count
    metric_count INT DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_hour_page (hour_start, page),
    INDEX idx_hourly_page (page),
    INDEX idx_hourly_hour (hour_start),
    INDEX idx_hourly_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Hourly aggregated performance metrics for analysis';

-- Event to aggregate metrics hourly (runs every hour)
CREATE EVENT IF NOT EXISTS aggregate_performance_metrics
ON SCHEDULE EVERY 1 HOUR
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    INSERT INTO performance_metrics_hourly 
    (hour_start, page, avg_lcp, avg_fid, avg_cls, avg_ttfb, avg_fcp, 
     p75_lcp, p75_fid, p75_cls, p75_ttfb, p75_fcp, metric_count)
    SELECT 
        DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00') as hour_start,
        page,
        AVG(lcp) as avg_lcp,
        AVG(fid) as avg_fid,
        AVG(cls) as avg_cls,
        AVG(ttfb) as avg_ttfb,
        AVG(fcp) as avg_fcp,
        PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY lcp) as p75_lcp,
        PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY fid) as p75_fid,
        PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY cls) as p75_cls,
        PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY ttfb) as p75_ttfb,
        PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY fcp) as p75_fcp,
        COUNT(*) as metric_count
    FROM performance_metrics
    WHERE created_at >= DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 HOUR), '%Y-%m-%d %H:00:00')
      AND created_at < DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00')
    GROUP BY hour_start, page
    ON DUPLICATE KEY UPDATE
        avg_lcp = VALUES(avg_lcp),
        avg_fid = VALUES(avg_fid),
        avg_cls = VALUES(avg_cls),
        avg_ttfb = VALUES(avg_ttfb),
        avg_fcp = VALUES(avg_fcp),
        p75_lcp = VALUES(p75_lcp),
        p75_fid = VALUES(p75_fid),
        p75_cls = VALUES(p75_cls),
        p75_ttfb = VALUES(p75_ttfb),
        p75_fcp = VALUES(p75_fcp),
        metric_count = VALUES(metric_count),
        updated_at = CURRENT_TIMESTAMP;
END;
