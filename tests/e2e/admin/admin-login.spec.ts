ete all the jsimport { test, expect } from '@playwright/test';

test.describe('Admin Authentication', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/login');
  });

  test('should display login form', async ({ page }) => {
    await expect(page.locator('input[name="email"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('should login with valid credentials', async ({ page }) => {
    await page.fill('input[name="email"]', 'admin@fashionstore.com');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL('/admin/dashboard');
    await expect(page.locator('h1')).toContainText('Dashboard');
  });

  test('should show error with invalid credentials', async ({ page }) => {
    await page.fill('input[name="email"]', 'invalid@test.com');
    await page.fill('input[name="password"]', 'wrongpassword');
    await page.click('button[type="submit"]');

    await expect(page.locator('.error-message')).toBeVisible();
    await expect(page.locator('.error-message')).toContainText('Invalid credentials');
  });

  test('should validate email format', async ({ page }) => {
    await page.fill('input[name="email"]', 'invalid-email');
    await page.fill('input[name="password"]', 'password123');
    await page.click('button[type="submit"]');

    await expect(page.locator('.error-message')).toContainText('Invalid email format');
  });

  test('should toggle password visibility', async ({ page }) => {
    const passwordInput = page.locator('input[name="password"]');
    const toggleButton = page.locator('.password-toggle');

    await page.fill('input[name="password"]', 'password123');
    await expect(passwordInput).toHaveAttribute('type', 'password');

    await toggleButton.click();
    await expect(passwordInput).toHaveAttribute('type', 'text');

    await toggleButton.click();
    await expect(passwordInput).toHaveAttribute('type', 'password');
  });

  // REGRESSION TESTS - Admin auth protection
  test.describe('Regression - Admin Auth Protection', () => {
    test('should block non-admin users from admin dashboard', async ({ page }) => {
      // Login as regular user
      await page.goto('/login');
      await page.fill('input[name="email"]', 'user@example.com');
      await page.fill('input[name="password"]', 'user123');
      await page.click('button[type="submit"]');

      // Try to access admin dashboard
      await page.goto('/admin/dashboard');

      // Should redirect to admin login
      await expect(page).toHaveURL('/admin/login');
    });

    test('should protect admin routes without authentication', async ({ page }) => {
      // Try to access admin dashboard without login
      await page.goto('/admin/dashboard');

      // Should redirect to admin login
      await expect(page).toHaveURL('/admin/login');
    });

    test('should protect admin products route', async ({ page }) => {
      await page.goto('/admin/products');

      // Should redirect to admin login
      await expect(page).toHaveURL('/admin/login');
    });

    test('should protect admin orders route', async ({ page }) => {
      await page.goto('/admin/orders');

      // Should redirect to admin login
      await expect(page).toHaveURL('/admin/login');
    });

    test('should protect admin users route', async ({ page }) => {
      await page.goto('/admin/users');

      // Should redirect to admin login
      await expect(page).toHaveURL('/admin/login');
    });

    test('should maintain admin session across navigation', async ({ page }) => {
      // Login as admin
      await page.fill('input[name="email"]', 'admin@fashionstore.com');
      await page.fill('input[name="password"]', 'admin123');
      await page.click('button[type="submit"]');
      await expect(page).toHaveURL('/admin/dashboard');

      // Navigate to different admin pages
      await page.goto('/admin/products');
      await expect(page).toHaveURL('/admin/products');

      await page.goto('/admin/orders');
      await expect(page).toHaveURL('/admin/orders');

      await page.goto('/admin/dashboard');
      await expect(page).toHaveURL('/admin/dashboard');
    });

    test('should handle admin token expiry gracefully', async ({ page }) => {
      // Login as admin
      await page.fill('input[name="email"]', 'admin@fashionstore.com');
      await page.fill('input[name="password"]', 'admin123');
      await page.click('button[type="submit"]');
      await expect(page).toHaveURL('/admin/dashboard');

      // Simulate token expiry by clearing cookies
      await page.context().clearCookies();

      // Try to navigate to protected route
      await page.goto('/admin/products');

      // Should redirect to login
      await expect(page).toHaveURL('/admin/login');
    });

    test('should prevent role escalation attacks', async ({ page }) => {
      // Login as regular user
      await page.goto('/login');
      await page.fill('input[name="email"]', 'user@example.com');
      await page.fill('input[name="password"]', 'user123');
      await page.click('button[type="submit"]');

      // Try to directly access admin routes
      await page.goto('/admin/users');

      // Should be blocked
      await expect(page).toHaveURL('/admin/login');
    });

    test('should validate admin secret key on registration', async ({ page }) => {
      await page.goto('/admin/register');

      // Try to register without admin key
      await page.fill('input[name="fullName"]', 'Test Admin');
      await page.fill('input[name="email"]', 'newadmin@test.com');
      await page.fill('input[name="password"]', 'password123');
      await page.fill('input[name="confirmPassword"]', 'password123');
      await page.fill('input[name="adminKey"]', 'wrongkey');
      await page.click('button[type="submit"]');

      // Should show error
      await expect(page.locator('.error-message')).toContainText('Invalid admin secret key');
    });
  });
});
