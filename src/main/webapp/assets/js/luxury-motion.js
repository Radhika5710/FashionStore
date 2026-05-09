/**
 * LUXURY MOTION - Cinematic premium interactions
 * - Splash logo reveal
 * - Scroll-driven parallax
 * - Reveal-on-scroll with staggered fade
 * - Magnetic button effects
 * - Navbar scroll state
 * - Cursor glow (desktop only)
 */
(function () {
    'use strict';

    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const isTouch = ('ontouchstart' in window) || navigator.maxTouchPoints > 0;

    /* ---------- 1. PREMIUM SPLASH ---------- */
    function initSplash() {
        if (sessionStorage.getItem('lux_splash_shown') === '1') return;
        if (reduced) { sessionStorage.setItem('lux_splash_shown', '1'); return; }

        const splash = document.createElement('div');
        splash.id = 'lux-splash';
        splash.innerHTML = `
            <div class="lux-splash-inner">
                <div class="lux-splash-logo">
                    <span class="lux-splash-letter">F</span><span class="lux-splash-letter">A</span><span class="lux-splash-letter">S</span><span class="lux-splash-letter">H</span><span class="lux-splash-letter">I</span><span class="lux-splash-letter">O</span><span class="lux-splash-letter">N</span><span class="lux-splash-letter lux-splash-amp">.</span><span class="lux-splash-letter">S</span><span class="lux-splash-letter">T</span><span class="lux-splash-letter">O</span><span class="lux-splash-letter">R</span><span class="lux-splash-letter">E</span>
                </div>
                <div class="lux-splash-tagline">Luxury Everyday Essentials</div>
                <div class="lux-splash-bar"><span></span></div>
            </div>
        `;
        document.documentElement.classList.add('lux-loading');
        document.body.appendChild(splash);

        sessionStorage.setItem('lux_splash_shown', '1');

        const total = 2200;
        setTimeout(() => {
            splash.classList.add('lux-splash-out');
            document.documentElement.classList.remove('lux-loading');
            setTimeout(() => splash.remove(), 1100);
        }, total);
    }

    /* ---------- 2. SCROLL REVEAL ---------- */
    function initRevealOnScroll() {
        const targets = document.querySelectorAll(
            '.reveal-on-scroll, [data-reveal], .product-card, .category-tile, .campaign-card, .trust-card, .editorial-band, .featured-section .section-head, .social-shot'
        );
        if (!targets.length) return;

        targets.forEach((el, i) => {
            if (!el.classList.contains('reveal-on-scroll')) {
                el.classList.add('reveal-on-scroll');
            }
            const delay = (el.dataset.revealDelay) ? parseInt(el.dataset.revealDelay, 10) : Math.min(i * 60, 480);
            el.style.transitionDelay = delay + 'ms';
        });

        if (!('IntersectionObserver' in window)) {
            targets.forEach(el => el.classList.add('is-visible'));
            return;
        }

        const io = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('is-visible');
                    io.unobserve(entry.target);
                }
            });
        }, { threshold: 0.12, rootMargin: '0px 0px -10% 0px' });

        targets.forEach(el => io.observe(el));
    }

    /* ---------- 3. NAVBAR SCROLL STATE ---------- */
    function initNavbarScroll() {
        const nav = document.querySelector('.navbar');
        if (!nav) return;

        let ticking = false;
        function update() {
            if (window.scrollY > 24) {
                nav.classList.add('scrolled');
            } else {
                nav.classList.remove('scrolled');
            }
            ticking = false;
        }
        window.addEventListener('scroll', () => {
            if (!ticking) {
                requestAnimationFrame(update);
                ticking = true;
            }
        }, { passive: true });
        update();
    }

    /* ---------- 4. PARALLAX (hero gradient orbs) ---------- */
    function initParallax() {
        if (reduced) return;
        const orbs = document.querySelectorAll('.hero-gradient-orb, .lux-orb, .editorial-image');
        if (!orbs.length) return;

        let ticking = false;
        function update() {
            const y = window.scrollY;
            orbs.forEach(el => {
                const speed = parseFloat(el.dataset.parallax || '0.15');
                el.style.transform = `translate3d(0, ${y * speed}px, 0)`;
            });
            ticking = false;
        }
        window.addEventListener('scroll', () => {
            if (!ticking) {
                requestAnimationFrame(update);
                ticking = true;
            }
        }, { passive: true });
    }

    /* ---------- 5. MAGNETIC BUTTONS ---------- */
    function initMagnetic() {
        if (reduced || isTouch) return;
        const els = document.querySelectorAll('.btn-primary, .btn-secondary, .footer-social-link, .nav-action-btn');
        els.forEach(el => {
            el.addEventListener('mousemove', (e) => {
                const rect = el.getBoundingClientRect();
                const x = e.clientX - rect.left - rect.width / 2;
                const y = e.clientY - rect.top - rect.height / 2;
                el.style.transform = `translate(${x * 0.18}px, ${y * 0.22}px)`;
            });
            el.addEventListener('mouseleave', () => {
                el.style.transform = '';
            });
        });
    }

    /* ---------- 6. CURSOR GLOW (subtle ambient light) ---------- */
    function initCursorGlow() {
        if (reduced || isTouch || window.innerWidth < 1024) return;
        const glow = document.createElement('div');
        glow.id = 'lux-cursor-glow';
        document.body.appendChild(glow);

        let mx = 0, my = 0, cx = 0, cy = 0;
        document.addEventListener('mousemove', (e) => { mx = e.clientX; my = e.clientY; });

        function loop() {
            cx += (mx - cx) * 0.12;
            cy += (my - cy) * 0.12;
            glow.style.transform = `translate3d(${cx - 200}px, ${cy - 200}px, 0)`;
            requestAnimationFrame(loop);
        }
        loop();
    }

    /* ---------- 7. PRODUCT CARD STAGGERED LOAD ---------- */
    function initProductStagger() {
        const grids = document.querySelectorAll('.product-grid, .home-product-grid');
        grids.forEach(grid => {
            const cards = grid.querySelectorAll('.product-card');
            cards.forEach((card, i) => {
                card.style.transitionDelay = Math.min(i * 80, 600) + 'ms';
            });
        });
    }

    /* ---------- INIT ---------- */
    function init() {
        initSplash();
        initNavbarScroll();
        initRevealOnScroll();
        initParallax();
        initMagnetic();
        initCursorGlow();
        initProductStagger();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
