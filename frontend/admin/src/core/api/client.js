/**
 * Admin API Client - JWT Authentication Only
 * 
 * HYBRID ARCHITECTURE:
 * ====================
 * - This client: Admin React Frontend (/api/admin/*)
 * - MVC client: Customer JSP Frontend (webapp/assets/js/api/client.js)
 * 
 * CRITICAL: This client is for ADMIN APIs ONLY (/api/admin/*).
 * Uses JWT tokens stored in HTTP-only cookies.
 * DO NOT use this for customer/MVC APIs.
 * 
 * AUTHENTICATION METHOD:
 * =====================
 * - JWT token-based via Authorization header
 * - Access token: 15 minutes (from HTTP-only cookie)
 * - Refresh token: 7 days (from HTTP-only cookie)
 * - Token injection: Bearer scheme in Authorization header
 * - No session cookies
 * - No CSRF tokens
 * 
 * FEATURES:
 * =========
 * - JWT token injection in Authorization header
 * - Automatic token refresh on 401
 * - Request queuing during token refresh
 * - Centralized error handling
 * - Request/response logging in dev mode
 * - Network error handling
 * - Permission error handling (403)
 * 
 * IMPORTANT SEPARATION:
 * ====================
 * ✓ Uses ONLY JWT tokens
 * ✓ Uses ONLY Authorization header
 * ✓ Uses ONLY /api/admin/* endpoints
 * ✓ NO JSESSIONID cookie
 * ✓ NO CSRF tokens
 * ✓ NO session attributes
 * 
 * Customer API client is separate:
 * - webapp/assets/js/api/client.js
 * - Uses CSRF tokens
 * - Uses JSESSIONID cookie
 * - Uses session-based auth
 * - Only for customer endpoints
 * 
 * REQUEST FLOW:
 * =============
 * 1. Request interceptor:
 *    - Logs request in dev mode
 *    - JWT token automatically included via cookies
 * 
 * 2. Request execution:
 *    - HTTP-only cookies automatically included
 *    - Authorization header ready for token injection
 *    - Timeout: 15 seconds
 * 
 * 3. Response interceptor:
 *    - Logs successful response in dev mode
 *    - Handles 401 with token refresh
 *    - Handles 403 (permission denied)
 *    - Handles 5xx (server errors)
 *    - Handles network errors
 * 
 * TOKEN REFRESH FLOW:
 * ===================
 * 1. Request fails with 401
 * 2. Check if already refreshing (isRefreshing flag)
 * 3. If refreshing: queue request and wait
 * 4. If not refreshing: start refresh
 * 5. POST /api/admin/refresh to get new token
 * 6. Process queued requests
 * 7. Retry original request
 * 8. If refresh fails: redirect to /admin/login
 * 
 * ERROR HANDLING:
 * ===============
 * - 401: Token expired → attempt refresh → retry request
 * - 403: Permission denied → log warning
 * - 5xx: Server error → log error
 * - Network: Connection error → log error
 * 
 * USAGE EXAMPLES:
 * ===============
 * // GET request
 * adminApiClient.get('/products').then(response => {
 *   console.log(response.data);
 * });
 * 
 * // POST request
 * adminApiClient.post('/products', { name: 'Product', price: 100 }).then(response => {
 *   console.log(response.data);
 * });
 * 
 * // PUT request
 * adminApiClient.put('/products/1', { name: 'Updated' }).then(response => {
 *   console.log(response.data);
 * });
 * 
 * // DELETE request
 * adminApiClient.delete('/products/1').then(response => {
 *   console.log(response.data);
 * });
 */

import axios from 'axios';

// In production/Docker, the nginx proxy handles /api routing,
// so we use a relative base URL. In dev (Vite), the proxy forwards to localhost.
// VITE_API_BASE env var can override for custom setups.
const API_BASE = import.meta.env.VITE_API_BASE || '/api/admin';

// Validate API base URL
if (!API_BASE || typeof API_BASE !== 'string') {
  console.error('Invalid API_BASE configuration:', API_BASE);
}

/**
 * Custom API Error class
 */
export class APIError extends Error {
  constructor(status, message, data = null) {
    super(message);
    this.name = 'APIError';
    this.status = status;
    this.data = data;
  }
}

/**
 * Handle authentication errors (401)
 * Redirects to login and dispatches logout event
 */
function handleAuthError(err) {
  const pathname = window.location?.pathname || '';
  
  // Avoid bounce loops on the login screen itself
  if (!pathname.endsWith('/login') && !pathname.startsWith('/login?')) {
    // Dispatch event for AuthProvider to handle router-aware navigation
    try {
      window.dispatchEvent(new CustomEvent('auth:logout'));
    } catch (e) {
      console.error('Failed to dispatch logout event:', e);
    }
  }
  
  return Promise.reject(err);
}

/**
 * Handle network errors
 * No response received from server
 */
function handleNetworkError(err) {
  console.error('Network error:', err.message);
  return Promise.reject(new Error('Network error - unable to reach server'));
}

/**
 * Handle server errors (500+)
 * Server-side errors
 */
function handleServerError(err) {
  const status = err.response?.status;
  const message = err.response?.data?.message || err.message || 'Unknown error';
  console.error('Server error:', status, message);
  return Promise.reject(new Error(`Server error: ${message}`));
}

/**
 * Create error from axios error
 */
function createErrorFromAxios(err) {
  if (!err) {
    return new Error('Unknown error');
  }
  
  const status = err.response?.status;
  const message = err.response?.data?.message || err.message || 'Unknown error';
  const data = err.response?.data;
  
  return new APIError(status, message, data);
}

/**
 * Create admin API client
 * Uses JWT tokens for authentication
 * Single source of truth for all admin API communication
 */
const adminApiClient = axios.create({
  baseURL: API_BASE,
  withCredentials: true, // include HTTP-only JWT cookies
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

/**
 * Retry configuration
 */
const RETRY_CONFIG = {
  retries: 3,
  retryDelay: 1000,
  retryCondition: (error) => {
    // Retry on network errors and 5xx errors
    return !error.response && (error.code === 'ECONNABORTED' || error.code === 'ENETDOWN') ||
           error.response && error.response.status >= 500;
  }
};

/**
 * Retry logic with exponential backoff
 */
async function retryRequest(config, retryCount = 0) {
  try {
    return await adminApiClient(config);
  } catch (error) {
    if (retryCount < RETRY_CONFIG.retries && 
        RETRY_CONFIG.retryCondition(error)) {
      const delay = RETRY_CONFIG.retryDelay * Math.pow(2, retryCount);
      await new Promise(resolve => setTimeout(resolve, delay));
      return retryRequest(config, retryCount + 1);
    }
    throw error;
  }
}

/**
 * JWT Request Interceptor
 * 
 * RESPONSIBILITIES:
 * =================
 * - Log request in development mode
 * - JWT token automatically included via HTTP-only cookies
 * - No manual token injection needed (withCredentials: true)
 * 
 * IMPORTANT: Do NOT manually inject tokens here
 * - HTTP-only cookies prevent JavaScript access
 * - Cookies automatically sent with withCredentials: true
 * - Authorization header is set by backend if needed
 * 
 * SECURITY:
 * =========
 * - HTTP-only cookies prevent XSS token theft
 * - Secure flag ensures HTTPS only
 * - SameSite prevents CSRF
 * - Token not accessible to JavaScript
 */
adminApiClient.interceptors.request.use(
  (config) => {
    // Log request in development
    if (import.meta.env.DEV) {
      console.debug(`[Admin API] ${config.method?.toUpperCase()} ${config.url}`);
    }
    
    // HTTP-only cookies are automatically included via withCredentials: true
    // No manual token injection needed
    
    return config;
  },
  (error) => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

/**
 * JWT Response Interceptor
 * 
 * RESPONSIBILITIES:
 * =================
 * - Handle 401 Unauthorized (token expired)
 * - Attempt token refresh on 401
 * - Queue requests during token refresh
 * - Handle 403 Forbidden (permission denied)
 * - Handle 5xx Server Errors
 * - Handle network errors
 * 
 * TOKEN REFRESH STRATEGY:
 * ======================
 * - Use isRefreshing flag to prevent multiple refresh requests
 * - Queue failed requests while refreshing
 * - Retry queued requests after successful refresh
 * - Redirect to login if refresh fails
 * 
 * IMPORTANT: This prevents duplicate refresh requests
 * - Multiple 401 errors trigger only one refresh
 * - Other requests wait for refresh to complete
 * - All requests retry after refresh
 */
let isRefreshing = false;
let failedQueue = [];

/**
 * Process queued requests after token refresh
 * 
 * @param {Error|null} error - Error if refresh failed
 * @param {string|null} token - New token if refresh succeeded
 */
const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  
  isRefreshing = false;
  failedQueue = [];
};

adminApiClient.interceptors.response.use(
  (response) => {
    // Log successful response in development
    if (import.meta.env.DEV) {
      console.debug(`[Admin API] Response ${response.status} from ${response.config?.url}`);
    }
    
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message || 'Unknown error';
    
    // Log error details
    console.error(`[Admin API Error] Status: ${status}, Message: ${message}`);
    
    // Handle 401 Unauthorized - attempt token refresh
    if (status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue request while refreshing
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => adminApiClient(originalRequest))
          .catch(err => Promise.reject(err));
      }
      
      originalRequest._retry = true;
      isRefreshing = true;
      
      try {
        // Attempt to refresh token
        const refreshResponse = await adminApiClient.post('/refresh');
        
        if (refreshResponse.data?.success) {
          // Token refreshed successfully
          processQueue(null);
          return adminApiClient(originalRequest);
        } else {
          // Refresh failed - logout user
          processQueue(new Error('Token refresh failed'), null);
          handleAuthError(error);
          window.location.href = '/admin/login';
          return Promise.reject(error);
        }
      } catch (refreshError) {
        // Refresh error - logout user
        processQueue(refreshError, null);
        handleAuthError(refreshError);
        window.location.href = '/admin/login';
        return Promise.reject(refreshError);
      }
    }
    
    // Handle 5xx Server Errors with retry
    if (status >= 500 && !originalRequest._retry) {
      if (originalRequest._retryCount === undefined) {
        originalRequest._retryCount = 0;
      }
      
      if (originalRequest._retryCount < RETRY_CONFIG.retries) {
        originalRequest._retryCount++;
        const delay = RETRY_CONFIG.retryDelay * Math.pow(2, originalRequest._retryCount - 1);
        await new Promise(resolve => setTimeout(resolve, delay));
        return adminApiClient(originalRequest);
      }
      
      handleServerError(error);
    }
    
    // Handle network errors with retry
    if (!error.response && !originalRequest._retry) {
      if (originalRequest._retryCount === undefined) {
        originalRequest._retryCount = 0;
      }
      
      if (originalRequest._retryCount < RETRY_CONFIG.retries) {
        originalRequest._retryCount++;
        const delay = RETRY_CONFIG.retryDelay * Math.pow(2, originalRequest._retryCount - 1);
        await new Promise(resolve => setTimeout(resolve, delay));
        return adminApiClient(originalRequest);
      }
      
      handleNetworkError(error);
    }
    
    // Handle 403 Forbidden
    if (status === 403) {
      console.warn('Access forbidden - insufficient permissions');
    }
    
    return Promise.reject(error);
  }
);

export default adminApiClient;
export { handleAuthError, handleNetworkError, handleServerError, createErrorFromAxios };
