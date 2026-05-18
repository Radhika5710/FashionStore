/**
 * Centralized API Layer Export
 * Single entry point for all API-related functionality
 */

// Core client (includes error handling utilities)
export { default as apiClient, handleAuthError, handleNetworkError, handleServerError, APIError, createErrorFromAxios } from './client.js';
export {
  isValidEmail,
  isValidPassword,
  validateRequired,
  isValidURL,
  isValidPhone,
  isNumeric,
  isPositiveNumber,
  isInteger,
  isValidDate,
  validateResponse,
  sanitizeInput,
  validateRequestData,
} from './validators.js';

// Response unwrapping utilities
const unwrapList = (key) => (res) => {
  try {
    if (!res || !res.data) {
      console.warn(`unwrapList: Invalid response for key "${key}"`);
      return [];
    }
    
    const body = res.data;
    
    // Try nested data structure first
    if (body?.data?.[key] && Array.isArray(body.data[key])) {
      return body.data[key];
    }
    
    // Try direct data array
    if (Array.isArray(body?.data)) {
      return body.data;
    }
    
    // Try root-level array
    if (Array.isArray(body)) {
      return body;
    }
    
    // Fallback to empty array
    console.warn(`unwrapList: Could not find array for key "${key}"`);
    return [];
  } catch (err) {
    console.error(`unwrapList error for key "${key}":`, err);
    return [];
  }
};

const unwrapOne = (key) => (res) => {
  try {
    if (!res || !res.data) {
      console.warn(`unwrapOne: Invalid response for key "${key}"`);
      return null;
    }
    
    const body = res.data;
    
    // Try nested data structure first
    if (body?.data?.[key]) {
      return body.data[key];
    }
    
    // Try direct data object
    if (body?.data && typeof body.data === 'object') {
      return body.data;
    }
    
    // Try root-level object
    if (typeof body === 'object' && !Array.isArray(body)) {
      return body;
    }
    
    // Fallback to null
    console.warn(`unwrapOne: Could not find object for key "${key}"`);
    return null;
  } catch (err) {
    console.error(`unwrapOne error for key "${key}":`, err);
    return null;
  }
};

const unwrap = (res) => {
  try {
    if (!res || !res.data) {
      console.warn('unwrap: Invalid response');
      return null;
    }
    return res.data?.data ?? res.data;
  } catch (err) {
    console.error('unwrap error:', err);
    return null;
  }
};

export { unwrapList, unwrapOne, unwrap };
