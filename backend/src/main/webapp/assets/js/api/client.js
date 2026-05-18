/**
 * FashionStore - MVC/JSP API Client
 * Session-based authentication ONLY
 * 
 * HYBRID ARCHITECTURE:
 * ====================
 * - This client: Customer MVC Frontend (JSP pages)
 * - Admin client: React Admin Frontend (/api/admin/*)
 * 
 * CRITICAL: This client is for CUSTOMER MVC FRONTEND ONLY.
 * Uses HttpSession (JSESSIONID cookie) for authentication.
 * DO NOT add JWT logic here. JWT is for admin APIs only (/api/admin/*).
 * 
 * AUTHENTICATION METHOD:
 * =====================
 * - Session-based via HttpSession
 * - JSESSIONID cookie (HTTP-only, secure)
 * - CSRF token injection for POST requests
 * - No JWT tokens
 * - No Authorization header
 * 
 * FEATURES:
 * =========
 * - CSRF token injection from meta tags
 * - Session-based authentication via JSESSIONID cookie
 * - Retry logic with exponential backoff
 * - Centralized error handling
 * - Request/response logging in debug mode
 * - Session expiry detection and redirect
 * - CSRF error handling
 * - Network error handling
 * 
 * IMPORTANT SEPARATION:
 * ====================
 * ✓ Uses ONLY CSRF tokens
 * ✓ Uses ONLY JSESSIONID cookie
 * ✓ NO JWT token injection
 * ✓ NO Authorization header
 * ✓ NO token refresh logic
 * ✓ NO admin API endpoints
 * 
 * Admin API client is separate:
 * - frontend/admin/src/core/api/client.js
 * - Uses JWT tokens
 * - Uses Authorization header
 * - Uses token refresh logic
 * - Only for /api/admin/* endpoints
 * 
 * REQUEST FLOW:
 * =============
 * 1. Request interceptor:
 *    - Injects CSRF token from meta tag
 *    - Adds timestamp to prevent caching (GET requests)
 *    - Logs request in debug mode
 * 
 * 2. Request execution:
 *    - JSESSIONID cookie automatically included
 *    - Retry on network errors and 5xx
 *    - Exponential backoff (1s, 2s, 4s)
 * 
 * 3. Response interceptor:
 *    - Normalizes response structure
 *    - Detects session expiry (401)
 *    - Detects CSRF errors (403)
 *    - Handles server errors (5xx)
 *    - Handles network errors
 * 
 * ERROR HANDLING:
 * ===============
 * - 401: Session expired → redirect to /login
 * - 403 (CSRF): Security token expired → reload page
 * - 5xx: Server error → show error message
 * - Network: Connection error → show error message
 * 
 * USAGE EXAMPLES:
 * ===============
 * // GET request
 * FashionStoreAPI.get('/api/products').then(response => {
 *   console.log(response.data);
 * });
 * 
 * // POST request with CSRF token (auto-injected)
 * FashionStoreAPI.post('/api/cart', { productId: 1, quantity: 2 }).then(response => {
 *   console.log(response.data);
 * });
 * 
 * // PUT request
 * FashionStoreAPI.put('/api/profile', { name: 'John' }).then(response => {
 *   console.log(response.data);
 * });
 * 
 * // DELETE request
 * FashionStoreAPI.delete('/api/cart/1').then(response => {
 *   console.log(response.data);
 * });
 */

// API Configuration
const API_CONFIG = {
    baseURL: window.contextPath || '',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest'
    },
    retryConfig: {
        retries: 3,
        retryDelay: 1000
    }
};

/**
 * Get CSRF token from meta tag or window variable
 */
function getCsrfToken() {
    const metaToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
    return metaToken || window.csrfToken || '';
}

/**
 * Build URL with query parameters
 */
function buildUrl(url, params) {
    if (!params) return url;
    const queryString = new URLSearchParams(params).toString();
    return queryString ? `${url}?${queryString}` : url;
}

/**
 * Normalize response structure
 */
function normalizeResponse(data) {
    // If response already has standard structure, return as-is
    if (data && typeof data === 'object' && ('success' in data || 'status' in data)) {
        return data;
    }

    // Wrap plain data in standard structure
    return {
        success: true,
        status: 'success',
        data: data
    };
}

/**
 * Create standardized API error
 */
function createApiError(message, statusCode, originalError) {
    const error = new Error(message);
    error.name = 'ApiError';
    error.statusCode = statusCode;
    error.originalError = originalError;
    return error;
}

/**
 * Handle session expiry
 */
function handleSessionExpiry() {
    // Clear session storage
    sessionStorage.clear();

    // Show notification
    if (typeof FashionStore !== 'undefined' && FashionStore.showToast) {
        FashionStore.showToast('Session expired. Redirecting to login...', 'warning');
    }

    // Redirect to login after delay
    setTimeout(() => {
        window.location.href = window.contextPath + '/login';
    }, 2000);
}

/**
 * Handle CSRF errors
 */
function handleCsrfError() {
    if (typeof FashionStore !== 'undefined' && FashionStore.showToast) {
        FashionStore.showToast('Security token expired. Refreshing page...', 'warning');
    }

    setTimeout(() => {
        window.location.reload();
    }, 2000);
}

/**
 * Retry logic with exponential backoff
 */
async function retryRequest(fn, retryCount = 0) {
    try {
        return await fn();
    } catch (error) {
        if (retryCount < API_CONFIG.retryConfig.retries) {
            const delay = API_CONFIG.retryConfig.retryDelay * Math.pow(2, retryCount);
            await new Promise(resolve => setTimeout(resolve, delay));
            return retryRequest(fn, retryCount + 1);
        }
        throw error;
    }
}

/**
 * Core fetch wrapper
 */
async function fetchAPI(url, options = {}) {
    const isGet = !options.method || options.method.toUpperCase() === 'GET';
    
    const config = {
        method: options.method || 'GET',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            ...options.headers
        },
        ...options
    };
    
    // Add CSRF token for non-GET requests
    if (!isGet) {
        config.headers['X-CSRF-Token'] = getCsrfToken();
    }
    
    // Add body if provided
    if (options.data) {
        config.body = JSON.stringify(options.data);
    }
    
    // Add timestamp to prevent caching for GET requests
    let finalUrl = API_CONFIG.baseURL + url;
    if (isGet) {
        finalUrl += (finalUrl.includes('?') ? '&' : '?') + '_t=' + Date.now();
        if (options.params) {
            finalUrl += '&' + new URLSearchParams(options.params).toString();
        }
    }
    
    // Log request in development
    if (window.DEBUG_MODE) {
        console.log(`[API Request] ${config.method.toUpperCase()} ${finalUrl}`, config.body);
    }
    
    try {
        const response = await fetch(finalUrl, config);
        
        // Log response in development
        if (window.DEBUG_MODE) {
            console.log(`[API Response] ${finalUrl}`, response.status);
        }
        
        // Handle session expiry
        if (response.status === 401) {
            handleSessionExpiry();
            throw createApiError('Session expired. Please login again.', 401);
        }
        
        // Handle CSRF errors
        if (response.status === 403) {
            handleCsrfError();
            throw createApiError('Security token expired. Please refresh the page.', 403);
        }
        
        // Handle server errors
        if (response.status >= 500) {
            throw createApiError('Server error. Please try again later.', response.status);
        }
        
        // Parse JSON response
        const data = await response.json();
        
        return normalizeResponse(data);
    } catch (error) {
        // Handle network errors
        if (!error.statusCode) {
            throw createApiError('Network error. Please check your connection.', 0, error);
        }
        throw error;
    }
}

/**
 * Enhanced API client with retry support
 */
const api = {
    get: (url, config) => retryRequest(() => fetchAPI(url, { ...config, method: 'GET' })),
    post: (url, data, config) => retryRequest(() => fetchAPI(url, { ...config, method: 'POST', data })),
    put: (url, data, config) => retryRequest(() => fetchAPI(url, { ...config, method: 'PUT', data })),
    delete: (url, config) => retryRequest(() => fetchAPI(url, { ...config, method: 'DELETE' })),
    patch: (url, data, config) => retryRequest(() => fetchAPI(url, { ...config, method: 'PATCH', data })),
    
    // Configuration
    setBaseURL: (baseURL) => {
        API_CONFIG.baseURL = baseURL;
    },
    setHeader: (key, value) => {
        API_CONFIG.headers[key] = value;
    },
    removeHeader: (key) => {
        delete API_CONFIG.headers[key];
    }
};

// Make available globally for backward compatibility
window.FashionStoreAPI = api;
