import { test, expect } from '@playwright/test';

test.describe('Protected Routes', () => {
  test('should redirect unauthenticated users to login', async ({ page }) => {
    // Try to access protected route without authentication
    await page.goto('/dashboard');
    
    // Should redirect to login
    await page.waitForURL('/login');
    await expect(page).toHaveURL('/login');
    
    // Should show login form
    await expect(page.locator('input[name="email"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
  });

  test('should allow authenticated users to access protected routes', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    
    // Wait for successful login
    await page.waitForURL('/');
    
    // Access protected route
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    
    // Should load successfully
    await expect(page).toHaveURL('/dashboard');
    await expect(page.locator('h1')).toContainText('Dashboard');
  });

  test('should redirect to original destination after login', async ({ page }) => {
    // Try to access protected route
    await page.goto('/profile');
    
    // Should redirect to login with redirect parameter
    await page.waitForURL(/\/login/);
    
    // Login
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    
    // Should redirect back to original destination
    await page.waitForURL('/profile');
    await expect(page).toHaveURL('/profile');
  });

  test('should handle multiple protected route redirects', async ({ page }) => {
    // Try to access multiple protected routes
    await page.goto('/dashboard');
    await page.waitForURL('/login');
    
    // Login
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    
    // Should redirect to dashboard (last attempted protected route)
    await page.waitForURL('/dashboard');
    await expect(page).toHaveURL('/dashboard');
  });

  test('should clear redirect after successful login', async ({ page }) => {
    // Try to access protected route
    await page.goto('/orders');
    await page.waitForURL(/\/login/);
    
    // Login
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    
    // Should redirect to orders
    await page.waitForURL('/orders');
    await expect(page).toHaveURL('/orders');
    
    // Logout
    await page.click('[data-testid="logout-button"]');
    
    // Try to access different protected route
    await page.goto('/profile');
    await page.waitForURL('/login');
    
    // Login again
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    
    // Should redirect to profile (not orders)
    await page.waitForURL('/profile');
    await expect(page).toHaveURL('/profile');
  });
});
