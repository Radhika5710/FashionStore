/**
 * FashionStore - Product Interactions Module
 * Wishlist, reviews, quick view, and product-related interactions
 * 
 * REFACTORED FOR MVC ARCHITECTURE:
 * - No product filtering or manipulation
 * - Only handles UI interactions (wishlist, reviews, quick view)
 * - All product data from backend (ProductController)
 * - All AJAX calls to backend endpoints
 * - No client-side product calculations or filtering
 */

const FashionStoreProductInteractions = (function() {
    const contextPath = window.contextPath || '';
    
    function init() {
        setupWishlistButtons();
        setupReviewForm();
        setupQuickViewButtons();
    }
    
    function setupWishlistButtons() {
        // Initialize wishlist buttons on page load
        document.querySelectorAll('.fs-product-card__wishlist, .wishlist-btn, [data-product-id]').forEach(btn => {
            const productId = btn.dataset.productId;
            if (productId) {
                checkWishlistStatus(productId, btn);
            }
        });
    }
    
    function checkWishlistStatus(productId, button) {
        // Use the correct API endpoint: /api/wishlist with action parameter
        fetch(`${contextPath}/api/wishlist?action=check&productId=${productId}`, {
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP error! status: ${res.status}`);
            }
            return res.json();
        })
        .then(data => {
            if (data.data && data.data.isFavorite) {
                button.classList.add('active');
                button.setAttribute('aria-pressed', 'true');
            }
        })
        .catch(err => {
            // Silent fail - wishlist check is optional for UX
            // Don't spam console with errors
        });
    }
    
    function toggleWishlist(productId, button) {
        if (!productId) return;
        
        const isAdding = !button || !button.classList.contains('active');
        
        // Show loading state
        if (button) {
            button.classList.add('loading');
            if (typeof StateManager !== 'undefined') {
                StateManager.setButtonLoading('wishlist-btn-' + productId, true);
            }
        }
        
        fetch(`${contextPath}/api/wishlist?action=toggle`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: JSON.stringify({ productId })
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                if (button) {
                    button.classList.toggle('active');
                    button.setAttribute('aria-pressed', button.classList.contains('active') ? 'true' : 'false');
                }
                
                FashionStore.showToast(
                    data.inWishlist ? 'Added to wishlist' : 'Removed from wishlist',
                    'success'
                );
                
                // Update navbar wishlist count if available
                updateWishlistCount(data.wishlistCount);
            }
        })
        .catch(err => {
            console.error('Error toggling wishlist:', err);
            FashionStore.showToast('Failed to update wishlist', 'error');
        })
        .finally(() => {
            if (button) {
                button.classList.remove('loading');
                if (typeof StateManager !== 'undefined') {
                    StateManager.setButtonLoading('wishlist-btn-' + productId, false);
                }
            }
        });
    }
    
    function updateWishlistCount(count) {
        const badge = document.getElementById('nav-wishlist-badge');
        if (badge) {
            badge.textContent = count;
            badge.style.display = count > 0 ? 'block' : 'none';
        }
    }
    
    function setupReviewForm() {
        const reviewForm = document.getElementById('review-form');
        if (!reviewForm) return;
        
        reviewForm.addEventListener('submit', (e) => {
            e.preventDefault();
            
            const productId = reviewForm.dataset.productId;
            const rating = reviewForm.querySelector('input[name="rating"]:checked')?.value;
            const comment = reviewForm.querySelector('textarea[name="comment"]')?.value;
            
            if (!rating) {
                FashionStore.showToast('Please select a rating', 'error');
                return;
            }
            
            submitReview(productId, rating, comment, reviewForm);
        });
    }
    
    function submitReview(productId, rating, comment, form) {
        FashionStore.showLoading(form, 'Submitting review...');
        
        fetch(`${contextPath}/review`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: new URLSearchParams({
                productId,
                rating: parseInt(rating),
                comment
            })
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                FashionStore.showToast('Review submitted successfully', 'success');
                form.reset();
                
                // Add new review to the reviews list
                const reviewsList = document.getElementById('reviews-list');
                if (reviewsList && data.review) {
                    const reviewHtml = createReviewHTML(data.review);
                    reviewsList.insertAdjacentHTML('afterbegin', reviewHtml);
                }
            } else {
                FashionStore.showToast(data.message || 'Failed to submit review', 'error');
            }
        })
        .catch(err => {
            console.error('Error submitting review:', err);
            FashionStore.showToast('Failed to submit review', 'error');
        })
        .finally(() => {
            FashionStore.hideLoading(form);
        });
    }
    
    function createReviewHTML(review) {
        const stars = '★'.repeat(review.rating) + '☆'.repeat(5 - review.rating);
        const date = new Date(review.createdAt).toLocaleDateString();
        
        return `
            <article class="review-card">
                <div class="review-card__header">
                    <span class="review-card__author">${escapeHtml(review.userName)}</span>
                    <span class="review-card__date">${date}</span>
                </div>
                <div class="review-card__rating">${stars}</div>
                <p class="review-card__comment">${escapeHtml(review.comment)}</p>
            </article>
        `;
    }
    
    function setupQuickViewButtons() {
        document.querySelectorAll('.quick-view-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const productId = btn.dataset.productId;
                openQuickView(productId);
            });
        });

        // Setup quickview close button
        const quickviewCloseBtn = document.getElementById('quickview-close-btn');
        if (quickviewCloseBtn) {
            quickviewCloseBtn.addEventListener('click', closeQuickView);
        }
    }
    
    function openQuickView(productId) {
        // Quick view endpoint doesn't exist in backend - redirect to product page instead
        window.location.href = `${contextPath}/product?id=${productId}`;
        return;
    }
    
    function createQuickViewModal(product) {
        const modal = document.createElement('div');
        modal.className = 'quick-view-modal';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.setAttribute('aria-labelledby', 'quick-view-title');
        
        modal.innerHTML = `
            <div class="quick-view-modal__backdrop"></div>
            <div class="quick-view-modal__content">
                <button class="quick-view-modal__close" aria-label="Close">✕</button>
                <div class="quick-view-modal__grid">
                    <div class="quick-view-modal__image">
                        <img src="${product.imageUrl}" alt="${escapeHtml(product.productName)}" onerror="this.src='${contextPath}/assets/images/placeholder-product.jpg'">
                    </div>
                    <div class="quick-view-modal__details">
                        <h2 id="quick-view-title" class="quick-view-modal__title">${escapeHtml(product.productName)}</h2>
                        <p class="quick-view-modal__price">₹${product.price.toFixed(2)}</p>
                        <p class="quick-view-modal__description">${escapeHtml(product.description || '')}</p>
                        <div class="quick-view-modal__actions">
                            <a href="${contextPath}/product?id=${product.productId}" class="fs-btn fs-btn--primary">View Full Details</a>
                            <button class="fs-btn fs-btn--outline wishlist-btn" data-product-id="${product.productId}">Add to Wishlist</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        // Setup wishlist button in modal
        const wishlistBtn = modal.querySelector('.wishlist-btn');
        if (wishlistBtn) {
            wishlistBtn.addEventListener('click', () => toggleWishlist(product.productId, wishlistBtn));
            checkWishlistStatus(product.productId, wishlistBtn);
        }
        
        return modal;
    }
    
    function closeQuickView() {
        const modal = document.querySelector('.quick-view-modal');
        if (modal) {
            modal.classList.remove('active');
            setTimeout(() => {
                modal.remove();
            }, 300);
        }
        // Remove escape key listener
        document.removeEventListener('keydown', handleEscape);
    }
    
    function handleEscape(e) {
        if (e.key === 'Escape') {
            closeQuickView();
        }
    }
    
    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    // Cleanup any open modals on page unload
    function cleanup() {
        closeQuickView();
    }
    
    // Remove item from wishlist (called from wishlist.jsp)
    function removeWishlistItem(productId) {
        if (!productId) {
            console.error('removeWishlistItem: productId is required');
            return;
        }
        
        // Call toggleWishlist to remove the item
        toggleWishlist(productId, null).then(() => {
            // Remove the item from DOM after successful removal
            const itemElement = document.querySelector(`[data-product-id="${productId}"]`);
            if (itemElement && itemElement.closest('.product-card')) {
                itemElement.closest('.product-card').remove();
                
                // Check if wishlist is now empty
                const remainingItems = document.querySelectorAll('.product-card');
                if (remainingItems.length === 0) {
                    location.reload(); // Reload to show empty state
                }
            }
        }).catch(err => {
            console.error('Error removing wishlist item:', err);
        });
    }
    
    // Public API
    return {
        init,
        toggleWishlist,
        removeWishlistItem,
        submitReview,
        openQuickView,
        closeQuickView,
        cleanup
    };
})();

// Make product interactions available globally for backward compatibility
if (typeof window.FashionStore === 'undefined') {
    window.FashionStore = {};
}
window.FashionStore.productInteractions = FashionStoreProductInteractions;
window.FashionStore.toggleWishlist = FashionStoreProductInteractions.toggleWishlist;
window.FashionStore.removeWishlistItem = FashionStoreProductInteractions.removeWishlistItem;
window.FashionStore.submitReview = FashionStoreProductInteractions.submitReview;
window.FashionStore.openQuickView = FashionStoreProductInteractions.openQuickView;
window.FashionStore.closeQuickView = FashionStoreProductInteractions.closeQuickView;

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('productInteractions', FashionStoreProductInteractions.init, 15);
}

// Cleanup on page navigation to prevent memory leaks
window.addEventListener('beforeunload', () => {
    FashionStoreProductInteractions.cleanup();
});

window.addEventListener('pagehide', () => {
    FashionStoreProductInteractions.cleanup();
});

// Export for ES6 modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FashionStoreProductInteractions;
}
