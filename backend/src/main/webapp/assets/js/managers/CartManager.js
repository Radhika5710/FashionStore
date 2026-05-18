/**
 * FashionStore - Consolidated Cart Manager
 * 
 * Single source of truth for all cart operations:
 * - Add to cart
 * - Update cart quantity
 * - Remove from cart
 * - Fetch cart data
 * - Mini cart UI updates
 * - Cart drawer management
 * 
 * Consolidates logic from:
 * - main.js (addToCart, updateMiniCartQty, removeMiniCartItem, fetchCart)
 * - cart.js (updateCart, syncMiniCart)
 * - cart-drawer.js (toggleMiniCart, fetchCart)
 */

const CartManager = (function() {
    const contextPath = window.contextPath || '';
    
    // Track in-flight requests to prevent duplicates
    const requestTracker = new Map();
    const MAX_REQUEST_TIMEOUT = 10000; // 10 seconds
    
    // Track concurrent add-to-cart requests
    const cartAddInProgress = new Set();
    const miniCartUpdateInProgress = new Set();
    
    // ========================================================================
    // PUBLIC API
    // ========================================================================
    
    return {
        /**
         * Add item to cart
         * @param {number} productId - Product ID
         * @param {string} size - Size (default 'M')
         * @param {number} quantity - Quantity (default 1)
         * @returns {Promise}
         */
        addToCart: function(productId, size = 'M', quantity = 1, button = null) {
            if (!productId) {
                console.error('addToCart: productId is required');
                FashionStore.showToast('Failed to add to cart - missing product ID', 'error');
                return Promise.reject('Missing productId');
            }
            
            // Prevent concurrent add-to-cart requests for the same product + size combination
            const lockKey = `${productId}_${size}`;
            if (cartAddInProgress.has(lockKey)) {
                console.warn('Cart addition already in progress for:', lockKey);
                return Promise.resolve();
            }
            cartAddInProgress.add(lockKey);
            
            // Use provided button or fallback to event target
            const targetButton = button || (typeof event !== 'undefined' && event && event.target ? event.target : null);
            if (targetButton) {
                FashionStore.showLoading(targetButton, 'Adding...');
            }

            return FashionStoreAPI.post('/cart', {
                action: 'add',
                productId,
                size,
                quantity
            })
            .then(data => {
                if (!data) return;
                if (data.success || data.status === 'success') {
                    this.updateMiniCartUI(data);
                    FashionStore.showToast("Added to cart", 'success');
                    
                    // Update navbar badge
                    const badgeEl = document.getElementById('nav-cart-badge');
                    if (badgeEl && data.cartCount !== undefined) {
                        badgeEl.innerText = data.cartCount;
                    }
                    
                    // Open drawer after a short delay
                    setTimeout(() => {
                        document.getElementById('mini-cart-overlay')?.classList.add('active');
                        document.getElementById('mini-cart-drawer')?.classList.add('active');
                    }, 300);
                } else {
                    FashionStore.showToast(data.message || "Failed to add to cart", 'error');
                }
            })
            .catch(err => {
                console.error("Error adding to cart:", err);
                FashionStore.showToast("Failed to add to cart: " + err.message, 'error');
            })
            .finally(() => {
                if (targetButton) {
                    FashionStore.hideLoading(targetButton);
                }
                cartAddInProgress.delete(lockKey);
            });
        },
        
        /**
         * Update cart item quantity
         * @param {number} cartItemId - Cart item ID
         * @param {number} newQty - New quantity
         */
        updateCartQty: function(cartItemId, newQty) {
            if (miniCartUpdateInProgress.has(cartItemId)) {
                console.warn('Mini-cart update already in progress for item:', cartItemId);
                return;
            }
            miniCartUpdateInProgress.add(cartItemId);

            FashionStoreAPI.post('/cart', {
                action: 'update',
                cartItemId,
                currentQty: newQty
            })
            .then(data => {
                if (!data) return;
                if (data.success || data.status === 'success') {
                    this.updateMiniCartUI(data);
                    FashionStore.showToast('Cart updated', 'success');
                } else {
                    FashionStore.showToast(data.message || 'Failed to update cart', 'error');
                }
            })
            .catch(err => {
                console.error('Mini cart update error:', err);
                FashionStore.showToast(err.message || 'Failed to update cart', 'error');
            })
            .finally(() => {
                miniCartUpdateInProgress.delete(cartItemId);
            });
        },
        
        /**
         * Remove item from cart
         * @param {number} cartItemId - Cart item ID
         */
        removeCartItem: function(cartItemId) {
            if (!cartItemId) return;
            
            if (miniCartUpdateInProgress.has(cartItemId)) {
                console.warn('Mini-cart remove already in progress for item:', cartItemId);
                return;
            }
            miniCartUpdateInProgress.add(cartItemId);

            const itemEl = document.querySelector(`.mini-cart-item[data-id="${cartItemId}"]`);
            if (itemEl) {
                itemEl.classList.add('removing');
            }

            FashionStoreAPI.post('/cart', {
                action: 'remove',
                cartItemId,
                currentQty: 0
            })
            .then(data => {
                if (!data) return;
                if (data.success || data.status === 'success') {
                    this.updateMiniCartUI(data);
                    FashionStore.showToast('Item removed', 'success');
                } else {
                    FashionStore.showToast(data.message || 'Failed to remove item', 'error');
                    if (itemEl) itemEl.classList.remove('removing');
                }
            })
            .catch(err => {
                console.error('Mini cart remove error:', err);
                FashionStore.showToast(err.message || 'Failed to remove item', 'error');
                if (itemEl) itemEl.classList.remove('removing');
            })
            .finally(() => {
                miniCartUpdateInProgress.delete(cartItemId);
            });
        },
        
        /**
         * Fetch cart data
         * @returns {Promise}
         */
        fetchCart: function() {
            return FashionStoreAPI.post('/cart', { action: 'get' })
            .then(data => {
                this.updateMiniCartUI(data);
            })
            .catch(err => console.error("Error fetching cart:", err));
        },
        
        /**
         * Update mini cart UI with data
         * @param {object} data - Cart data from server
         */
        updateMiniCartUI: function(data) {
            if (!data) return;
            
            const container = document.getElementById('mini-cart-items');
            if (!container) return;
            
            if (data.cartItems && data.cartItems.length > 0) {
                // Update items list
                container.innerHTML = data.cartItems.map(item => `
                    <div class="mini-cart-item" data-id="${item.cartItemId}">
                        <div class="mini-cart-item-info">
                            <div class="mini-cart-item-name">${item.productName}</div>
                            <div class="mini-cart-item-price">₹${item.price}</div>
                        </div>
                        <div class="mini-cart-item-qty">
                            <button class="mini-cart-qty-decrease" data-id="${item.cartItemId}" data-qty="${item.quantity}">−</button>
                            <span>${item.quantity}</span>
                            <button class="mini-cart-qty-increase" data-id="${item.cartItemId}" data-qty="${item.quantity}">+</button>
                        </div>
                        <button class="mini-cart-remove-btn" data-id="${item.cartItemId}">✕</button>
                    </div>
                `).join('');
                
                // Update totals
                const totalEl = document.getElementById('mini-cart-total');
                if (totalEl) {
                    totalEl.innerText = `₹${data.cartTotal || 0}`;
                }
                
                // Update badge
                const badgeEl = document.getElementById('nav-cart-badge');
                if (badgeEl && data.cartCount !== undefined) {
                    badgeEl.innerText = data.cartCount;
                }
                
                // Show content, hide empty state
                const emptyState = document.getElementById('mini-cart-empty');
                const cartContent = document.getElementById('mini-cart-content');
                if (emptyState && cartContent) {
                    emptyState.style.display = 'none';
                    cartContent.style.display = 'block';
                }
                
                // Re-attach event listeners
                this.attachMiniCartListeners();
            } else {
                // Show empty state
                const emptyState = document.getElementById('mini-cart-empty');
                const cartContent = document.getElementById('mini-cart-content');
                if (emptyState && cartContent) {
                    emptyState.style.display = 'block';
                    cartContent.style.display = 'none';
                }
            }
        },
        
        /**
         * Attach event listeners to mini cart buttons
         */
        attachMiniCartListeners: function() {
            const container = document.getElementById('mini-cart-items');
            if (!container) return;
            
            // Remove old listeners by cloning
            const newContainer = container.cloneNode(true);
            container.parentNode.replaceChild(newContainer, container);
            
            // Attach new listeners with event delegation
            newContainer.addEventListener('click', (e) => {
                const decreaseBtn = e.target.closest('.mini-cart-qty-decrease');
                const increaseBtn = e.target.closest('.mini-cart-qty-increase');
                const removeBtn = e.target.closest('.mini-cart-remove-btn');
                
                if (decreaseBtn) {
                    e.preventDefault();
                    const cartItemId = decreaseBtn.getAttribute('data-id');
                    const currentQty = parseInt(decreaseBtn.getAttribute('data-qty') || 1);
                    if (cartItemId && currentQty > 1) {
                        this.updateCartQty(cartItemId, currentQty - 1);
                    }
                } else if (increaseBtn) {
                    e.preventDefault();
                    const cartItemId = increaseBtn.getAttribute('data-id');
                    const currentQty = parseInt(increaseBtn.getAttribute('data-qty') || 1);
                    if (cartItemId) {
                        this.updateCartQty(cartItemId, currentQty + 1);
                    }
                } else if (removeBtn) {
                    e.preventDefault();
                    const cartItemId = removeBtn.getAttribute('data-id');
                    if (cartItemId) {
                        this.removeCartItem(cartItemId);
                    }
                }
            });
        },
        
        /**
         * Toggle mini cart drawer visibility
         * @param {Event} event - Click event
         */
        toggleMiniCart: function(event) {
            if (event) event.preventDefault();
            const overlay = document.getElementById('mini-cart-overlay');
            const drawer = document.getElementById('mini-cart-drawer');
            
            if (!overlay || !drawer) {
                console.warn('Mini cart elements not found');
                return;
            }
            
            if (overlay.classList.contains('active')) {
                overlay.classList.remove('active');
                drawer.classList.remove('active');
                document.body.classList.remove('cart-drawer-open');
            } else {
                overlay.classList.add('active');
                drawer.classList.add('active');
                document.body.classList.add('cart-drawer-open');
                this.fetchCart();
            }
        },
        
        /**
         * Initialize cart manager
         */
        init: function() {
            // Setup cart drawer toggle
            const cartToggle = document.getElementById('cart-toggle');
            if (cartToggle) {
                cartToggle.addEventListener('click', (e) => this.toggleMiniCart(e));
            }
            
            // Setup overlay click to close
            const overlay = document.getElementById('mini-cart-overlay');
            if (overlay) {
                overlay.addEventListener('click', (e) => {
                    if (e.target === overlay) {
                        this.toggleMiniCart(e);
                    }
                });
            }
            
            // Setup escape key to close
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') {
                    const overlay = document.getElementById('mini-cart-overlay');
                    if (overlay && overlay.classList.contains('active')) {
                        this.toggleMiniCart();
                    }
                }
            });
            
            // Attach mini cart listeners
            this.attachMiniCartListeners();
        }
    };
})();

// Initialize on DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => CartManager.init());
} else {
    CartManager.init();
}

// Export for use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CartManager;
}

// Make available globally
window.CartManager = CartManager;
