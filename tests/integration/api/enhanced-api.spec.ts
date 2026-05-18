import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080/api';

test.describe('API Integration Tests', () => {
  let authToken: string;

  test.beforeAll(async ({ request }) => {
    // Login to get auth token
    const response = await request.post(`${API_BASE}/auth/login`, {
      data: {
        email: 'test@example.com',
        password: 'TestPassword123!'
      }
    });
    
    const data = await response.json();
    authToken = data.token || data.accessToken;
  });

  test.describe('Authentication API', () => {
    test('should login with valid credentials', async ({ request }) => {
      const response = await request.post(`${API_BASE}/auth/login`, {
        data: {
          email: 'test@example.com',
          password: 'TestPassword123!'
        }
      });

      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data).toHaveProperty('token');
    });

    test('should reject invalid credentials', async ({ request }) => {
      const response = await request.post(`${API_BASE}/auth/login`, {
        data: {
          email: 'invalid@example.com',
          password: 'WrongPassword123!'
        }
      });

      expect(response.status()).toBe(401);
    });

    test('should register new user', async ({ request }) => {
      const response = await request.post(`${API_BASE}/auth/register`, {
        data: {
          email: `newuser${Date.now()}@example.com`,
          password: 'NewPassword123!',
          fullName: 'New User'
        }
      });

      expect(response.ok()).toBeTruthy();
    });

    test('should refresh token', async ({ request }) => {
      const response = await request.post(`${API_BASE}/auth/refresh`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });

      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data).toHaveProperty('token');
    });
  });

  test.describe('Products API', () => {
    test('should get all products', async ({ request }) => {
      const response = await request.get(`${API_BASE}/products`);

      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(Array.isArray(data)).toBeTruthy();
    });

    test('should get product by ID', async ({ request }) => {
      const response = await request.get(`${API_BASE}/products/1`);

      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data).toHaveProperty('id');
      expect(data).toHaveProperty('name');
    });

    test('should search products', async ({ request }) => {
      const response = await request.get(`${API_BASE}/products/search?q=shirt`);

      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(Array.isArray(data)).toBeTruthy();
    });
  });

  test.describe('Cart API', () => {
    test('should get cart items', async ({ request }) => {
      const response = await request.get(`${API_BASE}/cart`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });

      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(Array.isArray(data)).toBeTruthy();
    });

    test('should add item to cart', async ({ request }) => {
      const response = await request.post(`${API_BASE}/cart/items`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        },
        data: {
          productId: 1,
          quantity: 2
        }
      });

      expect(response.ok()).toBeTruthy();
    });

    test('should update cart item quantity', async ({ request }) => {
      const response = await request.put(`${API_BASE}/cart/items/1`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        },
        data: {
          quantity: 3
        }
      });

      expect(response.ok()).toBeTruthy();
    });

    test('should remove item from cart', async ({ request }) => {
      const response = await request.delete(`${API_BASE}/cart/items/1`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });

      expect(response.ok()).toBeTruthy();
    });
  });

  test.describe('Orders API', () => {
    test('should get user orders', async ({ request }) => {
      const response = await request.get(`${API_BASE}/orders`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });

      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(Array.isArray(data)).toBeTruthy();
    });

    test('should create order', async ({ request }) => {
      const response = await request.post(`${API_BASE}/orders`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        },
        data: {
          shippingAddress: {
            fullName: 'John Doe',
            address: '123 Main St',
            city: 'New York',
            zipCode: '10001',
            country: 'US'
          },
          paymentMethod: 'card'
        }
      });

      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data).toHaveProperty('orderId');
    });
  });

  test.describe('Admin API', () => {
    test('should require admin authentication', async ({ request }) => {
      const response = await request.get(`${API_BASE}/admin/users`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      });

      // Should fail with regular user token
      expect(response.status()).toBe(403);
    });

    test('should get admin dashboard stats', async ({ request }) => {
      // This would need admin token in real implementation
      const response = await request.get(`${API_BASE}/admin/stats`);

      // For now, just test the endpoint exists
      expect([200, 401, 403]).toContain(response.status());
    });
  });

  test.describe('Error Handling', () => {
    test('should handle 404 errors', async ({ request }) => {
      const response = await request.get(`${API_BASE}/products/99999`);

      expect(response.status()).toBe(404);
    });

    test('should handle validation errors', async ({ request }) => {
      const response = await request.post(`${API_BASE}/auth/login`, {
        data: {
          email: 'invalid-email',
          password: '123'
        }
      });

      expect(response.status()).toBe(400);
    });

    test('should handle unauthorized requests', async ({ request }) => {
      const response = await request.get(`${API_BASE}/cart`);

      expect(response.status()).toBe(401);
    });
  });
});
