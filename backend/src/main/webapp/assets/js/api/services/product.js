/**
 * FashionStore - Product API Service
 * Centralized product operations with standardized error handling
 */

import api from '../client.js';
import { handleApiError } from '../error.js';

/**
 * Product API Service
 */
const ProductAPI = {
    /**
     * Get product details
     */
    async getProduct(productId) {
        try {
            const response = await api.get(`/product?id=${productId}`);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Get product quick view
     * Note: Quick view uses the same endpoint as product details
     */
    async getQuickView(productId) {
        try {
            const response = await api.get(`/product?id=${productId}`);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Search products
     */
    async searchProducts(query, filters = {}) {
        try {
            const params = new URLSearchParams({ q: query, ...filters });
            const response = await api.get(`/search/suggestions?${params}`);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Submit product review
     */
    async submitReview(productId, rating, comment) {
        try {
            const response = await api.post('/review', {
                productId,
                rating: parseInt(rating),
                comment
            });
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    }
};

// Export for use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ProductAPI;
}

// Make available globally
window.ProductAPI = ProductAPI;
