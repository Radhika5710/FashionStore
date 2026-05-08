// Modern Commerce UX System
window.FashionStore = {
    // Toast System
    showToast: function(message, type = 'success') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `
            <div class="toast-icon">
                ${type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ'}
            </div>
            <div class="toast-message">${message}</div>
        `;
        
        container.appendChild(toast);

        // Trigger reflow for transition
        setTimeout(() => toast.classList.add('show'), 10);

        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    },

    // Loading States
    showLoading: function(element, text = 'Loading...') {
        if (!element) return;
        
        const originalContent = element.innerHTML;
        element.dataset.originalContent = originalContent;
        
        element.innerHTML = `
            <div class="loading-spinner">
                <div class="spinner"></div>
                <span>${text}</span>
            </div>
        `;
        element.disabled = true;
    },

    hideLoading: function(element) {
        if (!element || !element.dataset.originalContent) return;
        
        element.innerHTML = element.dataset.originalContent;
        element.disabled = false;
        delete element.dataset.originalContent;
    },

    // Skeleton Loaders
    showSkeleton: function(container, count = 3) {
        if (!container) return;
        
        let skeletonHTML = '';
        for (let i = 0; i < count; i++) {
            skeletonHTML += `
                <div class="skeleton-card">
                    <div class="skeleton skeleton-image"></div>
                    <div class="skeleton skeleton-title"></div>
                    <div class="skeleton skeleton-text"></div>
                    <div class="skeleton skeleton-price"></div>
                </div>
            `;
        }
        
        container.innerHTML = skeletonHTML;
    },

    hideSkeleton: function(container, content) {
        if (!container) return;
        container.innerHTML = content || '';
    },

    // Smooth Animations
    animateValue: function(element, start, end, duration = 300) {
        const range = end - start;
        const startTime = performance.now();
        
        const animate = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            const easeOutQuart = 1 - Math.pow(1 - progress, 4);
            const current = start + (range * easeOutQuart);
            
            element.textContent = current.toFixed(2);
            
            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };
        
        requestAnimationFrame(animate);
    },

    // Enhanced Cart Operations
    addToCart: function(productId, size = 'M', quantity = 1) {
        if (!productId) {
            console.error('addToCart: productId is required');
            FashionStore.showToast('Failed to add to cart - missing product ID', 'error');
            return Promise.reject('Missing productId');
        }
        
        const button = (typeof event !== 'undefined' && event && event.target) ? event.target : null;
        if (button) {
            FashionStore.showLoading(button, 'Adding...');
        }

        return fetch(`${contextPath}/cart`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: new URLSearchParams({
                action: 'add',
                productId: productId,
                size: size,
                quantity: quantity
            })
        })
        .then(res => {
            if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
            if(res.redirected) {
                window.location.href = res.url;
                return;
            }
            return res.json();
        })
        .then(data => {
            if(data.success || data.status === 'success') {
                FashionStore.updateMiniCartUI(data);
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
            if (button) {
                FashionStore.hideLoading(button);
            }
        });
    },

    // Enhanced Wishlist - Global function for inline handlers
    toggleWishlist: function(productId, button) {
        if (!productId) {
            console.error('toggleWishlist: productId is required');
            return Promise.reject('Missing productId');
        }
        
        if (button) {
            FashionStore.showLoading(button, '');
        }

        return fetch(`${contextPath}/wishlist`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: new URLSearchParams({
                action: 'toggle',
                productId: productId
            })
        })
        .then(res => {
            if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
            return res.json();
        })
        .then(data => {
            if(data.redirect) {
                window.location.href = data.redirect;
                return;
            }
            
            if(data.success) {
                FashionStore.showToast(data.message, 'success');
                
                // Update button state
                if (button) {
                    const icon = button.querySelector('.wishlist-icon');
                    if (icon) {
                        icon.classList.toggle('active', data.isFavorite);
                    }
                }
            } else {
                FashionStore.showToast(data.message || "Failed to update wishlist", 'error');
            }
        })
        .catch(err => {
            console.error("Error toggling wishlist:", err);
            FashionStore.showToast("Failed to update wishlist: " + err.message, 'error');
        })
        .finally(() => {
            if (button) {
                FashionStore.hideLoading(button);
            }
        });
    },

    // Product Reviews
    submitReview: function(productId, rating, comment) {
        const submitButton = document.querySelector('.submit-review-btn');
        if (submitButton) {
            FashionStore.showLoading(submitButton, 'Submitting...');
        }

        return fetch(`${contextPath}/review`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: new URLSearchParams({
                action: 'add',
                productId: productId,
                rating: rating,
                comment: comment
            })
        })
        .then(res => res.json())
        .then(data => {
            if(data.success) {
                FashionStore.showToast("Review submitted successfully", 'success');
                
                // Update reviews section
                const reviewsContainer = document.querySelector('.reviews-list');
                if (reviewsContainer && data.review) {
                    const reviewHTML = FashionStore.createReviewHTML(data.review);
                    reviewsContainer.insertAdjacentHTML('afterbegin', reviewHTML);
                }
                
                // Clear form
                const ratingInput = document.querySelector('#reviewRating');
                const commentInput = document.querySelector('#reviewComment');
                if (ratingInput) ratingInput.value = '5';
                if (commentInput) commentInput.value = '';
            } else {
                FashionStore.showToast(data.message || "Failed to submit review", 'error');
            }
        })
        .catch(err => {
            console.error("Error submitting review:", err);
            FashionStore.showToast("Failed to submit review", 'error');
        })
        .finally(() => {
            if (submitButton) {
                FashionStore.hideLoading(submitButton);
            }
        });
    },

    createReviewHTML: function(review) {
        return `
            <div class="review-card">
                <div class="review-header">
                    <div class="review-author">${review.userName}</div>
                    <div class="review-rating">
                        ${'★'.repeat(review.rating)}${'☆'.repeat(5 - review.rating)}
                    </div>
                </div>
                <div class="review-comment">${review.comment}</div>
                <div class="review-date">${new Date(review.createdAt).toLocaleDateString()}</div>
            </div>
        `;
    },

    // Mini Cart UI Update
    updateMiniCartUI: function(data) {
        const itemsContainer = document.getElementById('mini-cart-items');
        const totalPriceEl = document.getElementById('mini-cart-total-price');
        const badgeEl = document.getElementById('nav-cart-badge');

        if (!data.cartItems || data.cartItems.length === 0) {
            itemsContainer.innerHTML = '<div class="mini-cart-empty">Your cart is empty.</div>';
        } else {
            let html = '';
            data.cartItems.forEach(item => {
                html += `
                    <div class="mini-cart-item">
                        <img src="${item.imageUrl || ''}" alt="${item.productName}" onerror="this.style.display='none'">
                        <div class="mini-cart-item-details">
                            <h4>${item.productName || 'Product'}</h4>
                            <p>Size: ${item.sizeLabel || 'M'}</p>
                            <p>Qty: ${item.quantity}</p>
                            <p class="mini-cart-price">₹${item.price.toFixed(2)}</p>
                        </div>
                    </div>
                `;
            });
            itemsContainer.innerHTML = html;
        }

        if (totalPriceEl) {
            totalPriceEl.innerText = '₹' + (data.cartTotal ? data.cartTotal.toFixed(2) : '0.00');
        }
        if (badgeEl) {
            badgeEl.innerText = data.cartCount || 0;
        }
    },

    // Fetch Cart Data
    fetchCart: function() {
        return fetch(`${contextPath}/cart`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: new URLSearchParams({ action: 'get' })
        })
        .then(res => {
            if (!res.ok) {
                if(res.redirected) window.location.href = res.url;
                throw new Error("Not logged in or error");
            }
            return res.json();
        })
        .then(data => {
            FashionStore.updateMiniCartUI(data);
        })
        .catch(err => console.error("Error fetching cart:", err));
    },

    // Quick View Modal
    openQuickView: function(productId) {
        const modal = document.getElementById('quickViewModal');
        const modalContent = document.getElementById('modalContent');
        
        if (!modal || !modalContent) return;
        
        // Show loading state
        modalContent.innerHTML = '<div class="skeleton skeleton-image"></div><div class="skeleton skeleton-text"></div><div class="skeleton skeleton-text-short"></div><div class="skeleton skeleton-price"></div>';
        modal.classList.add('active');
        modal.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
        
        // Fetch product details
        fetch(`${contextPath}/product?id=${productId}`)
            .then(response => response.text())
            .then(html => {
                // Parse the HTML to extract product details
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, 'text/html');
                
                const productName = doc.querySelector('.product-title')?.textContent || '';
                const productPrice = doc.querySelector('.price')?.textContent || '';
                const productDescription = doc.querySelector('.description')?.textContent || '';
                const productImage = doc.querySelector('.main-image img')?.src || '';
                const sizes = Array.from(doc.querySelectorAll('.size-btn')).map(btn => btn.textContent.trim());
                
                // Build modal content
                modalContent.innerHTML = `
                    <div>
                        <img src="${productImage}" alt="${productName}" class="modal-image">
                    </div>
                    <div class="modal-details">
                        <h2 class="modal-title">${productName}</h2>
                        <div class="modal-price">${productPrice}</div>
                        <p class="modal-description">${productDescription}</p>
                        <div class="modal-sizes">
                            ${sizes.map(size => `<button class="modal-size-btn" onclick="this.classList.toggle('selected')">${size}</button>`).join('')}
                        </div>
                        <div class="modal-actions">
                            <button class="btn-primary" onclick="FashionStore.addToCart(${productId})" style="flex: 1;">Add to Cart</button>
                            <button class="btn-secondary" onclick="FashionStore.closeQuickView(); window.location.href='${contextPath}/product?id=${productId}'">View Details</button>
                        </div>
                    </div>
                `;
            })
            .catch(error => {
                console.error('Error loading product details:', error);
                modalContent.innerHTML = '<p style="text-align: center; color: var(--color-secondary);">Failed to load product details. Please try again.</p>';
            });
    },
    
    closeQuickView: function() {
        const modal = document.getElementById('quickViewModal');
        if (modal) {
            modal.classList.remove('active');
            modal.setAttribute('aria-hidden', 'true');
            document.body.style.overflow = '';
        }
    },

    // Coupon Application
    applyCoupon: function() {
        const couponInput = document.getElementById('couponCode');
        const couponMessage = document.getElementById('couponMessage');
        const discountRow = document.getElementById('discountRow');
        const discountSpan = document.getElementById('discount');
        const totalSpan = document.getElementById('summary-total');
        
        if (!couponInput || !couponInput.value.trim()) {
            if (couponMessage) {
                couponMessage.textContent = 'Please enter a coupon code';
                couponMessage.className = 'coupon-message error';
            }
            return;
        }
        
        const couponCode = couponInput.value.trim().toUpperCase();
        
        // Get current cart total
        const subtotalElement = document.getElementById('summary-subtotal');
        const currentSubtotal = subtotalElement ? parseFloat(subtotalElement.textContent.replace(/[₹,]/g, '')) : 0;
        
        // Show loading state
        if (couponMessage) {
            couponMessage.innerHTML = '<span class="loading-spinner"></span> Validating...';
            couponMessage.className = 'coupon-message';
        }
        
        fetch(`${contextPath}/cart`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: new URLSearchParams({
                action: 'applyCoupon',
                couponCode: couponCode,
                cartTotal: currentSubtotal
            })
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                if (couponMessage) {
                    couponMessage.textContent = `Coupon applied! You saved ₹${data.discount.toFixed(2)}`;
                    couponMessage.className = 'coupon-message success';
                }
                
                // Update discount display
                if (discountRow && discountSpan) {
                    discountRow.style.display = 'flex';
                    discountSpan.textContent = `-₹${data.discount.toFixed(2)}`;
                }
                
                // Update total
                if (totalSpan) {
                    totalSpan.textContent = data.total.toFixed(2);
                }
                
                // Store applied coupon
                window.appliedCoupon = couponCode;
                window.appliedDiscount = data.discount;
                
                FashionStore.showToast('Coupon applied successfully!', 'success');
            } else {
                if (couponMessage) {
                    couponMessage.textContent = data.message || 'Invalid coupon code';
                    couponMessage.className = 'coupon-message error';
                }
                
                // Hide discount row if coupon failed
                if (discountRow) {
                    discountRow.style.display = 'none';
                }
                
                FashionStore.showToast(data.message || 'Invalid coupon code', 'error');
            }
        })
        .catch(err => {
            console.error('Error applying coupon:', err);
            if (couponMessage) {
                couponMessage.textContent = 'Failed to apply coupon. Please try again.';
                couponMessage.className = 'coupon-message error';
            }
            FashionStore.showToast('Failed to apply coupon', 'error');
        });
    },

    // Shipping Estimation
    estimateShipping: function() {
        const shippingSpan = document.getElementById('shipping');
        if (!shippingSpan) return;
        
        const cartItems = document.querySelectorAll('.cart-card');
        if (cartItems.length === 0) {
            shippingSpan.textContent = 'FREE';
            return;
        }
        
        // Simple shipping logic: free for orders above ₹999, otherwise ₹49
        const subtotalElement = document.getElementById('summary-subtotal');
        const subtotal = subtotalElement ? parseFloat(subtotalElement.textContent.replace(/[₹,]/g, '')) : 0;
        
        if (subtotal >= 999) {
            shippingSpan.textContent = 'FREE';
            shippingSpan.className = 'free-tag';
        } else {
            shippingSpan.textContent = '₹49';
            shippingSpan.className = '';
        }
    },

    // Checkout Step Navigation
    goToCheckoutStep: function(stepNumber) {
        const steps = document.querySelectorAll('.progress-step');
        const sections = document.querySelectorAll('.checkout-step-section');
        
        // Update progress indicator
        steps.forEach((step, index) => {
            const stepNum = index + 1;
            step.classList.remove('active', 'completed');
            if (stepNum < stepNumber) {
                step.classList.add('completed');
            } else if (stepNum === stepNumber) {
                step.classList.add('active');
            }
        });
        
        // Show/hide sections
        sections.forEach((section, index) => {
            const sectionNum = index + 1;
            if (sectionNum === stepNumber) {
                section.style.display = 'block';
                section.style.animation = 'fadeIn 0.3s ease-out';
            } else {
                section.style.display = 'none';
            }
        });
    },

    // Address Validation
    validateAddress: function(form) {
        const fullName = form.querySelector('[name="fullName"]');
        const address = form.querySelector('[name="address"]');
        const city = form.querySelector('[name="city"]');
        const state = form.querySelector('[name="state"]');
        const zip = form.querySelector('[name="zip"]');
        const phone = form.querySelector('[name="phone"]');
        
        const errors = [];
        
        if (!fullName || !fullName.value.trim()) {
            errors.push('Full name is required');
        }
        
        if (!address || !address.value.trim()) {
            errors.push('Address is required');
        }
        
        if (!city || !city.value.trim()) {
            errors.push('City is required');
        }
        
        if (!state || !state.value.trim()) {
            errors.push('State is required');
        }
        
        if (!zip || !zip.value.trim() || !/^\d{6}$/.test(zip.value.trim())) {
            errors.push('Valid 6-digit ZIP code is required');
        }
        
        if (!phone || !phone.value.trim() || !/^[6-9]\d{9}$/.test(phone.value.trim())) {
            errors.push('Valid 10-digit phone number is required');
        }
        
        return errors;
    },

    // Payment Selection Handler
    selectPaymentMethod: function(method) {
        const paymentCards = document.querySelectorAll('.payment-card');
        paymentCards.forEach(card => {
            const input = card.querySelector('input[type="radio"]');
            if (input && input.value === method) {
                input.checked = true;
                card.classList.add('selected');
            } else {
                card.classList.remove('selected');
            }
        });
    },

    // Validate address and proceed to payment step
    validateAndProceedToPayment: function() {
        const form = document.getElementById('checkoutForm');
        if (!form) return;
        
        const errors = FashionStore.validateAddress(form);
        
        if (errors.length > 0) {
            FashionStore.showToast(errors[0], 'error');
            return;
        }
        
        FashionStore.goToCheckoutStep(2);
    },

    // Review order before final submission
    reviewOrder: function() {
        const form = document.getElementById('checkoutForm');
        if (!form) return;
        
        const paymentMethod = form.querySelector('input[name="paymentMethod"]:checked');
        if (!paymentMethod) {
            FashionStore.showToast('Please select a payment method', 'error');
            return;
        }
        
        FashionStore.goToCheckoutStep(3);
    },

    // Save for Later functionality
    saveForLater: function(cartItemId) {
        const cartItem = document.getElementById(`cart-item-${cartItemId}`);
        if (!cartItem) return;
        
        FashionStore.showLoading(cartItem, 'Saving...');
        
        const contextPath = document.querySelector('.cart-page')?.getAttribute('data-context-path') || window.contextPath || '';
        
        fetch(`${contextPath}/cart`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: new URLSearchParams({
                action: 'saveForLater',
                cartItemId: cartItemId
            })
        })
        .then(res => res.json())
        .then(data => {
            if (data.success || data.status === 'success') {
                FashionStore.showToast('Item saved for later', 'success');
                // Remove item from cart with animation
                cartItem.style.opacity = '0';
                cartItem.style.transform = 'translateX(-20px)';
                setTimeout(() => {
                    cartItem.remove();
                    FashionStore.fetchCart(); // Refresh cart
                }, 300);
            } else {
                FashionStore.showToast('Failed to save item', 'error');
            }
        })
        .catch(err => {
            console.error('Error saving item:', err);
            FashionStore.showToast('Failed to save item', 'error');
        })
        .finally(() => {
            FashionStore.hideLoading(cartItem);
        });
    },

    // Show loading state on an element
    showLoading: function(element, message) {
        if (!element) return;
        element.style.opacity = '0.5';
        element.style.pointerEvents = 'none';
    },

    // Hide loading state on an element
    hideLoading: function(element) {
        if (!element) return;
        element.style.opacity = '1';
        element.style.pointerEvents = 'auto';
    },

    // Fetch cart (for refresh after save for later)
    fetchCart: function() {
        const contextPath = document.querySelector('.cart-page')?.getAttribute('data-context-path') || window.contextPath || '';
        fetch(`${contextPath}/cart`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: new URLSearchParams({ action: 'get' })
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                FashionStore.updateMiniCartUI(data);
            }
        })
        .catch(err => console.error('Error fetching cart:', err));
    }
};

// Mini Cart Drawer
function toggleMiniCart(event) {
    if (event) event.preventDefault();
    const overlay = document.getElementById('mini-cart-overlay');
    const drawer = document.getElementById('mini-cart-drawer');
    
    if (overlay?.classList.contains('active')) {
        overlay.classList.remove('active');
        drawer.classList.remove('active');
    } else {
        overlay.classList.add('active');
        drawer.classList.add('active');
        FashionStore.fetchCart();
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    const badge = document.getElementById('nav-cart-badge');
    if (badge && parseInt(badge.innerText, 10) > 0) {
        FashionStore.fetchCart();
    }
});

// Backward compatibility
window.showToast = FashionStore.showToast;
window.addToCart = FashionStore.addToCart;
window.toggleWishlist = FashionStore.toggleWishlist;
