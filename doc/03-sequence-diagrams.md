# FashionStore Sequence Diagrams

## User Registration Flow

```mermaid
sequenceDiagram
    participant User
    participant RegisterController
    participant UserService
    participant UserDAO
    participant Database
    participant EmailService
    
    User->>RegisterController: POST /register (user details)
    RegisterController->>Validator: validate inputs
    Validator-->>RegisterController: validation result
    
    alt Invalid Input
        RegisterController-->>User: Error message
    else Valid Input
        RegisterController->>UserService: registerUser(user)
        UserService->>UserDAO: registerUser(user)
        UserDAO->>Database: INSERT INTO users
        Database-->>UserDAO: generated userId
        UserDAO-->>UserService: userId
        UserService-->>RegisterController: userId
        
        RegisterController->>EmailService: sendWelcomeEmail(user)
        EmailService-->>RegisterController: email sent
        
        RegisterController->>RegisterController: Create session
        RegisterController-->>User: Redirect to /home
    end
```

## User Login Flow

```mermaid
sequenceDiagram
    participant User
    participant SecurityFilter
    participant LoginController
    participant RateLimiter
    participant UserService
    participant UserDAO
    participant Database
    participant CSRFProtection
    
    User->>SecurityFilter: POST /login (credentials)
    SecurityFilter->>SecurityFilter: setSecurityHeaders()
    SecurityFilter->>SecurityFilter: handleCSRF()
    SecurityFilter->>RateLimiter: checkRateLimit()
    
    alt Rate Limit Exceeded
        RateLimiter-->>SecurityFilter: false
        SecurityFilter-->>User: Too many attempts error
    else Rate Limit OK
        RateLimiter-->>SecurityFilter: true
        SecurityFilter->>LoginController: Forward request
        LoginController->>CSRFProtection: validateToken()
        
        alt Invalid CSRF Token
            CSRFProtection-->>LoginController: false
            LoginController-->>User: CSRF validation failed
        else Valid CSRF Token
            CSRFProtection-->>LoginController: true
            LoginController->>UserService: loginUser(email, password)
            UserService->>UserDAO: loginUser(email, password)
            UserDAO->>Database: SELECT * FROM users WHERE email = ?
            Database-->>UserDAO: user record
            UserDAO->>UserDAO: BCrypt.checkpw(password, hash)
            
            alt Invalid Password
                UserDAO-->>UserService: null
                UserService-->>LoginController: null
                LoginController-->>User: Invalid credentials
            else Valid Password
                UserDAO-->>UserService: user
                UserService-->>LoginController: user
                LoginController->>LoginController: Create session
                LoginController->>CSRFProtection: generateToken()
                LoginController->>RateLimiter: resetRateLimit()
                LoginController-->>User: Redirect to /home
            end
        end
    end
```

## Product Browsing Flow

```mermaid
sequenceDiagram
    participant User
    participant SecurityFilter
    participant AuthFilter
    participant ProductController
    participant ProductService
    participant ProductDAO
    participant CacheService
    participant Database
    
    User->>SecurityFilter: GET /products
    SecurityFilter->>SecurityFilter: setSecurityHeaders()
    SecurityFilter->>AuthFilter: Forward request
    AuthFilter->>AuthFilter: Check if public path
    AuthFilter-->>ProductController: Forward request
    
    ProductController->>ProductService: getAllProducts()
    ProductService->>CacheService: get("fashionstore:products:all")
    
    alt Cache Hit
        CacheService-->>ProductService: products list
    else Cache Miss
        ProductService->>ProductDAO: getAllProducts()
        ProductDAO->>Database: SELECT * FROM products WHERE is_active = true
        Database-->>ProductDAO: products
        ProductDAO-->>ProductService: products
        ProductService->>CacheService: put("fashionstore:products:all", products, 1 hour)
        CacheService-->>ProductService: success
    end
    
    ProductService-->>ProductController: products
    ProductController->>ProductController: Set request attributes
    ProductController-->>User: Forward to product.jsp
```

## Product Details Flow

```mermaid
sequenceDiagram
    participant User
    participant ProductDetailsController
    participant ProductService
    participant ProductDAO
    participant ProductSizeDAO
    participant ReviewDAO
    participant Database
    
    User->>ProductDetailsController: GET /product?id=123
    ProductDetailsController->>ProductService: getProductWithSizes(123)
    ProductService->>CacheService: get("fashionstore:product:123")
    
    alt Cache Hit
        CacheService-->>ProductService: product
    else Cache Miss
        ProductService->>ProductDAO: getProductById(123)
        ProductDAO->>Database: SELECT * FROM products WHERE product_id = 123
        Database-->>ProductDAO: product
        ProductDAO-->>ProductService: product
        
        ProductService->>ProductSizeDAO: getSizesByProductId(123)
        ProductSizeDAO->>Database: SELECT * FROM product_sizes WHERE product_id = 123
        Database-->>ProductSizeDAO: sizes
        ProductSizeDAO-->>ProductService: sizes
        
        ProductService->>CacheService: put("fashionstore:product:123", product, 1 hour)
    end
    
    ProductService-->>ProductDetailsController: product with sizes
    
    ProductDetailsController->>ReviewDAO: getReviewsByProductId(123)
    ReviewDAO->>Database: SELECT * FROM reviews WHERE product_id = 123
    Database-->>ReviewDAO: reviews
    ReviewDAO-->>ProductDetailsController: reviews
    
    ProductDetailsController-->>User: Forward to product-details.jsp
```

## Add to Cart Flow

```mermaid
sequenceDiagram
    participant User
    participant AuthFilter
    participant CartController
    participant CartDAO
    participant Database
    participant CacheService
    
    User->>AuthFilter: POST /cart/add (productId, size, quantity)
    AuthFilter->>AuthFilter: Check session
    alt Not Logged In
        AuthFilter-->>User: Redirect to /login
    else Logged In
        AuthFilter->>CartController: Forward request
        CartController->>CartDAO: addToCart(cartItem)
        CartDAO->>Database: INSERT INTO cart_items
        Database-->>CartDAO: success
        CartDAO-->>CartController: true
        
        CartController->>CacheService: remove("cart:user:" + userId)
        CacheService-->>CartController: success
        
        CartController-->>User: JSON response {success: true}
    end
```

## Checkout Flow

```mermaid
sequenceDiagram
    participant User
    participant AuthFilter
    participant CheckoutController
    participant CartDAO
    participant Database
    participant CacheService
    
    User->>AuthFilter: POST /checkout (shipping details, payment)
    AuthFilter->>AuthFilter: Check session
    alt Not Logged In
        AuthFilter-->>User: Redirect to /login
    else Logged In
        AuthFilter->>CheckoutController: Forward request
        
        CheckoutController->>CheckoutController: Begin transaction
        CheckoutController->>CartDAO: getCartItemsByUserId(userId)
        CartDAO->>Database: SELECT * FROM cart_items WHERE user_id = ?
        Database-->>CartDAO: cart items
        CartDAO-->>CheckoutController: cart items
        
        loop For each cart item
            CheckoutController->>Database: UPDATE product_sizes SET stock = stock - ? WHERE product_id = ? AND size = ? AND stock >= ?
            alt Stock insufficient
                CheckoutController->>CheckoutController: Rollback transaction
                CheckoutController-->>User: Error: Insufficient stock
            end
        end
        
        CheckoutController->>Database: INSERT INTO orders (user_id, total, ...)
        Database-->>CheckoutController: orderId
        
        loop For each cart item
            CheckoutController->>Database: INSERT INTO order_items (order_id, product_id, ...)
        end
        
        CheckoutController->>Database: DELETE FROM cart_items WHERE user_id = ?
        CheckoutController->>CheckoutController: Commit transaction
        
        CheckoutController->>CacheService: remove("cart:user:" + userId)
        CheckoutController->>CacheService: invalidatePattern("fashionstore:products:*")
        
        CheckoutController-->>User: Redirect to /success
    end
```

## Order History Flow

```mermaid
sequenceDiagram
    participant User
    participant AuthFilter
    participant OrderController
    participant OrderDAO
    participant OrderItemDAO
    participant Database
    
    User->>AuthFilter: GET /orders
    AuthFilter->>AuthFilter: Check session
    alt Not Logged In
        AuthFilter-->>User: Redirect to /login
    else Logged In
        AuthFilter->>OrderController: Forward request
        OrderController->>OrderDAO: getOrdersByUserId(userId)
        OrderDAO->>Database: SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC
        Database-->>OrderDAO: orders
        OrderDAO-->>OrderController: orders
        
        loop For each order
            OrderController->>OrderItemDAO: getOrderItemsByOrderId(orderId)
            OrderItemDAO->>Database: SELECT * FROM order_items WHERE order_id = ?
            Database-->>OrderItemDAO: order items
            OrderItemDAO-->>OrderController: order items
        end
        
        OrderController-->>User: Forward to orders.jsp
    end
```

## Search Flow

```mermaid
sequenceDiagram
    participant User
    participant SearchController
    participant SearchService
    participant ProductDAO
    participant Database
    participant CacheService
    
    User->>SearchController: GET /search?q=keyword
    SearchController->>SearchService: searchProducts(keyword)
    SearchService->>CacheService: get("search:" + keyword)
    
    alt Cache Hit
        CacheService-->>SearchService: products
    else Cache Miss
        SearchService->>ProductDAO: searchProducts(keyword)
        ProductDAO->>Database: SELECT * FROM products WHERE name LIKE ? OR description LIKE ?
        Database-->>ProductDAO: products
        ProductDAO-->>SearchService: products
        SearchService->>CacheService: put("search:" + keyword, products, 30 minutes)
    end
    
    SearchService-->>SearchController: products
    SearchController-->>User: Forward to search results page
```

## Wishlist Flow

```mermaid
sequenceDiagram
    participant User
    participant AuthFilter
    participant WishlistController
    participant WishlistDAO
    participant Database
    participant CacheService
    
    User->>AuthFilter: POST /wishlist/add (productId)
    AuthFilter->>AuthFilter: Check session
    alt Not Logged In
        AuthFilter-->>User: Redirect to /login
    else Logged In
        AuthFilter->>WishlistController: Forward request
        WishlistController->>WishlistDAO: addToWishlist(userId, productId)
        WishlistDAO->>Database: INSERT INTO wishlist (user_id, product_id)
        Database-->>WishlistDAO: success
        WishlistDAO-->>WishlistController: true
        
        WishlistController->>CacheService: remove("wishlist:user:" + userId)
        
        WishlistController-->>User: JSON response {success: true}
    end
```

## Admin Product Management Flow

```mermaid
sequenceDiagram
    participant Admin
    participant AuthFilter
    participant AdminProductController
    participant ProductService
    participant ProductDAO
    participant ProductSizeDAO
    participant Database
    participant CacheService
    
    Admin->>AuthFilter: POST /admin/products/add (product details)
    AuthFilter->>AuthFilter: Check admin role
    alt Not Admin
        AuthFilter-->>Admin: 403 Forbidden
    else Admin
        AuthFilter->>AdminProductController: Forward request
        AdminProductController->>ProductService: addProduct(product)
        ProductService->>ProductDAO: addProduct(product)
        ProductDAO->>Database: INSERT INTO products
        Database-->>ProductDAO: productId
        ProductDAO-->>ProductService: productId
        
        loop For each size
            ProductService->>ProductSizeDAO: addProductSize(productId, size, stock)
            ProductSizeDAO->>Database: INSERT INTO product_sizes
            Database-->>ProductSizeDAO: success
        end
        
        ProductService->>CacheService: invalidatePattern("fashionstore:products:*")
        
        ProductService-->>AdminProductController: productId
        AdminProductController-->>Admin: Redirect to /admin/products
    end
```

## Payment Flow

```mermaid
sequenceDiagram
    participant User
    participant AuthFilter
    participant PaymentController
    participant PaymentService
    participant PaymentDAO
    participant PaymentMethodDAO
    participant Database
    participant EmailService
    
    User->>AuthFilter: POST /payment/process (payment details)
    AuthFilter->>AuthFilter: Check session
    alt Not Logged In
        AuthFilter-->>User: Redirect to /login
    else Logged In
        AuthFilter->>PaymentController: Forward request
        PaymentController->>PaymentService: processPayment(paymentDetails)
        PaymentService->>PaymentMethodDAO: getPaymentMethodById(paymentMethodId)
        PaymentMethodDAO->>Database: SELECT * FROM payment_methods WHERE payment_method_id = ?
        Database-->>PaymentMethodDAO: payment method
        PaymentMethodDAO-->>PaymentService: payment method
        
        PaymentService->>PaymentService: Validate payment details
        alt Invalid Payment
            PaymentService-->>PaymentController: error
            PaymentController-->>User: Payment failed
        else Valid Payment
            PaymentService->>PaymentDAO: createPayment(payment)
            PaymentDAO->>Database: INSERT INTO payments
            Database-->>PaymentDAO: paymentId
            PaymentDAO-->>PaymentService: paymentId
            
            PaymentService->>Database: UPDATE orders SET payment_status = 'paid' WHERE order_id = ?
            PaymentService->>EmailService: sendPaymentConfirmationEmail(user, order)
            EmailService-->>PaymentService: email sent
            
            PaymentService-->>PaymentController: success
            PaymentController-->>User: Redirect to /success
        end
    end
```

## Cache Invalidation Flow

```mermaid
sequenceDiagram
    participant Admin
    participant AdminProductController
    participant ProductService
    participant ProductDAO
    participant CacheService
    
    Admin->>AdminProductController: POST /admin/products/update
    AdminProductController->>ProductService: updateProduct(product)
    ProductService->>ProductDAO: updateProduct(product)
    ProductDAO-->>ProductService: success
    
    ProductService->>CacheService: remove("fashionstore:product:" + productId)
    CacheService-->>ProductService: success
    
    ProductService->>CacheService: invalidatePattern("fashionstore:products:*")
    CacheService->>CacheService: Remove all matching keys
    CacheService-->>ProductService: success
    
    ProductService-->>AdminProductController: success
```

## Password Reset Flow

```mermaid
sequenceDiagram
    participant User
    participant PasswordResetController
    participant UserService
    participant UserDAO
    participant PasswordResetTokenDAO
    participant Database
    participant EmailService
    
    User->>PasswordResetController: POST /forgot-password (email)
    PasswordResetController->>UserService: getUserByEmail(email)
    UserService->>UserDAO: getUserByEmail(email)
    UserDAO->>Database: SELECT * FROM users WHERE email = ?
    Database-->>UserDAO: user
    
    alt User not found
        UserDAO-->>UserService: null
        UserService-->>PasswordResetController: null
        PasswordResetController-->>User: If email exists, reset link sent
    else User found
        UserDAO-->>UserService: user
        UserService-->>PasswordResetController: user
        
        PasswordResetController->>PasswordResetTokenDAO: createResetToken(userId)
        PasswordResetTokenDAO->>Database: INSERT INTO password_reset_tokens
        Database-->>PasswordResetTokenDAO: token
        
        PasswordResetController->>EmailService: sendPasswordResetEmail(email, resetLink)
        EmailService-->>PasswordResetController: email sent
        
        PasswordResetController-->>User: Reset link sent
    end
```

## Review Submission Flow

```mermaid
sequenceDiagram
    participant User
    participant AuthFilter
    participant ReviewController
    participant ReviewDAO
    participant ProductDAO
    participant Database
    
    User->>AuthFilter: POST /review/submit (productId, rating, comment)
    AuthFilter->>AuthFilter: Check session
    alt Not Logged In
        AuthFilter-->>User: Redirect to /login
    else Logged In
        AuthFilter->>ReviewController: Forward request
        ReviewController->>ReviewDAO: addReview(review)
        ReviewDAO->>Database: INSERT INTO reviews (user_id, product_id, rating, comment)
        Database-->>ReviewDAO: reviewId
        ReviewDAO-->>ReviewController: reviewId
        
        ReviewController->>ProductDAO: updateProductRating(productId)
        ProductDAO->>Database: UPDATE products SET rating = (AVG rating) WHERE product_id = ?
        Database-->>ProductDAO: success
        
        ReviewController-->>User: Review submitted successfully
    end
```
