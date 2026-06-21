/**
 * Product Reviews Module
 * Handles product review submission with state management
 */
const ProductReviews = (function() {
    
    function submitReview(event, productId) {
        event.preventDefault();
        
        const rating = document.getElementById('reviewRating');
        const comment = document.getElementById('reviewComment');
        const submitBtn = document.getElementById('submitReviewBtn');
        
        if (!rating || !comment) {
            console.warn('Review form elements not found');
            return;
        }
        
        // Show loading state
        if (submitBtn) {
            submitBtn.classList.add('loading');
            if (typeof StateManager !== 'undefined') {
                StateManager.setButtonLoading('submitReviewBtn', true);
            }
        }
        
        fetch(window.contextPath + '/review', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: new URLSearchParams({
                productId: productId,
                rating: rating.value,
                comment: comment.value
            })
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                FashionStore.showToast(data.message || 'Review submitted successfully', 'success');
                setTimeout(() => window.location.reload(), 1500);
            } else {
                FashionStore.showToast(data.message || 'Failed to submit review', 'error');
            }
        })
        .catch(err => {
            console.error('Error submitting review:', err);
            FashionStore.showToast('Failed to submit review. Please try again.', 'error');
        })
        .finally(() => {
            if (submitBtn) {
                submitBtn.classList.remove('loading');
                if (typeof StateManager !== 'undefined') {
                    StateManager.setButtonLoading('submitReviewBtn', false);
                }
            }
        });
    }
    
    function init() {
        // Register event handlers with centralized event delegation
        if (typeof EventDelegation !== 'undefined') {
            EventDelegation.on('submit', '#reviewForm', function(event, target) {
                const productId = target.dataset.productId;
                if (productId) {
                    submitReview(event, productId);
                }
            });
        }
        
        // Check if reviews list is empty and show empty state
        const reviewsList = document.getElementById('reviews-list');
        if (reviewsList && !reviewsList.children.length) {
            if (typeof StateManager !== 'undefined') {
                StateManager.showEmpty('reviews-list', {
                    emptyTitle: 'No reviews yet',
                    emptyMessage: 'Be the first to review this product',
                    actionText: 'Write a Review',
                    onAction: function() {
                        const reviewForm = document.getElementById('reviewForm');
                        if (reviewForm) {
                            reviewForm.scrollIntoView({ behavior: 'smooth' });
                            document.getElementById('reviewRating')?.focus();
                        }
                    }
                });
            }
        }
    }
    
    return {
        init: init,
        submitReview: submitReview
    };
})();

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('productReviews', ProductReviews.init, 30);
}
