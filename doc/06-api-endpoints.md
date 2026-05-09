# FashionStore API Endpoints Documentation

## Overview

This document describes all HTTP endpoints exposed by the FashionStore application. The application follows RESTful conventions where applicable, with both HTML view responses and JSON API responses for AJAX requests.

## Base URL
```
http://localhost:8080/FashionStore
```

## Authentication Requirements

| Endpoint | Authentication Required | Role Required |
|----------|------------------------|---------------|
| `/login` | No | None |
| `/register` | No | None |
| `/home` | No | None |
| `/products` | No | None |
| `/product` | No | None |
| `/search` | No | None |
| `/cart/*` | Yes | Customer/Admin |
| `/wishlist/*` | Yes | Customer/Admin |
| `/checkout` | Yes | Customer/Admin |
| `/orders` | Yes | Customer/Admin |
| `/payment/*` | Yes | Customer/Admin |
| `/review/*` | Yes | Customer/Admin |
| `/admin/*` | Yes | Admin |

## Public Endpoints

### Authentication

#### POST /login
User login endpoint.

**Request:**
```
POST /FashionStore/login
Content-Type: application/x-www-form-urlencoded

email=user@example.com&password=secret123&csrf_token=abc123
```

**Parameters:**
- `email` (string, required): User email
- `password` (string, required): User password
- `csrf_token` (string, required): CSRF token from session

**Response:**
- Success: Redirect to `/home`
- Failure: Forward to `/login` with error message
- Rate Limit Exceeded: Error message

**Security:**
- CSRF protection
- Rate limiting (5 attempts per 15 minutes)
- BCrypt password verification

---

#### POST /register
User registration endpoint.

**Request:**
```
POST /FashionStore/register
Content-Type: application/x-www-form-urlencoded

fullName=John+Doe&email=john@example.com&password=Secret123&phone=9876543210&gender=Male&address=123+Street&csrf_token=abc123
```

**Parameters:**
- `fullName` (string, required): User's full name
- `email` (string, required): User's email
- `password` (string, required): User's password (min 8 chars, 1 uppercase, 1 lowercase, 1 digit)
- `phone` (string, optional): Phone number (10-digit Indian format)
- `gender` (string, optional): User's gender
- `address` (string, optional): User's address
- `csrf_token` (string, required): CSRF token from session

**Response:**
- Success: Redirect to `/login`
- Failure: Forward to `/register` with error message

**Security:**
- CSRF protection
- Input validation
- BCrypt password hashing

---

#### GET /logout
User logout endpoint.

**Request:**
```
GET /FashionStore/logout
```

**Response:**
- Success: Redirect to `/home`

**Security:**
- Invalidates session
- Removes CSRF token

---

### Product Endpoints

#### GET /home
Homepage with featured products.

**Request:**
```
GET /FashionStore/home
```

**Response:**
- HTML view with featured products and categories

**Attributes Set:**
- `featuredProducts`: List of trending products
- `categories`: List of product categories

---

#### GET /products
Product listing with filtering.

**Request:**
```
GET /FashionStore/products?category=1&sort=price_asc&minPrice=1000&maxPrice=5000
```

**Query Parameters:**
- `category` (integer, optional): Filter by category ID
- `sort` (string, optional): Sort order (price_asc, price_desc, name_asc, name_desc, newest)
- `minPrice` (decimal, optional): Minimum price filter
- `maxPrice` (decimal, optional): Maximum price filter
- `brand` (string, optional): Filter by brand
- `isNew` (boolean, optional): Filter new arrivals
- `isSale` (boolean, optional): Filter sale items
- `isTrending` (boolean, optional): Filter trending items

**Response:**
- HTML view with filtered products

**Attributes Set:**
- `products`: List of filtered products
- `categories`: List of all categories
- `filters`: Applied filters

---

#### GET /product
Product details page.

**Request:**
```
GET /FashionStore/product?id=123
```

**Query Parameters:**
- `id` (integer, required): Product ID

**Response:**
- HTML view with product details

**Attributes Set:**
- `product`: Product object with sizes
- `reviews`: List of product reviews
- `averageRating`: Average rating
- `csrfToken`: CSRF token for forms

---

#### GET /search
Product search endpoint.

**Request:**
```
GET /FashionStore/search?q=blazer&category=1&minPrice=1000&maxPrice=5000
```

**Query Parameters:**
- `q` (string, required): Search query
- `category` (integer, optional): Filter by category
- `minPrice` (decimal, optional): Minimum price
- `maxPrice` (decimal, optional): Maximum price
- `brand` (string, optional): Filter by brand

**Response:**
- HTML view with search results

**Attributes Set:**
- `products`: List of matching products
- `searchQuery`: Search query
- `resultCount`: Number of results

---

### Password Reset

#### POST /forgot-password
Initiate password reset.

**Request:**
```
POST /FashionStore/forgot-password
Content-Type: application/x-www-form-urlencoded

email=user@example.com
```

**Parameters:**
- `email` (string, required): User email

**Response:**
- Success: Message indicating reset link sent
- Failure: Error message

---

#### GET /reset-password
Password reset form.

**Request:**
```
GET /FashionStore/reset-password?token=abc123
```

**Query Parameters:**
- `token` (string, required): Reset token

**Response:**
- HTML view with reset form

---

#### POST /reset-password
Complete password reset.

**Request:**
```
POST /FashionStore/reset-password
Content-Type: application/x-www-form-urlencoded

token=abc123&newPassword=NewSecret123&confirmPassword=NewSecret123
```

**Parameters:**
- `token` (string, required): Reset token
- `newPassword` (string, required): New password
- `confirmPassword` (string, required): Confirm password

**Response:**
- Success: Redirect to `/login`
- Failure: Error message

---

## Private Endpoints (Authentication Required)

### Shopping Cart

#### GET /cart
View shopping cart.

**Request:**
```
GET /FashionStore/cart
```

**Response:**
- HTML view with cart items

**Attributes Set:**
- `cartItems`: List of cart items
- `cartTotal`: Total cart value
- `csrfToken`: CSRF token

---

#### POST /cart/add
Add item to cart.

**Request:**
```
POST /FashionStore/cart/add
Content-Type: application/x-www-form-urlencoded

productId=123&sizeLabel=M&quantity=2&csrf_token=abc123
```

**Parameters:**
- `productId` (integer, required): Product ID
- `sizeLabel` (string, required): Size label
- `quantity` (integer, required): Quantity
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Item added to cart",
  "cartCount": 5
}
```

**Response (Non-AJAX):**
- Redirect to `/cart`

---

#### POST /cart/update
Update cart item quantity.

**Request:**
```
POST /FashionStore/cart/update
Content-Type: application/x-www-form-urlencoded

cartItemId=456&quantity=3&csrf_token=abc123
```

**Parameters:**
- `cartItemId` (integer, required): Cart item ID
- `quantity` (integer, required): New quantity
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Cart updated",
  "cartTotal": 9999.00
}
```

---

#### POST /cart/remove
Remove item from cart.

**Request:**
```
POST /FashionStore/cart/remove
Content-Type: application/x-www-form-urlencoded

cartItemId=456&csrf_token=abc123
```

**Parameters:**
- `cartItemId` (integer, required): Cart item ID
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Item removed from cart",
  "cartCount": 4
}
```

---

#### POST /cart/clear
Clear entire cart.

**Request:**
```
POST /FashionStore/cart/clear
Content-Type: application/x-www-form-urlencoded

csrf_token=abc123
```

**Parameters:**
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Cart cleared"
}
```

---

### Wishlist

#### GET /wishlist
View wishlist.

**Request:**
```
GET /FashionStore/wishlist
```

**Response:**
- HTML view with wishlist items

**Attributes Set:**
- `wishlistItems`: List of wishlist items
- `csrfToken`: CSRF token

---

#### POST /wishlist/add
Add item to wishlist.

**Request:**
```
POST /FashionStore/wishlist/add
Content-Type: application/x-www-form-urlencoded

productId=123&csrf_token=abc123
```

**Parameters:**
- `productId` (integer, required): Product ID
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Item added to wishlist"
}
```

---

#### POST /wishlist/remove
Remove item from wishlist.

**Request:**
```
POST /FashionStore/wishlist/remove
Content-Type: application/x-www-form-urlencoded

wishlistId=789&csrf_token=abc123
```

**Parameters:**
- `wishlistId` (integer, required): Wishlist item ID
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Item removed from wishlist"
}
```

---

### Checkout

#### GET /checkout
Checkout page.

**Request:**
```
GET /FashionStore/checkout
```

**Response:**
- HTML view with checkout form

**Attributes Set:**
- `cartItems`: List of cart items
- `cartTotal`: Total cart value
- `csrfToken`: CSRF token

---

#### POST /checkout
Place order.

**Request:**
```
POST /FashionStore/checkout
Content-Type: application/x-www-form-urlencoded

fullName=John+Doe&address=123+Street&city=Mumbai&state=MH&zip=400001&phone=9876543210&paymentMethod=COD&csrf_token=abc123
```

**Parameters:**
- `fullName` (string, required): Recipient name
- `address` (string, required): Shipping address
- `city` (string, required): City
- `state` (string, required): State
- `zip` (string, required): ZIP code
- `phone` (string, required): Contact phone
- `paymentMethod` (string, required): Payment method (COD, CARD, UPI)
- `csrf_token` (string, required): CSRF token

**Response:**
- Success: Redirect to `/success`
- Failure: Forward to `/checkout` with error message

**Transaction:**
- Atomic transaction with stock reduction, order creation, cart clearing
- Rollback on any failure

---

### Orders

#### GET /orders
Order history.

**Request:**
```
GET /FashionStore/orders
```

**Response:**
- HTML view with order history

**Attributes Set:**
- `orders`: List of user orders

---

#### GET /order
Order details.

**Request:**
```
GET /FashionStore/order?id=123
```

**Query Parameters:**
- `id` (integer, required): Order ID

**Response:**
- HTML view with order details

**Attributes Set:**
- `order`: Order object
- `orderItems`: List of order items

---

### Payment

#### GET /payment
Payment page.

**Request:**
```
GET /FashionStore/payment?orderId=123
```

**Query Parameters:**
- `orderId` (integer, required): Order ID

**Response:**
- HTML view with payment form

**Attributes Set:**
- `order`: Order object
- `paymentMethods`: List of user's payment methods
- `csrfToken`: CSRF token

---

#### POST /payment/process
Process payment.

**Request:**
```
POST /FashionStore/payment/process
Content-Type: application/x-www-form-urlencoded

orderId=123&paymentMethodId=456&csrf_token=abc123
```

**Parameters:**
- `orderId` (integer, required): Order ID
- `paymentMethodId` (integer, required): Payment method ID
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Payment successful",
  "paymentId": 789
}
```

**Response (Failure):**
```json
{
  "success": false,
  "message": "Payment failed: Insufficient funds"
}
```

---

#### POST /payment/method/add
Add payment method.

**Request:**
```
POST /FashionStore/payment/method/add
Content-Type: application/x-www-form-urlencoded

methodType=credit_card&provider=Visa&cardNumber=4111111111111111&expiryMonth=12&expiryYear=2025&cvv=123&csrf_token=abc123
```

**Parameters:**
- `methodType` (string, required): Payment method type
- `provider` (string, required): Payment provider
- `cardNumber` (string, required): Card number
- `expiryMonth` (integer, required): Expiry month
- `expiryYear` (integer, required): Expiry year
- `cvv` (string, required): CVV
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Payment method added"
}
```

---

### Reviews

#### POST /review/submit
Submit product review.

**Request:**
```
POST /FashionStore/review/submit
Content-Type: application/x-www-form-urlencoded

productId=123&rating=5&comment=Great+product&csrf_token=abc123
```

**Parameters:**
- `productId` (integer, required): Product ID
- `rating` (integer, required): Rating (1-5)
- `comment` (string, optional): Review comment
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Review submitted successfully"
}
```

---

## Admin Endpoints (Admin Role Required)

### Admin Dashboard

#### GET /admin
Admin dashboard.

**Request:**
```
GET /FashionStore/admin
```

**Response:**
- HTML view with dashboard analytics

**Attributes Set:**
- `totalRevenue`: Total revenue
- `totalOrders`: Total orders
- `totalUsers`: Total users
- `recentOrders`: Recent orders
- `recentUsers`: Recent users

---

### Admin Products

#### GET /admin/products
Product management page.

**Request:**
```
GET /FashionStore/admin/products
```

**Response:**
- HTML view with product list

**Attributes Set:**
- `products`: List of all products
- `categories`: List of categories

---

#### POST /admin/products/add
Add new product.

**Request:**
```
POST /FashionStore/admin/products/add
Content-Type: multipart/form-data

productName=New+Product&description=Description&price=2999&categoryId=1&brand=Brand&image=file&csrf_token=abc123
```

**Parameters:**
- `productName` (string, required): Product name
- `description` (string, required): Product description
- `price` (decimal, required): Product price
- `categoryId` (integer, required): Category ID
- `brand` (string, optional): Product brand
- `image` (file, optional): Product image
- `csrf_token` (string, required): CSRF token

**Response:**
- Success: Redirect to `/admin/products`
- Failure: Error message

---

#### POST /admin/products/update
Update product.

**Request:**
```
POST /FashionStore/admin/products/update
Content-Type: multipart/form-data

productId=123&productName=Updated+Product&price=3499&csrf_token=abc123
```

**Parameters:**
- `productId` (integer, required): Product ID
- `productName` (string, required): Product name
- `description` (string, optional): Product description
- `price` (decimal, required): Product price
- `categoryId` (integer, optional): Category ID
- `brand` (string, optional): Product brand
- `image` (file, optional): Product image
- `csrf_token` (string, required): CSRF token

**Response:**
- Success: Redirect to `/admin/products`
- Failure: Error message

---

#### POST /admin/products/delete
Delete product.

**Request:**
```
POST /FashionStore/admin/products/delete
Content-Type: application/x-www-form-urlencoded

productId=123&csrf_token=abc123
```

**Parameters:**
- `productId` (integer, required): Product ID
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "Product deleted"
}
```

---

### Admin Users

#### GET /admin/users
User management page.

**Request:**
```
GET /FashionStore/admin/users
```

**Response:**
- HTML view with user list

**Attributes Set:**
- `users`: List of all users

---

#### POST /admin/users/update-role
Update user role.

**Request:**
```
POST /FashionStore/admin/users/update-role
Content-Type: application/x-www-form-urlencoded

userId=123&role=admin&csrf_token=abc123
```

**Parameters:**
- `userId` (integer, required): User ID
- `role` (string, required): New role (customer/admin)
- `csrf_token` (string, required): CSRF token

**Response:**
- Success: Redirect to `/admin/users`
- Failure: Error message

---

#### POST /admin/users/delete
Delete user.

**Request:**
```
POST /FashionStore/admin/users/delete
Content-Type: application/x-www-form-urlencoded

userId=123&csrf_token=abc123
```

**Parameters:**
- `userId` (integer, required): User ID
- `csrf_token` (string, required): CSRF token

**Response (AJAX):**
```json
{
  "success": true,
  "message": "User deleted"
}
```

---

### Admin Orders

#### GET /admin/orders
Order management page.

**Request:**
```
GET /FashionStore/admin/orders
```

**Response:**
- HTML view with order list

**Attributes Set:**
- `orders`: List of all orders

---

#### GET /admin/orders/details
Order details page.

**Request:**
```
GET /FashionStore/admin/orders/details?id=123
```

**Query Parameters:**
- `id` (integer, required): Order ID

**Response:**
- HTML view with order details

**Attributes Set:**
- `order`: Order object
- `orderItems`: List of order items

---

#### POST /admin/orders/update-status
Update order status.

**Request:**
```
POST /FashionStore/admin/orders/update-status
Content-Type: application/x-www-form-urlencoded

orderId=123&status=SHIPPED&trackingNumber=TRACK123&csrf_token=abc123
```

**Parameters:**
- `orderId` (integer, required): Order ID
- `status` (string, required): New status (PLACED, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
- `trackingNumber` (string, optional): Tracking number
- `csrf_token` (string, required): CSRF token

**Response:**
- Success: Redirect to `/admin/orders`
- Failure: Error message

---

## Response Codes

| Status Code | Description |
|-------------|-------------|
| 200 | Success |
| 302 | Redirect |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (not logged in) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found |
| 500 | Internal Server Error |

## Error Response Format

### HTML Error Response
```
Error page with error message
```

### JSON Error Response (AJAX)
```json
{
  "success": false,
  "message": "Error description",
  "redirect": "/login"
}
```

## CSRF Token Handling

### Getting CSRF Token
CSRF token is automatically generated and stored in session for GET requests. It can be accessed in JSP:

```jsp
<input type="hidden" name="csrf_token" value="${csrfToken}" />
```

### AJAX CSRF Token
For AJAX requests, include CSRF token in headers:

```javascript
headers: {
  'X-CSRF-Token': '${csrfToken}'
}
```

## Security Headers

All responses include the following security headers:

- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; img-src 'self' data: https:; font-src 'self' data: https://fonts.gstatic.com https://fonts.googleapis.com; connect-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'self'`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: geolocation=(), microphone=(), camera=(), payment=()`

## Rate Limiting

| Endpoint | Limit | Duration |
|----------|-------|----------|
| /login (POST) | 5 attempts | 15 minutes |

## File Upload

### Supported File Types
- Images: JPG, JPEG, PNG, GIF, WEBP

### Max File Size
- 5MB

### Upload Endpoints
- `/admin/products/add` (product image)
- `/admin/products/update` (product image)
