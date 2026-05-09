/**
 * ANIMATION ENGINE - Advanced Scroll Animations
 * Reveal-on-scroll, stagger animations, smooth page transitions
 */

(function() {
    'use strict';
    
    // ============================================
    // INTERSECTION OBSERVER FOR SCROLL ANIMATIONS
    // ============================================
    
    const AnimationEngine = {
        init: function() {
            this.initScrollReveal();
            this.initStaggerAnimations();
            this.initParallax();
            this.initSmoothScroll();
        },
        
        // ============================================
        // SCROLL REVEAL ANIMATIONS
        // ============================================
        
        initScrollReveal: function() {
            const revealElements = document.querySelectorAll('[data-reveal]');
            
            if (revealElements.length === 0) return;
            
            const revealObserver = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('revealed');
                        revealObserver.unobserve(entry.target);
                    }
                });
            }, {
                threshold: 0.1,
                rootMargin: '0px 0px -50px 0px'
            });
            
            revealElements.forEach(el => {
                el.classList.add('reveal-hidden');
                revealObserver.observe(el);
            });
        },
        
        // ============================================
        // STAGGER ANIMATIONS
        // ============================================
        
        initStaggerAnimations: function() {
            const staggerContainers = document.querySelectorAll('[data-stagger]');
            
            staggerContainers.forEach(container => {
                const children = container.children;
                const delay = parseInt(container.dataset.staggerDelay) || 100;
                
                Array.from(children).forEach((child, index) => {
                    child.style.opacity = '0';
                    child.style.transform = 'translateY(20px)';
                    child.style.transition = `opacity 0.6s cubic-bezier(0.4, 0, 0.2, 1), transform 0.6s cubic-bezier(0.4, 0, 0.2, 1)`;
                    child.style.transitionDelay = `${index * delay}ms`;
                });
                
                const observer = new IntersectionObserver((entries) => {
                    entries.forEach(entry => {
                        if (entry.isIntersecting) {
                            Array.from(children).forEach(child => {
                                child.style.opacity = '1';
                                child.style.transform = 'translateY(0)';
                            });
                            observer.unobserve(container);
                        }
                    });
                }, { threshold: 0.1 });
                
                observer.observe(container);
            });
        },
        
        // ============================================
        // PARALLAX EFFECT
        // ============================================
        
        initParallax: function() {
            const parallaxElements = document.querySelectorAll('[data-parallax]');
            
            if (parallaxElements.length === 0) return;
            
            let ticking = false;
            
            window.addEventListener('scroll', () => {
                if (!ticking) {
                    window.requestAnimationFrame(() => {
                        const scrollY = window.pageYOffset;
                        
                        parallaxElements.forEach(el => {
                            const speed = parseFloat(el.dataset.parallax) || 0.5;
                            const offset = scrollY * speed;
                            el.style.transform = `translateY(${offset}px)`;
                        });
                        
                        ticking = false;
                    });
                    
                    ticking = true;
                }
            }, { passive: true });
        },
        
        // ============================================
        // SMOOTH SCROLL
        // ============================================
        
        initSmoothScroll: function() {
            document.querySelectorAll('a[href^="#"]').forEach(anchor => {
                anchor.addEventListener('click', function(e) {
                    const href = this.getAttribute('href');
                    
                    if (href === '#') return;
                    
                    const target = document.querySelector(href);
                    
                    if (target) {
                        e.preventDefault();
                        
                        target.scrollIntoView({
                            behavior: 'smooth',
                            block: 'start'
                        });
                    }
                });
            });
        }
    };
    
    // ============================================
    // CSS CLASSES FOR ANIMATIONS
    // ============================================
    
    // Add CSS dynamically
    const style = document.createElement('style');
    style.textContent = `
        .reveal-hidden {
            opacity: 0;
            transform: translateY(30px);
            transition: opacity 0.8s cubic-bezier(0.4, 0, 0.2, 1),
                        transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
        }
        
        .reveal-hidden[data-reveal="left"] {
            transform: translateX(-30px);
        }
        
        .reveal-hidden[data-reveal="right"] {
            transform: translateX(30px);
        }
        
        .reveal-hidden[data-reveal="scale"] {
            transform: scale(0.9);
        }
        
        .revealed {
            opacity: 1 !important;
            transform: translateY(0) translateX(0) scale(1) !important;
        }
        
        [data-stagger] > * {
            will-change: opacity, transform;
        }
        
        [data-parallax] {
            will-change: transform;
        }
    `;
    document.head.appendChild(style);
    
    // ============================================
    // INITIALIZE
    // ============================================
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => AnimationEngine.init());
    } else {
        AnimationEngine.init();
    }
    
    // Expose for external use
    window.AnimationEngine = AnimationEngine;
})();
