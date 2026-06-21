/**
 * FashionStore - Wishlist API Service
 * Centralized wishlist operations with standardized error handling
 */

import api from '../client.js';
import { handleApiError } from '../error.js';

/**
 * Wishlist API Service
 */
const WishlistAPI = {
    /**
     * Check if product is in wishlist
     */
    async checkWishlistStatus(productId) {
        try {
            const response = await api.get(`/api/wishlist?action=check&productId=${productId}`);
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Toggle product in wishlist
     */
    async toggleWishlist(productId) {
        try {
            const response = await api.post('/api/wishlist?action=toggle', {
                productId
            });
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Get wishlist items
     */
    async getWishlist() {
        try {
            const response = await api.get('/wishlist');
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Remove item from wishlist
     */
    async removeFromWishlist(productId) {
        try {
            const response = await api.post('/api/wishlist?action=remove', {
                productId
            });
            return response.data;
        } catch (error) {
            throw handleApiError(error);
        }
    },

    /**
     * Get wishlist count
     */
    async getWishlistCount() {
        try {
            const response = await api.get('/api/wishlist');
            const items = response.data.items || [];
            return items.length;
        } catch (error) {
            throw handleApiError(error);
        }
    }
};

// Export for use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = WishlistAPI;
}

// Make available globally
window.WishlistAPI = WishlistAPI;
