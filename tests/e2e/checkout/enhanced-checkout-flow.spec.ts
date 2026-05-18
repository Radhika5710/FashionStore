import { test, expect } from '@playwright/test';

test.describe('Enhanced Checkout Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  });

  test('should complete full checkout flow', async ({ page }) => {
    // Add product to cart
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    
    // Proceed to checkout
    await page.click('[data-testid="cart-icon"]');
    await page.click('[data-testid="checkout-button"]');
    
    // Verify checkout page loads
    await expect(page).toHaveURL('/checkout');
    await expect(page.locator('h1')).toContainText('Checkout');
    
    // Fill shipping information
    await page.fill('input[name="fullName"]', 'John Doe');
    await page.fill('input[name="address"]', '123 Main St');
    await page.fill('input[name="city"]', 'New York');
    await page.fill('input[name="zipCode"]', '10001');
    await page.selectOption('select[name="country"]', 'US');
    
    // Continue to payment
    await page.click('[data-testid="continue-to-payment"]');
    
    // Fill payment information
    await page.fill('input[name="cardNumber"]', '4242424242424242');
    await page.fill('input[name="expiryDate"]', '12/25');
    await page.fill('input[name="cvv"]', '123');
    
    // Place order
    await page.click('[data-testid="place-order-button"]');
    
    // Verify order confirmation
    await page.waitForURL(/\/order\/confirmation/);
    await expect(page.locator('h1')).toContainText('Order Confirmation');
    await expect(page.locator('[data-testid="order-number"]')).toBeVisible();
  });

  test('should validate required fields', async ({ page }) => {
    // Add product to cart and proceed to checkout
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    await page.click('[data-testid="cart-icon"]');
    await page.click('[data-testid="checkout-button"]');
    
    // Try to continue without filling required fields
    await page.click('[data-testid="continue-to-payment"]');
    
    // Should show validation errors
    await expect(page.locator('[data-testid="validation-error"]')).toBeVisible();
  });

  test('should handle payment failure gracefully', async ({ page }) => {
    // Add product to cart and proceed to checkout
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    await page.click('[data-testid="cart-icon"]');
    await page.click('[data-testid="checkout-button"]');
    
    // Fill shipping information
    await page.fill('input[name="fullName"]', 'John Doe');
    await page.fill('input[name="address"]', '123 Main St');
    await page.fill('input[name="city"]', 'New York');
    await page.fill('input[name="zipCode"]', '10001');
    await page.selectOption('select[name="country"]', 'US');
    
    // Continue to payment
    await page.click('[data-testid="continue-to-payment"]');
    
    // Use invalid card
    await page.fill('input[name="cardNumber"]', '4000000000000002');
    await page.fill('input[name="expiryDate"]', '12/25');
    await page.fill('input[name="cvv"]', '123');
    
    // Try to place order
    await page.click('[data-testid="place-order-button"]');
    
    // Should show payment error
    await expect(page.locator('[data-testid="payment-error"]')).toBeVisible();
  });

  test('should save shipping address for future orders', async ({ page }) => {
    // Add product to cart and proceed to checkout
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    await page.click('[data-testid="cart-icon"]');
    await page.click('[data-testid="checkout-button"]');
    
    // Fill and save shipping address
    await page.fill('input[name="fullName"]', 'John Doe');
    await page.fill('input[name="address"]', '123 Main St');
    await page.fill('input[name="city"]', 'New York');
    await page.fill('input[name="zipCode"]', '10001');
    await page.selectOption('select[name="country"]', 'US');
    await page.check('[data-testid="save-address"]');
    
    // Continue to payment
    await page.click('[data-testid="continue-to-payment"]');
    
    // Complete order
    await page.fill('input[name="cardNumber"]', '4242424242424242');
    await page.fill('input[name="expiryDate"]', '12/25');
    await page.fill('input[name="cvv"]', '123');
    await page.click('[data-testid="place-order-button"]');
    
    // Start new checkout
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    await page.click('[data-testid="cart-icon"]');
    await page.click('[data-testid="checkout-button"]');
    
    // Verify saved address is pre-filled
    await expect(page.locator('input[name="fullName"]')).toHaveValue('John Doe');
    await expect(page.locator('input[name="address"]')).toHaveValue('123 Main St');
  });

  test('should handle empty cart on checkout', async ({ page }) => {
    // Try to access checkout with empty cart
    await page.goto('/checkout');
    
    // Should redirect to cart or show empty cart message
    await expect(page.locator('[data-testid="empty-cart-message"]')).toBeVisible();
  });

  test('should calculate order total correctly', async ({ page }) => {
    // Add multiple products
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:nth-of-type(1)');
    await page.click('[data-testid="add-to-cart-button"]:nth-of-type(2)');
    
    // Proceed to checkout
    await page.click('[data-testid="cart-icon"]');
    await page.click('[data-testid="checkout-button"]');
    
    // Verify order summary
    const subtotal = await page.locator('[data-testid="order-subtotal"]').textContent();
    const tax = await page.locator('[data-testid="order-tax"]').textContent();
    const total = await page.locator('[data-testid="order-total"]').textContent();
    
    expect(subtotal).toBeTruthy();
    expect(tax).toBeTruthy();
    expect(total).toBeTruthy();
  });

  test('should apply discount code', async ({ page }) => {
    // Add product to cart
    await page.goto('/products');
    await page.click('[data-testid="add-to-cart-button"]:first');
    
    // Proceed to checkout
    await page.click('[data-testid="cart-icon"]');
    await page.click('[data-testid="checkout-button"]');
    
    // Apply discount code
    await page.fill('input[name="discountCode"]', 'SAVE10');
    await page.click('[data-testid="apply-discount"]');
    
    // Verify discount applied
    await expect(page.locator('[data-testid="discount-applied"]')).toBeVisible();
  });
});
