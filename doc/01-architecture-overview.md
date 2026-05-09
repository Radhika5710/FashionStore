# FashionStore Architecture Overview

## System Architecture

FashionStore is a Java-based ecommerce platform built using the Model-View-Controller (MVC) pattern with JSP and Servlets.

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Client Browser                        │
│                    (HTML/CSS/JavaScript)                     │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/HTTPS
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Tomcat Server                            │
│  ┌───────────────────────────────────────────────────────┐  │
│  │           Servlet Container (web.xml)                   │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌──────────┬──────────────┬──────────────────────────────┐  │
│  │ Filters  │ Controllers   │ Views (JSP)                 │  │
│  ├──────────┼──────────────┼──────────────────────────────┤  │
│  │ Auth     │ HomeServlet  │ home.jsp                    │  │
│  │ CSRF     │ Login        │ login.jsp                   │  │
│  │ Security │ Product      │ product.jsp                 │  │
│  │ Exception│ Cart         │ cart.jsp                    │  │
│  │ Headers  │ Checkout     │ checkout.jsp                │  │
│  └──────────┼──────────────┼──────────────────────────────┤  │
│  │          │ Order        │ orders.jsp                  │  │
│  │          │ Admin*       │ admin/*.jsp                 │  │
│  │          └──────────────┴──────────────────────────────┤  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Service Layer                            │  │
│  │  UserService | ProductService | PaymentService         │  │
│  │  EmailService | SearchService | RecommendationService   │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    DAO Layer                               │  │
│  │  UserDAO | ProductDAO | CartDAO | OrderDAO | PaymentDAO  │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                   Utilities & Cache                       │  │
│  │  DBConnection | CacheService | SecurityUtil | Validator │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────┘
                         │ JDBC
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    MySQL Database                             │
│  users | products | orders | cart_items | order_items     │
│  categories | reviews | payments | product_sizes          │
└─────────────────────────────────────────────────────────────┘
```

## Package Structure

```
com.fashionstore/
├── cache/              # Caching layer
│   └── CacheService.java
├── controller/         # MVC Controllers (Servlets)
│   ├── AdminDashboardController.java
│   ├── AdminOrderController.java
│   ├── AdminProductController.java
│   ├── AdminUsersController.java
│   ├── CartController.java
│   ├── CheckoutController.java
│   ├── HomeServlet.java
│   ├── LoginController.java
│   ├── LogoutController.java
│   ├── OrderController.java
│   ├── PasswordResetController.java
│   ├── PaymentController.java
│   ├── ProductController.java
│   ├── ProductDetailsController.java
│   ├── RegisterController.java
│   ├── ReviewController.java
│   ├── SearchController.java
│   ├── SuccessController.java
│   └── WishlistController.java
├── dao/                # Data Access Object Interfaces
│   ├── AddressDAO.java
│   ├── CartDAO.java
│   ├── CategoryDAO.java
│   ├── CouponDAO.java
│   ├── OrderDAO.java
│   ├── OrderItemDAO.java
│   ├── PasswordResetTokenDAO.java
│   ├── PaymentDAO.java
│   ├── PaymentMethodDAO.java
│   ├── ProductDAO.java
│   ├── ProductSizeDAO.java
│   ├── ReviewDAO.java
│   ├── SavedItemDAO.java
│   ├── UserDAO.java
│   └── WishlistDAO.java
├── daoimpl/            # DAO Implementations
│   └── (15 implementation classes)
├── domain/             # Domain-specific classes
│   └── ProductQuery.java
├── enums/              # Enumerations
│   └── CategoryType.java
├── exception/          # Custom exceptions
│   └── ApplicationException.java
├── filter/             # Servlet Filters
│   ├── AuthFilter.java
│   ├── CSRFFilter.java
│   ├── ExceptionHandler.java
│   ├── SecurityFilter.java
│   └── SecurityHeadersFilter.java
├── model/              # Entity Models (POJOs)
│   ├── Address.java
│   ├── CartItem.java
│   ├── Category.java
│   ├── Coupon.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderStatus.java
│   ├── PasswordResetToken.java
│   ├── Payment.java
│   ├── PaymentMethod.java
│   ├── PaymentTransaction.java
│   ├── Product.java
│   ├── ProductQuery.java
│   ├── ProductSize.java
│   ├── Review.java
│   ├── SavedItem.java
│   ├── User.java
│   └── WishlistItem.java
├── security/           # Security components
│   ├── CSRFProtection.java
│   └── RateLimiter.java
├── service/            # Business Logic Layer
│   ├── CategoryService.java
│   ├── EmailService.java
│   ├── PaymentService.java
│   ├── ProductService.java
│   ├── RecommendationService.java
│   ├── SearchService.java
│   └── UserService.java
├── util/               # Utility classes
│   ├── AuditLogger.java
│   ├── DBConnection.java
│   ├── ExceptionHandler.java
│   ├── ExceptionHandlerUtil.java
│   ├── NullSafetyUtil.java
│   ├── SecurityUtil.java
│   └── XSSUtil.java
└── validation/         # Input validation
    ├── Validator.java
    └── ValidationException.java
```

## MVC Pattern Implementation

### Model Layer
The Model layer consists of POJOs (Plain Old Java Objects) that represent the data entities:

- **User**: Represents user accounts
- **Product**: Represents product information
- **CartItem**: Represents items in shopping cart
- **Order**: Represents customer orders
- **OrderItem**: Represents items within an order
- **Payment**: Represents payment information
- **Review**: Represents product reviews
- **WishlistItem**: Represents wishlist items

### View Layer
The View layer consists of JSP (JavaServer Pages) files located in `/WEB-INF/views/`:

- **home.jsp**: Homepage with featured products
- **login.jsp**: Login form
- **register.jsp**: Registration form
- **product.jsp**: Product listing page
- **product-details.jsp**: Individual product details
- **cart.jsp**: Shopping cart
- **checkout.jsp**: Checkout form
- **orders.jsp**: Order history
- **admin/*.jsp**: Admin dashboard pages

### Controller Layer
The Controller layer consists of HttpServlet classes that handle HTTP requests:

- Controllers receive HTTP requests (GET/POST)
- Process business logic via Service layer
- Interact with DAO layer for data access
- Forward to appropriate JSP views
- Handle exceptions and errors

## Request Flow

### Standard Request Flow

```
1. Client sends HTTP Request
   ↓
2. Tomcat receives request
   ↓
3. Filters execute in order (web.xml):
   - SecurityHeadersFilter
   - SecurityFilter
   - CSRFFilter
   - AuthFilter
   ↓
4. Controller Servlet receives request
   ↓
5. Controller validates CSRF token (if POST)
   ↓
6. Controller calls Service layer
   ↓
7. Service layer calls DAO layer
   ↓
8. DAO layer executes SQL queries
   ↓
9. Data flows back: DAO → Service → Controller
   ↓
10. Controller sets request attributes
   ↓
11. Controller forwards to JSP view
   ↓
12. JSP renders HTML response
   ↓
13. Response sent to client
```

## Filter Chain

The application uses a filter chain for cross-cutting concerns:

```
Request → SecurityHeadersFilter → SecurityFilter → CSRFFilter → AuthFilter → Controller → Response
```

**SecurityHeadersFilter**: Sets security headers (CSP, HSTS, XSS protection)

**SecurityFilter**: 
- CSRF token generation/validation
- Rate limiting for login attempts
- Security headers

**CSRFFilter**: Additional CSRF protection for specific endpoints

**AuthFilter**: 
- Enforces authentication for private paths
- Enforces admin authorization for admin paths
- Handles AJAX vs regular request responses

## Security Architecture

### Authentication Flow

```
┌──────────────┐
│ Login Request│
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Rate Limiter  │ ← Check login attempts
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ LoginController│
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ UserService  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ UserDAO      │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ BCrypt Verify│ ← Password hashing
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Session Created
│ + CSRF Token  │
└──────────────┘
```

### CSRF Protection

```
1. GET Request → Generate CSRF Token → Store in Session
2. Form includes CSRF Token as hidden field
3. POST Request → Validate Token against Session
4. Valid → Process Request
5. Invalid → Return 403 Forbidden
```

## Caching Architecture

```
┌──────────────┐
│ Controller   │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ CacheService │ ← Singleton Pattern
└──────┬───────┘
       │
       ├─── In-Memory Cache (ConcurrentHashMap)
       │    - TTL-based expiration
       │    - Pattern-based invalidation
       │
       └─── Redis (Future - currently disabled)
```

## Transaction Management

The application uses JDBC transaction management:

```java
Connection con = DBConnection.getConnection();
con.setAutoCommit(false);

try {
    // Execute multiple operations
    operation1(con);
    operation2(con);
    operation3(con);
    
    con.commit(); // Commit if all succeed
} catch (Exception e) {
    con.rollback(); // Rollback on any failure
} finally {
    con.setAutoCommit(true);
    con.close();
}
```

## Error Handling

```
Exception → ExceptionHandler Filter
              ↓
         Log Error (SLF4J)
              ↓
    User-Safe Error Message
              ↓
         Error Page / JSON Response
```

## Technology Stack

- **Backend**: Java 21, Jakarta Servlet API
- **Frontend**: JSP, HTML5, CSS3, JavaScript
- **Database**: MySQL
- **Build Tool**: Maven
- **Server**: Apache Tomcat
- **Security**: BCrypt (password hashing), Custom CSRF implementation
- **Logging**: SLF4J
- **Caching**: In-memory (ConcurrentHashMap) with Redis placeholder
