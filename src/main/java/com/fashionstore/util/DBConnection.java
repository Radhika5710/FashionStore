package com.fashionstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static HikariDataSource dataSource;
    private static volatile boolean initialized = false;
    private static volatile boolean connectionValid = false;

    static {
        initializeDataSource();
    }

    private static void initializeDataSource() {
        if (initialized) {
            return;
        }

        try {
            logger.info("Initializing database connection pool...");
            
            HikariConfig config = new HikariConfig();
            
            // Detect environment profile
            String profile = System.getenv("FASHIONSTORE_PROFILE");
            if (profile == null || profile.isBlank()) {
                profile = "dev";
            }
            logger.info("Environment profile: {}", profile);

            // Try environment variables first (production)
            String url = System.getenv("FASHIONSTORE_DB_URL");
            String user = System.getenv("FASHIONSTORE_DB_USER");
            String password = System.getenv("FASHIONSTORE_DB_PASSWORD");

            // If environment variables not set, try loading from properties file (dev/local)
            if (url == null || url.isBlank() || user == null || user.isBlank() || password == null) {
                try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                    if (is != null) {
                        Properties props = new Properties();
                        props.load(is);
                        url = props.getProperty("db.url");
                        user = props.getProperty("db.user");
                        password = props.getProperty("db.password");
                        logger.info("Loaded database configuration from db.properties");
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load db.properties: {}", e.getMessage());
                }
            } else {
                logger.info("Loaded database configuration from environment variables");
            }

            if (url == null || url.isBlank() || user == null || user.isBlank() || password == null) {
                logger.error("Database configuration is missing. Please set environment variables or configure db.properties");
                logger.error("Required environment variables: FASHIONSTORE_DB_URL, FASHIONSTORE_DB_USER, FASHIONSTORE_DB_PASSWORD");
                throw new IllegalStateException(
                        "Database configuration is missing. Set FASHIONSTORE_DB_URL, FASHIONSTORE_DB_USER, and FASHIONSTORE_DB_PASSWORD environment variables, or create src/main/resources/db.properties file.");
            }

            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Pool configuration based on environment
            if ("prod".equalsIgnoreCase(profile)) {
                config.setMaximumPoolSize(20);
                config.setMinimumIdle(5);
                config.setIdleTimeout(60000);
                config.setConnectionTimeout(30000);
                config.setMaxLifetime(1800000);
                logger.info("Production pool configuration applied");
            } else {
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setIdleTimeout(30000);
                config.setConnectionTimeout(20000);
                config.setMaxLifetime(1800000);
                logger.info("Development pool configuration applied");
            }

            // Optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            // Connection test
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);

            dataSource = new HikariDataSource(config);
            
            // Validate connection on startup
            try (Connection testConn = dataSource.getConnection()) {
                if (testConn.isValid(5)) {
                    connectionValid = true;
                    logger.info("Database connection validated successfully");
                } else {
                    throw new SQLException("Connection validation failed");
                }
            }

            initialized = true;
            logger.info("Database connection pool initialized successfully");

        } catch (Exception e) {
            initialized = false;
            connectionValid = false;
            logger.error("Failed to initialize database connection pool: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized || !connectionValid) {
            logger.error("Database connection pool is not initialized or invalid");
            throw new SQLException("Database connection pool is not available");
        }
        
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Failed to get database connection: {}", e.getMessage());
            throw e;
        }
    }

    public static boolean isHealthy() {
        return initialized && connectionValid;
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool...");
            dataSource.close();
            initialized = false;
            connectionValid = false;
            logger.info("Database connection pool closed successfully");
        }
    }
}