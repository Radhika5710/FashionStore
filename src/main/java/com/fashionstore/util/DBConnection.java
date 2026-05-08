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
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        
        // Try environment variables first
        String url = System.getenv("FASHIONSTORE_DB_URL");
        String user = System.getenv("FASHIONSTORE_DB_USER");
        String password = System.getenv("FASHIONSTORE_DB_PASSWORD");

        // If environment variables not set, try loading from properties file
        if (url == null || url.isBlank() || user == null || user.isBlank() || password == null) {
            try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    url = props.getProperty("db.url");
                    user = props.getProperty("db.user");
                    password = props.getProperty("db.password");
                }
            } catch (Exception e) {
                logger.warn("Failed to load db.properties: {}", e.getMessage());
            }
        }

        if (url == null || url.isBlank() || user == null || user.isBlank() || password == null) {
            throw new IllegalStateException(
                    "Database configuration is missing. Set FASHIONSTORE_DB_URL, FASHIONSTORE_DB_USER, and FASHIONSTORE_DB_PASSWORD environment variables, or create src/main/resources/db.properties file.");
        }

        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Production-ready pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000);
        
        // Optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}