import { test, expect } from '@playwright/test';

test.describe('Token Refresh Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login with valid credentials
    await page.goto('/login');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="password"]', 'TestPassword123!');
    await page.click('button[type="submit"]');
    
    // Wait for successful login
    await page.waitForURL('/');
    await expect(page).toHaveURL('/');
  });

  test('should refresh access token when expired', async ({ page, context }) => {
    // Clear access token to simulate expiration
    const cookies = await context.cookies();
    const accessTokenCookie = cookies.find(c => c.name === 'access_token');
    
    if (accessTokenCookie) {
      await context.clearCookies();
      // Keep refresh token, remove access token
      const refreshTokenCookie = cookies.find(c => c.name === 'refresh_token');
      if (refreshTokenCookie) {
        await context.addCookies([refreshTokenCookie]);
      }
    }

    // Navigate to protected route
    await page.goto('/dashboard');
    
    // Should automatically refresh token and load page
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL('/dashboard');
    
    // Verify new access token is set
    const newCookies = await context.cookies();
    const newAccessToken = newCookies.find(c => c.name === 'access_token');
    expect(newAccessToken).toBeDefined();
  });

  test('should redirect to login when refresh token is invalid', async ({ page, context }) => {
    // Clear all cookies to simulate expired refresh token
    await context.clearCookies();

    // Navigate to protected route
    await page.goto('/dashboard');
    
    // Should redirect to login
    await page.waitForURL('/login');
    await expect(page).toHaveURL('/login');
  });

  test('should handle concurrent token refresh requests', async ({ page, context }) => {
    // Clear access token
    const cookies = await context.cookies();
    const accessTokenCookie = cookies.find(c => c.name === 'access_token');
    
    if (accessTokenCookie) {
      await context.clearCookies();
      const refreshTokenCookie = cookies.find(c => c.name === 'refresh_token');
      if (refreshTokenCookie) {
        await context.addCookies([refreshTokenCookie]);
      }
    }

    // Make multiple concurrent requests to protected routes
    const promises = [
      page.goto('/dashboard'),
      page.goto('/profile'),
      page.goto('/orders'),
    ];

    await Promise.all(promises);
    
    // Should handle gracefully without multiple refresh requests
    await page.waitForLoadState('networkidle');
    const finalCookies = await context.cookies();
    const newAccessToken = finalCookies.find(c => c.name === 'access_token');
    expect(newAccessToken).toBeDefined();
  });
});
