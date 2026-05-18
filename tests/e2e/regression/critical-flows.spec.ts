import { test, expect } from '@playwright/test';

test.describe('Critical Flows Regression Suite', () => {
  test.describe('Authentication Flows', () => {
    test('should register new user', async ({ page }) => {
      await page.goto('/register');
      await page.fill('input[name="email"]', 'newuser@example.com');
      await page.fill('input[name="password"]', 'NewPassword123!');
      await page.fill('input[name="confirmPassword"]', 'NewPassword123!');
      await page.fill('input[name="fullName"]', 'New User');
      await page.click('button[type="submit"]');
      
      await page.waitForURL('/');
      await expect(page).toHaveURL('/');
    });

    test('should login with valid credentials', async ({ page }) => {
      await page.goto('/login');
      await page.fill('input[name="email"]', 'test@example.com');
      await page.fill('input[name="password"]', 'TestPassword123!');
      await page.click('button[type="submit"]');
      
      await page.waitForURL('/');
      await expect(page).toHaveURL('/');
    });

    test('should logout user', async ({ page }) => {
      // Login first
      await page.goto('/login');
      await page.fill('input[name="email"]', 'test@example.com');
      await page.fill('input[name="password"]', 'TestPassword123!');
      await page.click('button[type="submit"]');
      await page.waitForURL('/');
      
      // Logout
      await page.click('[data-testid="logout-button"]');
      
      // Should redirect to login
      await page.waitForURL('/login');
      await expect(page).toHaveURL('/login');
    });

    test('should handle invalid login', async ({ page }) => {
      await page.goto('/login');
      await page.fill('input[name="email"]', 'invalid@example.com');
      await page.fill('input[name="password"]', 'WrongPassword123!');
      await page.click('button[type="submit"]');
      
      // Should show error message
      await expect(page.locator('[data-testid="login-error"]')).toBeVisible();
    });
  });

  test.describe('Admin Flows', () => {
    test('should login to admin panel', async ({ page }) => {
      await page.goto('/admin/login');
      await page.fill('input[name="email"]', 'admin@example.com');
      await page.fill('input[name="password"]', 'AdminPassword123!');
      await page.click('button[type="submit"]');
      
      await page.waitForURL('/admin/dashboard');
      await expect(page).toHaveURL('/admin/dashboard');
    });

    test('should protect admin routes', async ({ page }) => {
      await page.goto('/admin/dashboard');
      
      // Should redirect to admin login
      await page.waitForURL('/admin/login');
      await expect(page).toHaveURL('/admin/login');
    });

    test('should load admin dashboard', async ({ page }) => {
      // Login as admin
      await page.goto('/admin/login');
      await page.fill('input[name="email"]', 'admin@example.com');
      await page.fill('input[name="password"]', 'AdminPassword123!');
      await page.click('button[type="submit"]');
      await page.waitForURL('/admin/dashboard');
      
      // Verify dashboard loads
      await expect(page.locator('[data-testid="admin-dashboard"]')).toBeVisible();
      await expect(page.locator('[data-testid="stats-cards"]')).toBeVisible();
    });
  });

  test.describe('Cart and Checkout Flows', () => {
    test.beforeEach(async ({ page }) => {
      // Login before each test
      await page.goto('/login');
      await page.fill('input[name="email"]', 'test@example.com');
      await page.fill('input[name="password"]', 'TestPassword123!');
      await page.click('button[type="submit"]');
      await page.waitForURL('/');
    });

    test('should add product to cart', async ({ page }) => {
      await page.goto('/products');
      await page.click('[data-testid="add-to-cart-button"]:first');
      
      await expect(page.locator('[data-testid="cart-count"]')).toHaveText('1');
    });

    test('should proceed to checkout', async ({ page }) => {
      await page.goto('/products');
      await page.click('[data-testid="add-to-cart-button"]:first');
      await page.click('[data-testid="cart-icon"]');
      await page.click('[data-testid="checkout-button"]');
      
      await expect(page).toHaveURL('/checkout');
    });

    test('should complete checkout', async ({ page }) => {
      await page.goto('/products');
      await page.click('[data-testid="add-to-cart-button"]:first');
      await page.click('[data-testid="cart-icon"]');
      await page.click('[data-testid="checkout-button"]');
      
      // Fill shipping
      await page.fill('input[name="fullName"]', 'John Doe');
      await page.fill('input[name="address"]', '123 Main St');
      await page.fill('input[name="city"]', 'New York');
      await page.fill('input[name="zipCode"]', '10001');
      await page.selectOption('select[name="country"]', 'US');
      await page.click('[data-testid="continue-to-payment"]');
      
      // Fill payment
      await page.fill('input[name="cardNumber"]', '4242424242424242');
      await page.fill('input[name="expiryDate"]', '12/25');
      await page.fill('input[name="cvv"]', '123');
      await page.click('[data-testid="place-order-button"]');
      
      await page.waitForURL(/\/order\/confirmation/);
      await expect(page.locator('h1')).toContainText('Order Confirmation');
    });
  });

  test.describe('Protected Routes', () => {
    test('should redirect unauthenticated users to login', async ({ page }) => {
      await page.goto('/dashboard');
      await page.waitForURL('/login');
      await expect(page).toHaveURL('/login');
    });

    test('should allow authenticated users to access protected routes', async ({ page }) => {
      // Login
      await page.goto('/login');
      await page.fill('input[name="email"]', 'test@example.com');
      await page.fill('input[name="password"]', 'TestPassword123!');
      await page.click('button[type="submit"]');
      await page.waitForURL('/');
      
      // Access protected route
      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');
      await expect(page).toHaveURL('/dashboard');
    });
  });

  test.describe('Responsive Design', () => {
    test('should render correctly on mobile', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      await page.goto('/');
      
      await expect(page.locator('[data-testid="mobile-nav"]')).toBeVisible();
      await expect(page.locator('[data-testid="desktop-nav"]')).not.toBeVisible();
    });

    test('should render correctly on desktop', async ({ page }) => {
      await page.setViewportSize({ width: 1920, height: 1080 });
      await page.goto('/');
      
      await expect(page.locator('[data-testid="desktop-nav"]')).toBeVisible();
      await expect(page.locator('[data-testid="mobile-nav"]')).not.toBeVisible();
    });

    test('should render correctly on tablet', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      await page.goto('/');
      
      await expect(page.locator('[data-testid="responsive-layout"]')).toBeVisible();
    });
  });
});
