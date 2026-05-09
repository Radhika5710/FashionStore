<%@ page contentType="text/html;charset=UTF-8" %>

<footer class="footer">
    <div class="footer-inner">
        <div class="footer-grid">
            <div class="footer-brand">
                <div class="footer-brand-logo">FashionStore</div>
                <p class="footer-brand-description">Modern fashion marketplace with curated styles for every season. Discover elevated essentials inspired by global streetwear.</p>
                <div class="footer-social">
                    <a href="#" class="footer-social-link" aria-label="Instagram">
                        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect><path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path><line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line></svg>
                    </a>
                    <a href="#" class="footer-social-link" aria-label="Twitter">
                        <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"></path></svg>
                    </a>
                </div>
            </div>
            <div class="footer-section">
                <h4 class="footer-heading">Shop</h4>
                <div class="footer-links">
                    <a href="<%= request.getContextPath() %>/products" class="footer-link">All Products</a>
                    <a href="<%= request.getContextPath() %>/products?category=men" class="footer-link">Men</a>
                    <a href="<%= request.getContextPath() %>/products?category=women" class="footer-link">Women</a>
                    <a href="<%= request.getContextPath() %>/products?category=footwear" class="footer-link">Footwear</a>
                    <a href="<%= request.getContextPath() %>/products?category=accessories" class="footer-link">Accessories</a>
                </div>
            </div>
            <div class="footer-section">
                <h4 class="footer-heading">Account</h4>
                <div class="footer-links">
                    <a href="<%= request.getContextPath() %>/cart" class="footer-link">My Cart</a>
                    <a href="<%= request.getContextPath() %>/wishlist" class="footer-link">Wishlist</a>
                    <a href="<%= request.getContextPath() %>/orders" class="footer-link">Orders</a>
                </div>
            </div>
            <div class="footer-section">
                <h4 class="footer-heading">Support</h4>
                <div class="footer-links">
                    <a href="<%= request.getContextPath() %>/home" class="footer-link">Home</a>
                    <a href="<%= request.getContextPath() %>/login" class="footer-link">Login</a>
                    <a href="<%= request.getContextPath() %>/register" class="footer-link">Register</a>
                </div>
            </div>
            <div class="footer-section">
                <h4 class="footer-heading">Legal</h4>
                <div class="footer-links">
                    <a href="#" class="footer-link">Privacy Policy</a>
                    <a href="#" class="footer-link">Terms of Service</a>
                    <a href="#" class="footer-link">Shipping Info</a>
                </div>
            </div>
        </div>
        <div class="footer-bottom">
            <p class="footer-copyright">&copy; 2026 FashionStore. All rights reserved.</p>
            <div class="footer-legal">
                <a href="#" class="footer-legal-link">Privacy</a>
                <a href="#" class="footer-legal-link">Terms</a>
            </div>
        </div>
    </div>
</footer>
