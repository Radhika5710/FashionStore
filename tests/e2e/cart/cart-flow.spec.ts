import { test, expect } from '@playwright/test';

test.describe('Cart Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  });

  test('should add product to cart', async ({ page }) => {
    // Navigate to products page
    await page.goto('/products');
    
    // Click add to cart on first product
    await page.click('[data-testid="add-to-cart-button"]:first');
    
    // Verify cart indicator updates
    await expect(page.locator('[data-testid="cart-count"]')).toHaveText('1');
    
    // Open cart drawer
    await page.click('[data-testid="cart-icon"]');
    
    // Verify product in cart
    await expect(page.locator('[data-testid="cart-item"]')).toHaveCount(1);
  });

  test('should remove product from cart', async ({ page }) => {
    // Add product to cart
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    
    // Open cart
    await page.click('[data-testid="cart-icon"]');
    
    // Remove product
    await page.click('[data-testid="remove-item-button"]');
    
    // Verify cart is empty
    await expect(page.locator('[data-testid="cart-item"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="cart-count"]')).toHaveText('0');
  });

  test('should update product quantity in cart', async ({ page }) => {
    // Add product to cart
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    
    // Open cart
    await page.click('[data-testid="cart-icon"]');
    
    // Increase quantity
    await page.click('[data-testid="quantity-increase"]');
    await expect(page.locator('[data-testid="cart-quantity"]')).toHaveText('2');
    
    // Decrease quantity
    await page.click('[data-testid="quantity-decrease"]');
    await expect(page.locator('[data-testid="cart-quantity"]')).toHaveText('1');
  });

  test('should calculate cart total correctly', async ({ page }) => {
    // Add two products to cart
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:nth-of-type(1)');
    await page.click('[data-testid="add-to-cart-button"]:nth-of-type(2)');
    
    // Open cart
    await page.click('[data-testid="cart-icon"]');
    
    // Verify total calculation
    const totalText = await page.locator('[data-testid="cart-total"]').textContent();
    expect(totalText).toBeTruthy();
    expect(totalText).toMatch(/\$\d+\.\d{2}/);
  });

  test('should persist cart across page navigation', async ({ page }) => {
    // Add product to cart
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    
    // Navigate to different page
    await page.goto('/about');
    
    // Verify cart still has product
    await expect(page.locator('[data-testid="cart-count"]')).toHaveText('1');
    
    // Open cart
    await page.click('[data-testid="cart-icon"]');
    await expect(page.locator('[data-testid="cart-item"]')).toHaveCount(1);
  });

  test('should handle out of stock products', async ({ page }) => {
    // Navigate to out of stock product
    await page.goto('/products/out-of-stock-product');
    
    // Add to cart button should be disabled
    await expect(page.locator('[data-testid="add-to-cart-button"]')).toBeDisabled();
  });

  test('should clear cart after logout', async ({ page }) => {
    // Add product to cart
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    
    // Logout
    await page.click('[data-testid="logout-button"]');
    
    // Login again
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    
    // Verify cart is empty
    await expect(page.locator('[data-testid="cart-count"]')).toHaveText('0');
  });
});
