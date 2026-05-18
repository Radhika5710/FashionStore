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

        // Retry logic for database connection during startup
        int maxRetries = 10;
        int retryDelayMs = 5000;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                logger.info("Initializing database connection pool (attempt {}/{})...", attempt, maxRetries);
                
                HikariConfig config = new HikariConfig();
                
                // Detect environment profile
                String profile = System.getenv("FASHIONSTORE_PROFILE");
                if (profile == null || profile.isBlank()) {
                    profile = "dev";
                }
                logger.info("Environment profile: {}", profile);

                // Try environment variables first (production), with fallback to system properties for integration tests
                String url = System.getenv("FASHIONSTORE_DB_URL");
                if (url == null || url.isBlank()) {
                    url = System.getProperty("FASHIONSTORE_DB_URL");
                }
                String user = System.getenv("FASHIONSTORE_DB_USER");
                if (user == null || user.isBlank()) {
                    user = System.getProperty("FASHIONSTORE_DB_USER");
                }
                String password = System.getenv("FASHIONSTORE_DB_PASSWORD");
                if (password == null || password.isBlank()) {
                    password = System.getProperty("FASHIONSTORE_DB_PASSWORD");
                }

                // Docker-style env fallback: DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD
                if (url == null || url.isBlank()) {
                    String host = System.getenv("DB_HOST");
                    String port = System.getenv("DB_PORT");
                    String dbName = System.getenv("DB_NAME");
                    if (host != null && !host.isBlank()) {
                        if (port == null || port.isBlank()) {
                            port = "3306";
                        }
                        if (dbName == null || dbName.isBlank()) {
                            dbName = "fashionstore";
                        }
                        url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                    }
                    if (user == null || user.isBlank()) {
                        user = System.getenv("DB_USER");
                    }
                    if (password == null) {
                        password = System.getenv("DB_PASSWORD");
                    }
                }

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

                // Final local defaults for developer machines
                if (url == null || url.isBlank()) {
                    url = "jdbc:mysql://localhost:3306/fashionstore?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                }
                if (user == null || user.isBlank()) {
                    user = "fashionstore";
                }
                if (password == null) {
                    password = "fashionstore";
                }

                config.setJdbcUrl(url);
                config.setUsername(user);
                config.setPassword(password);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");

                // Pool configuration based on environment
                if ("prod".equalsIgnoreCase(profile)) {
                    config.setMaximumPoolSize(50);
                    config.setMinimumIdle(10);
                    config.setIdleTimeout(60000);
                    config.setConnectionTimeout(30000);
                    config.setMaxLifetime(1800000);
                    logger.info("Production pool configuration applied");
                } else {
                    config.setMaximumPoolSize(50);
                    config.setMinimumIdle(10);
                    config.setIdleTimeout(60000);
                    config.setConnectionTimeout(30000);
                    config.setMaxLifetime(1800000);
                    logger.info("Development pool configuration applied (increased for stability)");
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
                return; // Success, exit retry loop

            } catch (Exception e) {
                initialized = false;
                connectionValid = false;
                
                if (attempt < maxRetries) {
                    logger.warn("Database connection attempt {}/{} failed, retrying in {}ms: {}", 
                        attempt, maxRetries, retryDelayMs, e.getMessage());
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Database connection retry interrupted");
                        break;
                    }
                } else {
                    logger.error("Failed to initialize database connection pool after {} attempts: {}", 
                        maxRetries, e.getMessage(), e);
                    // Keep application booting so controllers can fail gracefully instead of
                    // causing class-initialization errors and blanket HTTP 500 responses.
                }
                
                // Clean up failed datasource
                if (dataSource != null) {
                    dataSource.close();
                    dataSource = null;
                }
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized || !connectionValid) {
            logger.error("Database connection pool is not initialized or invalid");
            throw new SQLException("Database connection pool is not available");
        }
        
        try {
            Connection conn = dataSource.getConnection();
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to get database connection: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get connection with custom network timeout
     * Use for long-running operations that need extended timeout
     */
    public static Connection getConnectionWithTimeout(int timeoutSeconds) throws SQLException {
        if (!initialized || !connectionValid) {
            logger.error("Database connection pool is not initialized or invalid");
            throw new SQLException("Database connection pool is not available");
        }
        
        try {
            Connection conn = dataSource.getConnection();
            conn.setNetworkTimeout(null, timeoutSeconds * 1000);
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to get database connection with timeout: {}", e.getMessage());
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