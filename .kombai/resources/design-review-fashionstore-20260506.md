# Design Review Results: FashionStore — All Pages

**Review Date**: 2026-05-06  
**Pages Reviewed**: `/home`, `/products`, `/product-details`, `/cart`, `/checkout`, `/login`, `/register`, `/orders`, `/404`, `/error`  
**Focus Areas**: Visual Design, UX/Usability, Responsive/Mobile, Accessibility, Micro-interactions/Motion, Consistency, Performance

> **Note**: This review was conducted through static code analysis only, as the development server was not running. Visual inspection via browser would provide additional insights into layout rendering, interactive behaviors, and actual appearance.

---

## Summary

FashionStore has a solid visual concept with a consistent indigo color palette, card-based layouts, and thoughtful component design. However, there are **critical broken-style issues** caused by a split design-token naming convention across CSS files — most page-level stylesheets reference CSS variables that are never defined in the global `style.css`, causing entire pages to render without colors, spacing, or typography. Additionally, several high-priority accessibility and structural HTML issues need addressing before the app can be considered production-ready.

---

## Issues

| # | Issue | Criticality | Category | Location |
|---|-------|-------------|----------|----------|
| 1 | **Split CSS variable naming system**: `style.css` defines `--primary-color`, `--text-primary`, `--bg-color`, `--surface-color`, `--border-color` etc., but `home.css`, `cart.css`, `checkout.css`, `auth.css`, and `orders.css` all reference `--color-primary`, `--color-secondary`, `--color-bg`, `--color-surface`, `--color-border`, `--color-surface-alt`, `--color-danger`, `--color-success`, `--color-accent`, `--font-display`, `--transition-base`, `--transition-fast` — **none of which are defined anywhere**. This means entire sections of layout, color, and typography silently fall back to browser defaults. | 🔴 Critical | Consistency / Visual Design | `assets/css/style.css:3-37`, `home.css`, `cart.css`, `checkout.css`, `auth.css`, `orders.css` (all files) |
| 2 | **Missing spacing tokens `--space-6`, `--space-7`, `--space-8`**: `style.css` only defines `--space-1` through `--space-5`. At least `home.css`, `cart.css`, `checkout.css`, `auth.css`, `orders.css` all use `--space-6` (and some `--space-7`, `--space-8`). Padding and margins on major sections resolve to `0`, collapsing layouts. | 🔴 Critical | Visual Design / Consistency | `assets/css/style.css:16-21`, `cart.css:3`, `checkout.css:4`, `home.css:79`, `auth.css:6`, `orders.css:3` |
| 3 | **Unclosed `<div class="app-wrapper">`**: `navbar.jsp` opens `<div class="app-wrapper">` (line 17) but `footer.jsp` does not close it. Only `products.jsp` manually adds a closing `</div>` (line 192). All other pages (`home`, `cart`, `checkout`, `login`, `register`, `orders`, `product-details`) have an unclosed div, resulting in invalid HTML. | 🔴 Critical | UX / Visual Design | `WEB-INF/views/partials/navbar.jsp:17`, `partials/footer.jsp` (missing closing tag) |
| 4 | **Cart badge hardcoded to `16`**: The navbar cart badge always displays "16" regardless of actual cart contents. It is never read from the session. | 🟠 High | UX / Consistency | `WEB-INF/views/partials/navbar.jsp:54` |
| 5 | **Products page: product cards are not clickable links**: Each `div.product-card` on the products listing is not wrapped in an `<a>` tag. The only interaction is the price/cart button. Users cannot click the product image or name to view details (unlike `home.jsp` where cards are full anchor links). | 🟠 High | UX / Usability | `WEB-INF/views/products.jsp:153-176` |
| 6 | **Form labels missing `for` attribute; inputs missing `id`**: All `<label>` elements in `login.jsp` and `register.jsp` lack `for="..."` attributes, and their corresponding inputs have no `id` attribute. Clicking a label does not focus its input, and screen readers cannot associate the label with the control. | 🟠 High | Accessibility | `WEB-INF/views/login.jsp:25-29`, `register.jsp:29-52` |
| 7 | **Missing `lang` attribute on `<html>`**: `home.jsp`, `login.jsp`, `register.jsp`, `orders.jsp`, `product-details.jsp` are missing `lang="en"`. Screen readers and search engines rely on this to determine the document language. | 🟠 High | Accessibility | `home.jsp:5`, `login.jsp:4`, `register.jsp:3`, `orders.jsp:6`, `product-details.jsp:5` |
| 8 | **No `:focus-visible` styles on any interactive element**: Pills (`.pill`), nav items (`.nav-item`), buttons (`.fav-btn`, `.price-btn`, `.qty-btn`, `.remove-btn`), and product cards have only `:hover` states. Keyboard users navigating via Tab key see no focus ring. This fails WCAG 2.4.7 (Focus Visible). | 🟠 High | Accessibility | `assets/css/style.css`, `products.css`, `cart.css` (all interactive elements) |
| 9 | **Orders page displays Product ID instead of product name or image**: `order.jsp` shows `<p><strong>Product ID:</strong> <%= item.getProductId() %></p>` in each order item box. Users cannot identify which products they ordered without a database lookup. Should show product name, image, and price per unit. | 🟠 High | UX / Usability | `WEB-INF/views/orders.jsp:79` |
| 10 | **No responsive breakpoints for the products sidebar**: `products.css` has no `@media` queries. The filter sidebar is `flex: 0 0 280px` and never collapses or hides on smaller viewports. On tablet/mobile, content is severely cramped. | 🟠 High | Responsive / Mobile | `assets/css/products.css:9-16` |
| 11 | **Hardcoded inline styles bypass the design system in multiple pages**: Colors, spacing, and border-radius values are hard-coded as inline `style="..."` attributes: error message in `cart.jsp`, error text in `register.jsp`, link colors in `login.jsp` and `register.jsp`, status badge colors in `orders.jsp`, and price range slider labels in `products.jsp`. These are invisible to the design system and create maintenance drift. | 🟡 Medium | Consistency / Visual Design | `cart.jsp:48`, `register.jsp:24,44,57-58`, `login.jsp:34-35`, `orders.jsp:50-58`, `products.jsp:67-69` |
| 12 | **Filter controls in products.jsp are non-functional or incomplete**: Category pills (Men, Women, Accessories, etc.) all link to `#` (line 39-47). The price range `<input type="range">` has no `name` attribute and cannot be submitted (line 66). Brand checkboxes are hardcoded HTML, not populated from the database. Star rating filter is display-only. | 🟡 Medium | UX / Usability | `WEB-INF/views/products.jsp:38-47`, `products.jsp:66`, `products.jsp:94-126` |
| 13 | **Checkout form validation uses a native `alert()` dialog**: On submit, if fields are empty the script calls `alert('Please fill all shipping details.')` — a jarring modal that blocks the entire browser. Should use inline validation messages below each field. | 🟡 Medium | UX / Micro-interactions | `WEB-INF/views/checkout.jsp:174` |
| 14 | **Cart item removal uses a native `confirm()` dialog**: `cart.js` shows a browser-native `confirm('Are you sure you want to remove this item?')` dialog. This is disruptive and visually inconsistent. Should be replaced with an inline undo-toast or a smooth dismiss animation. | 🟡 Medium | UX / Micro-interactions | `assets/js/cart.js:24` |
| 15 | **Duplicate Google Fonts loading on every page**: `style.css` has `@import url('https://fonts.googleapis.com/...')` at line 1. Every JSP also loads the same font via a `<link>` tag in `<head>`. This causes two separate network requests for the same font resource on every page load. | 🟡 Medium | Performance | `assets/css/style.css:1`, `home.jsp:12`, `products.jsp:14`, `cart.jsp:14`, `checkout.jsp:14`, etc. |
| 16 | **Double `border-radius` declaration on `.navbar`**: `style.css` sets `border-radius: 0 0 var(--radius-xl) var(--radius-xl)` at line 77, then immediately overrides it with `border-radius: var(--radius-lg)` at line 83. The first declaration is dead code. | 🟡 Medium | Visual Design | `assets/css/style.css:77`, `style.css:83` |
| 17 | **Missing `<meta charset="UTF-8">` declaration on several pages**: `home.jsp`, `login.jsp`, `register.jsp`, and `orders.jsp` rely only on the JSP `contentType` directive and lack an explicit `<meta charset="UTF-8">` in `<head>`. Content-type headers may be stripped by proxies; an explicit meta tag is the safe standard. | 🟡 Medium | Accessibility / Performance | `home.jsp:7-14`, `login.jsp:5-12`, `register.jsp:5-12`, `orders.jsp:7-15` |
| 18 | **Inconsistent page `<title>` formatting**: `home.jsp` uses `<title>Home</title>` with no brand name, while `products.jsp`, `cart.jsp`, `checkout.jsp`, and `orders.jsp` use `"Page Name – FashionStore"`. SEO and browser tab identity is inconsistent. | 🟡 Medium | Consistency / Performance (SEO) | `home.jsp:8`, `login.jsp:6`, `orders.jsp:9`, `product-details.jsp:8` |
| 19 | **Potential `NullPointerException` on checkout page**: `checkout.jsp` line 26 casts `request.getAttribute("cartTotal")` directly to `Double` with no null check: `double cartTotal = (Double) request.getAttribute("cartTotal")`. If the controller doesn't set this attribute (e.g. on an error redirect), the page throws a 500 error. | 🟡 Medium | UX / Performance | `WEB-INF/views/checkout.jsp:26` |
| 20 | **`product-details.css` loaded before `style.css`**: In `product-details.jsp`, the page-specific CSS is linked before the global `style.css`. If `product-details.css` references CSS variables from `style.css`, those variables will not yet be defined when it is parsed. All other pages load `style.css` first. | 🟡 Medium | Consistency / Performance | `WEB-INF/views/product-details.jsp:12-13` |
| 21 | **`background-attachment: fixed` on hero section causes iOS scroll jank**: `home.css` uses `background-attachment: fixed` on the hero (line 16). This property triggers full-page repaints on every scroll tick on iOS Safari and is generally not GPU-composited. Should use a parallax JS approach or be removed for mobile. | ⚪ Low | Performance / Responsive | `assets/css/home.css:16` |
| 22 | **Cart empty state uses emoji icon**: `cart.jsp` uses `<div class="empty-icon">🛍️</div>`. Emoji rendering differs significantly across operating systems and may appear as a tofu box or incorrect glyph. Should use an inline SVG (consistent with the shopping cart SVG already used in the navbar). | ⚪ Low | Visual Design / Consistency | `WEB-INF/views/cart.jsp:165` |
| 23 | **Cart toast notification built entirely with inline JS styles**: The `showToast()` function in `cart.js` (lines 133–144) applies all positioning, color, font, and z-index as JavaScript inline styles. This bypasses the design system, makes the toast non-themeable, and adds JS complexity. Should be a CSS class toggled via JS. | ⚪ Low | Consistency / Performance | `assets/js/cart.js:128-150` |
| 24 | **Internal database seed instructions exposed in production UI**: The empty state in `home.jsp` displays `` Run `schema.sql` seed inserts to load stock items and product photos. `` — a developer note that is visible to end users when the product catalog is empty. | ⚪ Low | UX / Consistency | `WEB-INF/views/home.jsp:73` |

---

## Criticality Legend

- 🔴 **Critical**: Breaks functionality, silently corrupts styles/layout, or violates foundational standards
- 🟠 **High**: Significantly impacts user experience, accessibility, or design quality
- 🟡 **Medium**: Noticeable issue that should be addressed before launch
- ⚪ **Low**: Nice-to-have improvement for polish or robustness

---

## Next Steps

### Immediate (Pre-launch blockers)

1. **Unify CSS variables** — Decide on one naming convention (`--color-primary` or `--primary-color`) and update all files consistently. The most complete token set used in page CSS files (the `--color-*` convention) should be the authoritative one; update `style.css` to define all missing variables.
2. **Add `--space-6`, `--space-7`, `--space-8` tokens** to `style.css` (suggested: `56px`, `72px`, `96px`).
3. **Close `<div class="app-wrapper">`** — Either move the closing tag into `footer.jsp`, or remove the wrapper entirely and rely on `body` for layout.
4. **Fix cart badge** — Read cart count from session in `navbar.jsp` instead of the hardcoded `16`.
5. **Wrap product cards in `products.jsp` with `<a href="/product?id=...">` links**.
6. **Add `for`/`id` pairs to all form labels and inputs** in `login.jsp` and `register.jsp`.

### Short-term (Sprint 2)
- Add `lang="en"` and `<meta charset="UTF-8">` to all pages
- Add `focus-visible` ring styles to all interactive elements (one CSS rule in `style.css`)
- Replace `alert()` / `confirm()` with inline validation and toast-based confirmation
- Add responsive breakpoints to `products.css` to collapse the sidebar on `max-width: 900px`
- Show product name + image in Orders page items instead of bare Product ID
- Remove the SQL seed instruction from the home empty state

### Polish (Sprint 3)
- Remove duplicate Google Fonts loading — keep `<link>` in HTML only, remove `@import` from CSS
- Fix double `border-radius` on `.navbar`
- Replace emoji icon in empty cart state with SVG
- Move cart toast styles to a CSS class
- Add null guard for `cartTotal` in `checkout.jsp`
- Fix CSS load order in `product-details.jsp`
- Remove `background-attachment: fixed` on mobile via a media query
