/**
 * FashionStore - UI Utilities Module
 * Provides toast notifications and loading state helpers
 */

const FashionStoreUI = (function() {
    const contextPath = window.contextPath || '';
    
    /**
     * Show toast notification
     * @param {string} message - Toast message
     * @param {string} type - Toast type (success, error, warning, info)
     * @param {number} duration - Duration in milliseconds (default 3000)
     */
    function showToast(message, type = 'info', duration = 3000) {
        const container = document.getElementById('toast-container');
        if (!container) {
            console.warn('Toast container not found');
            return;
        }
        
        const toast = document.createElement('div');
        toast.className = `toast toast--${type}`;
        toast.innerHTML = `
            <span class="toast__message">${message}</span>
            <button class="toast__close" aria-label="Close">×</button>
        `;
        
        container.appendChild(toast);
        
        // Animate in
        requestAnimationFrame(() => {
            toast.classList.add('toast--visible');
        });
        
        // Close button handler
        const closeBtn = toast.querySelector('.toast__close');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                removeToast(toast);
            });
        }
        
        // Auto-dismiss
        setTimeout(() => {
            removeToast(toast);
        }, duration);
    }
    
    /**
     * Remove toast element
     * @param {HTMLElement} toast - Toast element to remove
     */
    function removeToast(toast) {
        if (!toast) return;
        
        toast.classList.remove('toast--visible');
        toast.classList.add('toast--removing');
        
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }
    
    /**
     * Show loading state on button
     * @param {HTMLElement} element - Button or element to show loading on
     * @param {string} text - Loading text
     */
    function showLoading(element, text = 'Loading...') {
        if (!element) return;
        
        const originalText = element.textContent || element.innerHTML;
        element.dataset.originalText = originalText;
        element.dataset.loading = 'true';
        element.disabled = true;
        element.textContent = text;
        element.classList.add('btn--loading');
    }
    
    /**
     * Hide loading state on button
     * @param {HTMLElement} element - Button or element to hide loading on
     */
    function hideLoading(element) {
        if (!element) return;
        
        const originalText = element.dataset.originalText;
        delete element.dataset.loading;
        element.disabled = false;
        element.classList.remove('btn--loading');
        
        if (originalText) {
            element.textContent = originalText;
        }
    }
    
    // Public API
    return {
        showToast,
        showLoading,
        hideLoading
    };
})();

// Make available globally for backward compatibility
if (typeof window.FashionStore === 'undefined') {
    window.FashionStore = {};
}
window.FashionStore.showToast = FashionStoreUI.showToast;
window.FashionStore.showLoading = FashionStoreUI.showLoading;
window.FashionStore.hideLoading = FashionStoreUI.hideLoading;

// Export for ES6 modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FashionStoreUI;
}
