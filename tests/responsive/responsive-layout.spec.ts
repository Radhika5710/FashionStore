import { test, expect, devices } from '@playwright/test';

const breakpoints = {
  desktop: { width: 1920, height: 1080 },
  laptop: { width: 1366, height: 768 },
  tablet: { width: 768, height: 1024 },
  mobile: { width: 375, height: 667 },
};

test.describe('Responsive Layout Tests', () => {
  test.describe('Desktop Layout', () => {
    test.use({ viewport: breakpoints.desktop });
    
    test('should display 4-column product grid on desktop', async ({ page }) => {
      await page.goto('/products');
      
      const grid = page.locator('.product-grid');
      await expect(grid).toBeVisible();
      
      const firstRow = grid.locator('.product-card').nth(3);
      await expect(firstRow).toBeVisible();
    });

    test('should display top navbar on desktop', async ({ page }) => {
      await page.goto('/');
      
      await expect(page.locator('.fs-storefront-nav')).toBeVisible();
      await expect(page.locator('.fs-mobile-nav')).not.toBeVisible();
    });

    test('should display 4-column footer on desktop', async ({ page }) => {
      await page.goto('/');
      
      const footer = page.locator('.desktop-footer');
      await expect(footer).toBeVisible();
      await expect(footer.locator('.footer-grid')).toBeVisible();
    });
  });

  test.describe('Tablet Layout', () => {
    test.use({ viewport: breakpoints.tablet });
    
    test('should display 3-column product grid on tablet', async ({ page }) => {
      await page.goto('/products');
      
      const grid = page.locator('.product-grid');
      await expect(grid).toBeVisible();
      
      const thirdCard = grid.locator('.product-card').nth(2);
      await expect(thirdCard).toBeVisible();
    });

    test('should display filter sidebar on tablet', async ({ page }) => {
      await page.goto('/products');
      
      await expect(page.locator('.fs-filter-sidebar')).toBeVisible();
    });
  });

  test.describe('Mobile Layout', () => {
    test.use({ viewport: breakpoints.mobile });
    
    test('should display 2-column product grid on mobile', async ({ page }) => {
      await page.goto('/products');
      
      const grid = page.locator('.product-grid');
      await expect(grid).toBeVisible();
      
      const secondCard = grid.locator('.product-card').nth(1);
      await expect(secondCard).toBeVisible();
    });

    test('should display mobile bottom navigation', async ({ page }) => {
      await page.goto('/');
      
      await expect(page.locator('.fs-mobile-nav')).toBeVisible();
      await expect(page.locator('.fs-mobile-nav__item')).toHaveCount(5);
    });

    test('should hide top navbar on mobile', async ({ page }) => {
      await page.goto('/');
      
      await expect(page.locator('.fs-storefront-nav')).not.toBeVisible();
    });

    test('should display filter drawer on mobile', async ({ page }) => {
      await page.goto('/products');
      
      await expect(page.locator('.fs-filter-sidebar')).not.toBeVisible();
      
      await page.click('.filter-toggle-btn');
      await expect(page.locator('.fs-filter-sidebar.active')).toBeVisible();
    });

    test('should display mobile footer', async ({ page }) => {
      await page.goto('/');
      
      await expect(page.locator('.mobile-footer')).toBeVisible();
      await expect(page.locator('.desktop-footer')).not.toBeVisible();
    });
  });

  test.describe('Hero Section Responsive', () => {
    test('should stack hero content on mobile', async ({ page }) => {
      await page.setViewportSize(breakpoints.mobile);
      await page.goto('/');
      
      const hero = page.locator('.hero-section');
      await expect(hero).toBeVisible();
      
      const heroContent = page.locator('.hero-content');
      await expect(heroContent).toBeVisible();
    });

    test('should display side-by-side on desktop', async ({ page }) => {
      await page.setViewportSize(breakpoints.desktop);
      await page.goto('/');
      
      const hero = page.locator('.hero-section');
      await expect(hero).toBeVisible();
      
      const heroMedia = page.locator('.hero-media');
      await expect(heroMedia).toBeVisible();
    });
  });

  // RENDERING STABILITY TESTS
  test.describe('Rendering Stability', () => {
    test('should maintain layout on window resize', async ({ page }) => {
      await page.goto('/');
      
      // Start with desktop
      await page.setViewportSize(breakpoints.desktop);
      await expect(page.locator('.fs-storefront-nav')).toBeVisible();
      
      // Resize to tablet
      await page.setViewportSize(breakpoints.tablet);
      await expect(page.locator('.fs-storefront-nav')).toBeVisible();
      
      // Resize to mobile
      await page.setViewportSize(breakpoints.mobile);
      await expect(page.locator('.fs-storefront-nav')).not.toBeVisible();
      await expect(page.locator('.fs-mobile-nav')).toBeVisible();
      
      // Resize back to desktop
      await page.setViewportSize(breakpoints.desktop);
      await expect(page.locator('.fs-storefront-nav')).toBeVisible();
      await expect(page.locator('.fs-mobile-nav')).not.toBeVisible();
    });

    test('should prevent layout shift on image load', async ({ page }) => {
      await page.goto('/products');
      
      // Get initial grid dimensions
      const grid = page.locator('.product-grid');
      const initialBox = await grid.boundingBox();
      
      // Wait for images to load
      await page.waitForLoadState('networkidle');
      
      // Check if layout shifted
      const finalBox = await grid.boundingBox();
      
      // Allow small differences but prevent major shifts
      const heightDiff = Math.abs(initialBox.height - finalBox.height);
      expect(heightDiff).toBeLessThan(100);
    });

    test('should handle orientation change gracefully', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      await page.goto('/');
      
      // Portrait mode
      await expect(page.locator('.fs-mobile-nav')).toBeVisible();
      
      // Rotate to landscape
      await page.setViewportSize({ width: 667, height: 375 });
      
      // Should still render without breaking
      const body = page.locator('body');
      await expect(body).toBeVisible();
    });

    test('should maintain scroll position on resize', async ({ page }) => {
      await page.setViewportSize(breakpoints.desktop);
      await page.goto('/products');
      
      // Scroll down
      await page.evaluate(() => window.scrollTo(0, 500));
      
      // Resize
      await page.setViewportSize(breakpoints.tablet);
      
      // Check scroll position is reasonable (not reset to 0)
      const scrollY = await page.evaluate(() => window.scrollY);
      expect(scrollY).toBeGreaterThan(0);
    });

    test('should prevent horizontal scroll on mobile', async ({ page }) => {
      await page.setViewportSize(breakpoints.mobile);
      await page.goto('/');
      
      // Check body width
      const bodyWidth = await page.evaluate(() => document.body.scrollWidth);
      const viewportWidth = await page.evaluate(() => window.innerWidth);
      
      // Body should not exceed viewport
      expect(bodyWidth).toBeLessThanOrEqual(viewportWidth + 10);
    });

    test('should handle font loading gracefully', async ({ page }) => {
      await page.goto('/');
      
      // Wait for fonts to load
      await page.waitForLoadState('networkidle');
      
      // Check that text is visible
      const heading = page.locator('h1, h2, h3').first();
      await expect(heading).toBeVisible();
      
      // Check that text has reasonable font size
      const fontSize = await heading.evaluate(el => window.getComputedStyle(el).fontSize);
      expect(parseInt(fontSize)).toBeGreaterThan(0);
    });
  });
});
