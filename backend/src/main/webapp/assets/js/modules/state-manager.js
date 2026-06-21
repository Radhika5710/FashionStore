/**
 * State Manager Module
 * Provides reusable UI state management for loading, success, empty, and error states
 */
const StateManager = (function() {
    
    // State constants
    const STATE = {
        LOADING: 'loading',
        SUCCESS: 'success',
        ERROR: 'error',
        EMPTY: 'empty',
        IDLE: 'idle'
    };
    
    // Store callbacks for event delegation
    const callbacks = new Map();
    
    // Default configurations
    const DEFAULT_CONFIG = {
        loadingText: 'Loading...',
        errorTitle: 'Something went wrong',
        errorMessage: 'Please try again later.',
        emptyTitle: 'No results found',
        emptyMessage: 'There are no items to display.',
        showRetry: true,
        retryText: 'Retry'
    };
    
    /**
     * Show loading state
     * @param {string} containerId - Container element ID
     * @param {object} config - Configuration options
     */
    function showLoading(containerId, config = {}) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const settings = { ...DEFAULT_CONFIG, ...config };
        
        container.innerHTML = `
            <div class="loading-state">
                <div class="loading-state__spinner"></div>
                <p class="loading-state__text">${settings.loadingText}</p>
            </div>
        `;
        
        container.dataset.state = STATE.LOADING;
    }
    
    /**
     * Show success state
     * @param {string} containerId - Container element ID
     * @param {object} config - Configuration options
     */
    function showSuccess(containerId, config = {}) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const settings = { ...DEFAULT_CONFIG, ...config };
        
        container.innerHTML = `
            <div class="success-state">
                <svg class="success-state__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <h3 class="success-state__title">${settings.title || 'Success!'}</h3>
                <p class="success-state__message">${settings.message || 'Operation completed successfully.'}</p>
            </div>
        `;
        
        container.dataset.state = STATE.SUCCESS;
    }
    
    /**
     * Show error state
     * @param {string} containerId - Container element ID
     * @param {object} config - Configuration options
     */
    function showError(containerId, config = {}) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const settings = { ...DEFAULT_CONFIG, ...config };
        
        let retryButton = '';
        if (settings.showRetry && settings.onRetry) {
            const callbackId = 'retry-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
            callbacks.set(callbackId, settings.onRetry);
            retryButton = `
                <button class="fs-btn fs-btn--primary error-state__retry" data-callback-id="${callbackId}">
                    ${settings.retryText}
                </button>
            `;
        }
        
        container.innerHTML = `
            <div class="error-state">
                <svg class="error-state__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <h3 class="error-state__title">${settings.errorTitle}</h3>
                <p class="error-state__message">${settings.errorMessage}</p>
                ${retryButton}
            </div>
        `;
        
        container.dataset.state = STATE.ERROR;
    }
    
    /**
     * Show empty state
     * @param {string} containerId - Container element ID
     * @param {object} config - Configuration options
     */
    function showEmpty(containerId, config = {}) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const settings = { ...DEFAULT_CONFIG, ...config };
        
        let actionButton = '';
        if (settings.actionText && settings.onAction) {
            const callbackId = 'action-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
            callbacks.set(callbackId, settings.onAction);
            actionButton = `
                <button class="fs-btn fs-btn--primary empty-state__action" data-callback-id="${callbackId}">
                    ${settings.actionText}
                </button>
            `;
        }
        
        container.innerHTML = `
            <div class="empty-state">
                <svg class="empty-state__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
                </svg>
                <h3 class="empty-state__title">${settings.emptyTitle}</h3>
                <p class="empty-state__message">${settings.emptyMessage}</p>
                ${actionButton}
            </div>
        `;
        
        container.dataset.state = STATE.EMPTY;
    }
    
    /**
     * Restore original content
     * @param {string} containerId - Container element ID
     * @param {string} content - Original content to restore
     */
    function restoreContent(containerId, content) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        container.innerHTML = content;
        container.dataset.state = STATE.IDLE;
    }
    
    /**
     * Set button loading state
     * @param {string} buttonId - Button element ID
     * @param {boolean} isLoading - Loading state
     * @param {string} originalText - Original button text
     */
    function setButtonLoading(buttonId, isLoading, originalText) {
        const button = document.getElementById(buttonId);
        if (!button) return;
        
        if (isLoading) {
            button.classList.add('btn--loading');
            button.disabled = true;
            if (originalText) {
                button.dataset.originalText = originalText;
            }
        } else {
            button.classList.remove('btn--loading');
            button.disabled = false;
        }
    }
    
    /**
     * Show inline loading indicator
     * @param {string} containerId - Container element ID
     * @param {boolean} show - Show or hide
     * @param {string} text - Loading text
     */
    function showInlineLoading(containerId, show, text = 'Loading...') {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        if (show) {
            container.innerHTML = `
                <div class="loading-inline">
                    <div class="loading-inline__spinner"></div>
                    <span class="loading-inline__text">${text}</span>
                </div>
            `;
        } else {
            container.innerHTML = '';
        }
    }
    
    /**
     * Show loading overlay
     * @param {boolean} show - Show or hide
     * @param {string} text - Loading text
     */
    function showOverlay(show, text = 'Loading...') {
        let overlay = document.getElementById('global-loading-overlay');
        
        if (show) {
            if (!overlay) {
                overlay = document.createElement('div');
                overlay.id = 'global-loading-overlay';
                overlay.className = 'loading-overlay';
                document.body.appendChild(overlay);
            }
            
            // Check if already showing to avoid duplication
            if (overlay.style.display === 'flex') {
                return;
            }
            
            overlay.innerHTML = `
                <div class="loading-overlay__spinner"></div>
                <p>${text}</p>
            `;
            overlay.style.display = 'flex';
            document.body.style.overflow = 'hidden';
        } else {
            if (overlay) {
                overlay.style.display = 'none';
                document.body.style.overflow = '';
            }
        }
    }
    
    /**
     * Async wrapper with automatic state management
     * @param {function} asyncFn - Async function to execute
     * @param {object} options - Options object
     */
    async function withState(asyncFn, options = {}) {
        const {
            containerId,
            buttonId,
            loadingText,
            onSuccess,
            onError,
            onEmpty,
            showOverlay: useOverlay = false
        } = options;
        
        try {
            // Show loading state
            if (containerId) {
                showLoading(containerId, { loadingText });
            }
            if (buttonId) {
                setButtonLoading(buttonId, true);
            }
            if (useOverlay) {
                showOverlay(true, loadingText);
            }
            
            // Execute async function
            const result = await asyncFn();
            
            // Handle result
            if (onSuccess) {
                onSuccess(result);
            }
            
            return result;
        } catch (error) {
            console.error('State manager error:', error);
            
            // Show error state
            if (containerId) {
                showError(containerId, {
                    errorMessage: error.message || DEFAULT_CONFIG.errorMessage,
                    onRetry: () => withState(asyncFn, options)
                });
            }
            
            if (onError) {
                onError(error);
            }
            
            throw error;
        } finally {
            // Cleanup loading states
            if (buttonId) {
                setButtonLoading(buttonId, false);
            }
            if (useOverlay) {
                showOverlay(false);
            }
        }
    }
    
    /**
     * Initialize event delegation for state manager buttons
     */
    function init() {
        if (typeof EventDelegation !== 'undefined') {
            EventDelegation.on('click', '.error-state__retry', function(event, target) {
                const callbackId = target.dataset.callbackId;
                if (callbackId && callbacks.has(callbackId)) {
                    event.preventDefault();
                    const callback = callbacks.get(callbackId);
                    callback();
                    callbacks.delete(callbackId); // Clean up after execution
                }
            });

            EventDelegation.on('click', '.empty-state__action', function(event, target) {
                const callbackId = target.dataset.callbackId;
                if (callbackId && callbacks.has(callbackId)) {
                    event.preventDefault();
                    const callback = callbacks.get(callbackId);
                    callback();
                    callbacks.delete(callbackId); // Clean up after execution
                }
            });
        }
    }
    
    return {
        STATE,
        showLoading,
        showSuccess,
        showError,
        showEmpty,
        restoreContent,
        setButtonLoading,
        showInlineLoading,
        showOverlay,
        withState,
        init
    };
})();

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('stateManager', StateManager.init, 20);
}
