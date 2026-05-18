/**
 * Admin API Facade
 * 
 * Re-exports the centralized admin API client from core/api/client.js
 * Provides domain-specific API helpers for admin operations
 * 
 * IMPORTANT: This is for ADMIN APIs ONLY using JWT authentication.
 * DO NOT use this for customer/MVC APIs.
 */

import adminApiClient from '../core/api/client.js';
import { unwrapList, unwrapOne, unwrap } from '../core/api/index.js';

// Re-export the centralized client for backward compatibility
const api = adminApiClient;

// Domain-specific helpers
export const AuthApi = {
  login: (email, password) => api.post('/login', { email, password }),
  logout: () => api.post('/logout'),
  me: () => api.get('/me'),
  register: (data) => api.post('/register', data),
};

export const DashboardApi = {
  fetch: () => api.get('/dashboard').then(unwrap),
};

export const OrdersApi = {
  list: (limit = 50) => api.get('/orders', { params: { limit } }).then(unwrapList('orders')),
  get: (id) => api.get(`/orders/${id}`).then(unwrapOne('order')),
  updateStatus: (id, status) => api.put(`/orders/${id}/status`, { status }).then(unwrap),
  approve: (id) => api.put(`/orders/${id}/approve`).then(unwrap),
  cancel: (id) => api.put(`/orders/${id}/cancel`).then(unwrap),
  ship: (id) => api.put(`/orders/${id}/ship`).then(unwrap),
  deliver: (id) => api.put(`/orders/${id}/deliver`).then(unwrap),
  refund: (id) => api.put(`/orders/${id}/refund`).then(unwrap),
};

export const ProductsApi = {
  list: () => api.get('/products').then(unwrapList('products')),
  get: (id) => api.get(`/products/${id}`).then(unwrapOne('product')),
  create: (data) => api.post('/products', data).then(unwrap),
  update: (id, data) => api.put(`/products/${id}`, data).then(unwrap),
  delete: (id) => api.delete(`/products/${id}`).then(unwrap),
};

export const UsersApi = {
  list: () => api.get('/users').then(unwrapList('users')),
  get: (id) => api.get(`/users/${id}`).then(unwrapOne('user')),
  update: (id, data) => api.put(`/users/${id}`, data).then(unwrap),
  delete: (id) => api.delete(`/users/${id}`).then(unwrap),
};

export const InventoryApi = {
  list: () => api.get('/inventory').then(unwrapList('products')),
  updateStock: (id, stock) => api.put(`/inventory/${id}/stock`, { stock }).then(unwrap),
  lowStock: () => api.get('/inventory/low-stock').then(unwrapList('products')),
};

export const CategoriesApi = {
  list: () => api.get('/categories').then(unwrapList('categories')),
  create: (data) => api.post('/categories', data).then(unwrap),
  update: (id, data) => api.put(`/categories/${id}`, data).then(unwrap),
  delete: (id) => api.delete(`/categories/${id}`).then(unwrap),
};

export const CouponsApi = {
  list: () => api.get('/coupons').then(unwrapList('coupons')),
  create: (data) => api.post('/coupons', data).then(unwrap),
  update: (id, data) => api.put(`/coupons/${id}`, data).then(unwrap),
  delete: (id) => api.delete(`/coupons/${id}`).then(unwrap),
};

export const AdminApi = {
  stats: () => api.get('/stats').then(unwrap),
  recentOrders: (limit = 10) => api.get('/orders/recent', { params: { limit } }).then(unwrapList('orders')),
  recentUsers: (limit = 10) => api.get('/users/recent', { params: { limit } }).then(unwrapList('users')),
};

export default api;
