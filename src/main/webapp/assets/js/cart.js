/**
 * FashionStore - Cart AJAX Logic
 */
document.addEventListener('DOMContentLoaded', function() {
    const contextPath = document.querySelector('.cart-page')?.getAttribute('data-context-path') || '';
    
    // Quantity Buttons (+ / -)
    const qtyButtons = document.querySelectorAll('.ajax-qty-btn');
    qtyButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const action = this.getAttribute('data-action');
            const cartItemId = this.getAttribute('data-id');
            const currentQty = parseInt(this.getAttribute('data-qty'));
            
            updateCart(action, cartItemId, currentQty);
        });
    });

    // Remove Buttons
    const removeButtons = document.querySelectorAll('.ajax-remove-btn');
    removeButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const cartItemId = this.getAttribute('data-id');
            updateCart('remove', cartItemId, 0);
        });
    });

    // Save for Later Buttons
    const saveLaterButtons = document.querySelectorAll('.ajax-save-later-btn');
    saveLaterButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const cartItemId = this.getAttribute('data-id');
            updateCart('saveForLater', cartItemId, 0);
        });
    });

    /**
     * Helper to send AJAX request to CartController
     */
    function updateCart(action, cartItemId, currentQty) {
        const params = new URLSearchParams();
        params.append('action', action);
        params.append('cartItemId', cartItemId);
        params.append('currentQty', currentQty);

        // Visual feedback: disable buttons or show loading
        setLoadingState(true);

        fetch(`${contextPath}/cart`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: params.toString()
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok: ' + response.status);
            }
            if (response.redirected) {
                window.location.href = response.url;
                return;
            }
            return response.json();
        })
        .then(data => {
            if (data.success || data.status === 'success') {
                updateDOM(data);
                showToast('Cart updated', 'success');
            } else {
                showToast('Failed to update cart. Please try again.', 'error');
            }
        })
        .catch(error => {
            console.error('Cart update error:', error);
            showToast('An error occurred. Please refresh the page.', 'error');
            setTimeout(() => window.location.reload(), 2000);
        })
        .finally(() => {
            setLoadingState(false);
        });
    }

    /**
     * Update the DOM elements with new values from server
     */
    function updateDOM(data) {
        if (data.removed || data.newQuantity <= 0) {
            // Remove the item card from DOM
            const itemCard = document.getElementById(`cart-item-${data.cartItemId}`);
            if (itemCard) {
                itemCard.style.opacity = '0';
                setTimeout(() => {
                    itemCard.remove();
                    checkEmptyCart();
                }, 300);
            }
        } else {
            // Update quantity display
            const qtyVal = document.getElementById(`qty-val-${data.cartItemId}`);
            if (qtyVal) qtyVal.innerText = data.newQuantity;

            // Update item subtotal
            const itemTotal = document.getElementById(`item-total-${data.cartItemId}`);
            if (itemTotal) itemTotal.innerText = data.itemTotal.toFixed(2);

            // Update data-qty attribute on buttons
            const buttons = document.querySelectorAll(`.ajax-qty-btn[data-id="${data.cartItemId}"]`);
            buttons.forEach(b => b.setAttribute('data-qty', data.newQuantity));
        }

        // Update global totals
        const summarySubtotal = document.getElementById('summary-subtotal');
        if (summarySubtotal) summarySubtotal.innerText = data.cartTotal.toFixed(2);

        const summaryTotal = document.getElementById('summary-total');
        if (summaryTotal) summaryTotal.innerText = data.cartTotal.toFixed(2);

        const summaryCount = document.getElementById('summary-count');
        if (summaryCount) summaryCount.innerText = data.cartCount;

        const countHeader = document.getElementById('cart-count-header');
        if (countHeader) countHeader.innerText = data.cartCount;

        // Sync navbar badge
        const navBadge = document.getElementById('nav-cart-badge');
        if (navBadge) navBadge.innerText = data.cartCount;
    }

    function setLoadingState(isLoading) {
        const btns = document.querySelectorAll('.ajax-qty-btn, .ajax-remove-btn, .ajax-save-later-btn');
        btns.forEach(b => {
            b.disabled = isLoading;
            b.style.opacity = isLoading ? '0.5' : '1';
        });
    }

    function checkEmptyCart() {
        const itemsList = document.getElementById('cart-items-list');
        if (itemsList && itemsList.children.length === 0) {
            // Reload to show empty cart state from JSP
            window.location.reload();
        }
    }

    // Delegate to global FashionStore.showToast defined in main.js (loaded via navbar.jsp)
    function showToast(message, type) {
        if (typeof window.FashionStore !== 'undefined' && typeof window.FashionStore.showToast === 'function') {
            window.FashionStore.showToast(message, type || 'success');
        } else if (typeof window.showToast === 'function') {
            window.showToast(message, type || 'success');
        }
    }
});
