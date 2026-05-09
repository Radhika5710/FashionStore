# FashionStore

A premium luxury fashion e-commerce platform built with Java 21, Jakarta EE 9+, JSP, Servlets, MySQL, and Maven.

## Features

- **Product Management**: Product listing, details, categories, search, and filtering
- **Shopping Cart**: Add to cart, quantity adjustment, coupon codes
- **Checkout & Payment**: Multi-step checkout with Stripe integration
- **User Authentication**: Registration, login, password reset with BCrypt
- **Wishlist**: Save favorite items with heart animations
- **Admin Dashboard**: Product, order, and user management with analytics
- **Luxury UI/UX**: Premium design system with cinematic typography and glassmorphism

## Tech Stack

- Java 21 (LTS)
- Jakarta EE 9+ (Servlet 6.0, JSP 3.1)
- MySQL 8.0+
- Maven 3.9+
- HikariCP 5.1.0
- BCrypt 0.4
- SLF4J 2.0.7 + Logback 1.4.11
- Redis (Jedis 5.1.0)

## Quick Start

1. **Setup Database**
   ```bash
   mysql -u root -p fashionstore < schema.sql
   ```

2. **Configure Database Connection**
   - Set environment variables: `FASHIONSTORE_DB_URL`, `FASHIONSTORE_DB_USER`, `FASHIONSTORE_DB_PASSWORD`
   - Or create `src/main/resources/db.properties`

3. **Build Application**
   ```bash
   mvn clean package
   ```

4. **Deploy to Tomcat**
   ```bash
   cp target/FashionStore.war $TOMCAT_HOME/webapps/
   ```

5. **Access Application**
   ```
   http://localhost:8080/FashionStore/home
   ```

## Documentation

For complete project audit, features, and remaining issues, see [PROJECT_AUDIT.md](PROJECT_AUDIT.md)

## License

Proprietary - All Rights Reserved