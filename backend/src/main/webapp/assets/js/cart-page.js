/**
 * FashionStore - Cart Page JavaScript
 * Handles cart page interactions: quantity updates, item removal, coupon application
 */

document.addEventListener('DOMContentLoaded', function() {
    const contextPath = window.contextPath || '';
    
    // ── Quantity Update Handlers ───────────────────────────────────────────────
    
    const cartItemsContainer = document.querySelector('.fs-cart-items');
    if (cartItemsContainer) {
        cartItemsContainer.addEventListener('click', function(e) {
            const decreaseBtn = e.target.closest('.fs-qty-stepper__btn[data-action="decrease"]');
            const increaseBtn = e.target.closest('.fs-qty-stepper__btn[data-action="increase"]');
            const removeBtn = e.target.closest('.fs-cart-item__action--danger');
            const saveBtn = e.target.closest('.fs-cart-item__action:not(.fs-cart-item__action--danger)');
            
            if (decreaseBtn) {
                e.preventDefault();
                const cartItemId = decreaseBtn.getAttribute('data-id');
                const currentQty = parseInt(decreaseBtn.getAttribute('data-qty') || 1);
                if (cartItemId && currentQty > 1) {
                    CartManager.updateCartQty(cartItemId, currentQty - 1);
                }
            } else if (increaseBtn) {
                e.preventDefault();
                const cartItemId = increaseBtn.getAttribute('data-id');
                const currentQty = parseInt(increaseBtn.getAttribute('data-qty') || 1);
                if (cartItemId) {
                    CartManager.updateCartQty(cartItemId, currentQty + 1);
                }
            } else if (removeBtn) {
                e.preventDefault();
                const cartItemId = removeBtn.getAttribute('data-id');
                if (cartItemId) {
                    CartManager.removeCartItem(cartItemId);
                }
            } else if (saveBtn) {
                e.preventDefault();
                const cartItemId = saveBtn.getAttribute('data-id');
                if (cartItemId) {
                    saveBtn.disabled = true;
                    saveBtn.textContent = 'Saving...';
                    
                    FashionStoreAPI.post('/cart', {
                        action: 'saveForLater',
                        cartItemId: cartItemId
                    })
                    .then(data => {
                        if (data && (data.success || data.status === 'success')) {
                            FashionStore.showToast('Saved for later', 'success');
                            // Reload page to show updated cart
                            setTimeout(() => {
                                window.location.reload();
                            }, 500);
                        } else {
                            FashionStore.showToast(data.message || 'Failed to save for later', 'error');
                            saveBtn.disabled = false;
                            saveBtn.textContent = 'Save for later';
                        }
                    })
                    .catch(err => {
                        console.error('Save for later error:', err);
                        FashionStore.showToast('Failed to save for later. Please try again.', 'error');
                        saveBtn.disabled = false;
                        saveBtn.textContent = 'Save for later';
                    });
                }
            }
        });
        
        // Handle quantity input changes
        cartItemsContainer.addEventListener('change', function(e) {
            const qtyInput = e.target.closest('.fs-qty-stepper__input');
            if (qtyInput) {
                const cartItemId = qtyInput.getAttribute('data-id');
                const newQty = parseInt(qtyInput.value);
                if (cartItemId && newQty >= 1 && newQty <= 10) {
                    CartManager.updateCartQty(cartItemId, newQty);
                } else {
                    qtyInput.value = qtyInput.getAttribute('data-qty');
                }
            }
        });
    }
    
    // ── Coupon Application ───────────────────────────────────────────────────────
    
    const couponCodeInput = document.getElementById('couponCode');
    const applyCouponBtn = document.querySelector('.fs-cart-summary__coupon button');
    const couponMessage = document.getElementById('couponMessage');
    
    if (applyCouponBtn && couponCodeInput) {
        applyCouponBtn.addEventListener('click', function() {
            const code = couponCodeInput.value.trim();
            if (!code) {
                if (couponMessage) {
                    couponMessage.textContent = 'Please enter a coupon code';
                    couponMessage.style.color = '#ff4d4d';
                }
                return;
            }
            
            applyCouponBtn.disabled = true;
            applyCouponBtn.textContent = 'Applying...';
            
            FashionStoreAPI.post('/cart', {
                action: 'applyCoupon',
                couponCode: code
            })
            .then(data => {
                if (data && (data.success || data.status === 'success')) {
                    if (couponMessage) {
                        couponMessage.textContent = data.message || 'Coupon applied successfully';
                        couponMessage.style.color = '#22c55e';
                    }
                    // Reload page to show updated totals
                    setTimeout(() => {
                        window.location.reload();
                    }, 1000);
                } else {
                    if (couponMessage) {
                        couponMessage.textContent = data.message || 'Invalid coupon code';
                        couponMessage.style.color = '#ff4d4d';
                    }
                }
            })
            .catch(err => {
                console.error('Coupon application error:', err);
                if (couponMessage) {
                    couponMessage.textContent = 'Failed to apply coupon. Please try again.';
                    couponMessage.style.color = '#ff4d4d';
                }
            })
            .finally(() => {
                applyCouponBtn.disabled = false;
                applyCouponBtn.textContent = 'Apply';
            });
        });
    }
    
    // ── Free Shipping Progress Bar ─────────────────────────────────────────────
    
    const progressBar = document.querySelector('.fs-free-ship-bar__bar');
    if (progressBar) {
        const progress = progressBar.getAttribute('data-progress');
        if (progress) {
            progressBar.style.width = Math.min(progress, 100) + '%';
        }
    }
    
    // ── Make applyCoupon available globally for backward compatibility ───────────
    
    window.FashionStore = window.FashionStore || {};
    window.FashionStore.applyCoupon = function() {
        if (applyCouponBtn) {
            applyCouponBtn.click();
        }
    };
});
