/**
 * FashionStore - Customer MVC Session Authentication Handler
 * 
 * IMPORTANT: This script handles CUSTOMER LOGIN ONLY using SESSIONS.
 * DO NOT add JWT logic here. JWT is for admin APIs only (/api/admin/*).
 * 
 * Features:
 * - Password visibility toggle
 * - Loading spinner with debounce protection
 * - Duplicate form submission prevention
 * - Graceful API error handling
 * - Inline validation states
 * - Optimized performance
 * 
 * Authentication Flow:
 * 1. Form submission to /login (POST)
 * 2. Server validates credentials
 * 3. Server creates HttpSession
 * 4. Server returns JSON response with redirect
 * 5. Client redirects to /home
 * 6. Session maintained via JSESSIONID cookie
 */
document.addEventListener('DOMContentLoaded', () => {
    const contextPath = window.contextPath || '';

    // ── Helper utilities ─────────────────────────────────────────────────────

    const escapeHtml = (text) => {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    };

    const clearFormAlerts = () => {
        const alerts = document.querySelectorAll('.auth-alert');
        alerts.forEach(alert => alert.remove());
    };

    const showInlineError = (fieldId, message) => {
        const errorEl = document.getElementById(`${fieldId}-error`);
        if (errorEl) {
            errorEl.textContent = message;
            const inputEl = document.getElementById(fieldId);
            if (inputEl) {
                inputEl.classList.add('auth-field__input--error');
            }
        }
    };

    const clearInlineErrors = () => {
        const errorEls = document.querySelectorAll('.auth-field__error');
        errorEls.forEach(el => el.textContent = '');
        const inputEls = document.querySelectorAll('.auth-field__input');
        inputEls.forEach(el => el.classList.remove('auth-field__input--error'));
    };

    const showFormAlert = (message, type = 'error') => {
        clearFormAlerts();
        const alertHtml = `
            <div class="auth-alert auth-alert--${type}" role="alert">
                <span class="auth-alert__icon">${type === 'error' ? '⚠️' : '✓'}</span>
                <span class="auth-alert__message">${escapeHtml(message)}</span>
            </div>
        `;
        const form = document.querySelector('.auth-form');
        if (!form) return;
        form.insertAdjacentHTML('beforebegin', alertHtml);
    };

    // ── 1. Password Visibility Toggle ────────────────────────────────────────

    const togglePasswordBtns = document.querySelectorAll('.auth-toggle-password');
    togglePasswordBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const wrapper = btn.closest('.auth-password-wrapper');
            const input = wrapper?.querySelector('input');
            if (!input) return;

            const isPassword = input.getAttribute('type') === 'password';
            input.setAttribute('type', isPassword ? 'text' : 'password');
            btn.setAttribute('aria-pressed', isPassword ? 'true' : 'false');

            const showIcon = btn.querySelector('.auth-toggle-password__icon--show');
            const hideIcon = btn.querySelector('.auth-toggle-password__icon--hide');
            
            if (showIcon && hideIcon) {
                showIcon.style.display = isPassword ? 'none' : 'block';
                hideIcon.style.display = isPassword ? 'block' : 'none';
            }
        });
    });

    // ── 2. Inline Email Validation ───────────────────────────────────────────

    const emailInput = document.getElementById('email');
    if (emailInput) {
        let emailDebounceTimer = null;
        
        emailInput.addEventListener('input', () => {
            clearTimeout(emailDebounceTimer);
            clearInlineErrors();
            
            emailDebounceTimer = setTimeout(() => {
                const email = emailInput.value.trim();
                if (email && !email.match(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)) {
                    showInlineError('email', 'Please enter a valid email address');
                }
            }, 300);
        });

        emailInput.addEventListener('blur', () => {
            clearTimeout(emailDebounceTimer);
            const email = emailInput.value.trim();
            if (email && !email.match(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)) {
                showInlineError('email', 'Please enter a valid email address');
            }
        });
    }

    // ── 3. Form Submission with Debounce & Duplicate Prevention ───────────────

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        let isSubmitting = false;
        let submitDebounceTimer = null;

        loginForm.addEventListener('submit', (e) => {
            e.preventDefault();

            // Prevent duplicate submissions
            if (isSubmitting) {
                console.warn('Form submission already in progress');
                return;
            }

            // Clear any pending debounce
            clearTimeout(submitDebounceTimer);

            // Clear inline errors
            clearInlineErrors();
            clearFormAlerts();

            // Client-side validation
            const email = emailInput?.value.trim() || '';
            const passwordInput = document.getElementById('password');
            const password = passwordInput?.value || '';

            if (!email) {
                showInlineError('email', 'Email is required');
                return;
            }

            if (!email.match(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)) {
                showInlineError('email', 'Please enter a valid email address');
                return;
            }

            if (!password) {
                showInlineError('password', 'Password is required');
                return;
            }

            // Set submitting state
            isSubmitting = true;
            const submitBtn = loginForm.querySelector('.auth-submit-btn');
            submitBtn.classList.add('auth-submit-btn--loading');
            submitBtn.disabled = true;

            // Prepare form data
            const formData = new URLSearchParams(new FormData(loginForm));
            const actionUrl = loginForm.getAttribute('action');

            const resetSubmitState = () => {
                isSubmitting = false;
                submitBtn.classList.remove('auth-submit-btn--loading');
                submitBtn.disabled = false;
            };

            FashionStoreAPI.post('/login', { email, password })
            .then(data => {
                if (!data) return; // Already handled redirect

                if (data.success) {
                    // Update CSRF token
                    if (data.csrfToken) {
                        window.csrfToken = data.csrfToken;
                        const csrfInputs = document.querySelectorAll('input[name="csrfToken"]');
                        csrfInputs.forEach(inp => inp.value = data.csrfToken);
                    }

                    showFormAlert(data.message || 'Login successful! Redirecting...', 'success');

                    // Redirect after short delay
                    setTimeout(() => {
                        window.location.href = data.redirect || `${contextPath}/home`;
                    }, 800);
                } else {
                    showFormAlert(data.message || 'Authentication failed.');
                    resetSubmitState();
                }
            })
            .catch(err => {
                console.error('Auth AJAX error:', err);
                showFormAlert(err.message || 'A secure connection failure occurred. Please try again.');
                resetSubmitState();
            });
        });

        // Add debounce protection for rapid clicks
        loginForm.addEventListener('submit', () => {
            submitDebounceTimer = setTimeout(() => {
                isSubmitting = false;
            }, 1000);
        });
    }

    // ── 4. Password Strength (for registration page) ───────────────────────────

    const regPassword = document.getElementById('reg-password');
    const strengthFill = document.getElementById('password-strength-fill');
    const strengthText = document.getElementById('password-strength-text');
    const requirementItems = document.querySelectorAll('.auth-password-requirements__item');

    if (regPassword && strengthFill && strengthText) {
        const checkPasswordRequirements = (password) => {
            const requirements = {
                length: password.length >= 8,
                uppercase: /[A-Z]/.test(password),
                lowercase: /[a-z]/.test(password),
                number: /[0-9]/.test(password),
                special: /[^A-Za-z0-9]/.test(password)
            };
            return requirements;
        };

        const calculatePasswordStrength = (requirements) => {
            const score = Object.values(requirements).filter(Boolean).length;
            if (score <= 1) return 'weak';
            if (score <= 3) return 'medium';
            return 'strong';
        };

        const updateStrengthUI = (password) => {
            const requirements = checkPasswordRequirements(password);
            const strength = calculatePasswordStrength(requirements);

            // Update strength bar
            strengthFill.className = 'auth-strength-fill';
            if (strength === 'weak') {
                strengthFill.classList.add('auth-strength-fill--weak');
                strengthText.textContent = 'Weak Password';
                strengthText.style.color = '#ff4d4d';
            } else if (strength === 'medium') {
                strengthFill.classList.add('auth-strength-fill--medium');
                strengthText.textContent = 'Medium Password';
                strengthText.style.color = '#ffb84d';
            } else if (strength === 'strong') {
                strengthFill.classList.add('auth-strength-fill--strong');
                strengthText.textContent = 'Strong Password';
                strengthText.style.color = '#22c55e';
            } else {
                strengthText.textContent = 'Strength: None';
                strengthText.style.color = 'var(--color-text-tertiary)';
            }

            // Update requirement items
            requirementItems.forEach(item => {
                const requirement = item.getAttribute('data-requirement');
                if (requirements[requirement]) {
                    item.classList.add('valid');
                } else {
                    item.classList.remove('valid');
                }
            });

            return strength;
        };

        regPassword.addEventListener('input', () => {
            updateStrengthUI(regPassword.value);
        });
    }

    // ── 5. Confirm Password Match (for registration page) ─────────────────────

    const confirmPass = document.getElementById('confirmPassword');
    if (regPassword && confirmPass) {
        const checkMatch = () => {
            const errorEl = document.getElementById('confirmPassword-error');
            if (!confirmPass.value) {
                if (errorEl) errorEl.textContent = '';
                return;
            }

            if (regPassword.value === confirmPass.value) {
                if (errorEl) {
                    errorEl.textContent = '';
                    confirmPass.classList.remove('auth-field__input--error');
                }
            } else {
                if (errorEl) {
                    errorEl.textContent = 'Passwords do not match';
                    confirmPass.classList.add('auth-field__input--error');
                }
            }
        };
        confirmPass.addEventListener('input', checkMatch);
        regPassword.addEventListener('input', checkMatch);
    }

    // ── 6. Registration Form Validation ───────────────────────────────────────

    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        let isSubmitting = false;
        let submitDebounceTimer = null;

        // Real-time field validation
        const fullNameInput = document.getElementById('fullName');
        const phoneInput = document.getElementById('phone');
        const addressInput = document.getElementById('address');
        const genderSelect = document.getElementById('gender');

        // Full name validation
        if (fullNameInput) {
            fullNameInput.addEventListener('blur', () => {
                const value = fullNameInput.value.trim();
                if (value && value.length < 2) {
                    showInlineError('fullName', 'Name must be at least 2 characters');
                } else {
                    clearInlineErrors();
                }
            });
        }

        // Phone validation
        if (phoneInput) {
            phoneInput.addEventListener('blur', () => {
                const value = phoneInput.value.trim();
                if (value && !value.match(/^[\d\s\-\+\(\)]+$/)) {
                    showInlineError('phone', 'Please enter a valid phone number');
                } else {
                    clearInlineErrors();
                }
            });
        }

        // Address validation
        if (addressInput) {
            addressInput.addEventListener('blur', () => {
                const value = addressInput.value.trim();
                if (value && value.length < 5) {
                    showInlineError('address', 'Address must be at least 5 characters');
                } else {
                    clearInlineErrors();
                }
            });
        }

        // Gender validation
        if (genderSelect) {
            genderSelect.addEventListener('blur', () => {
                if (!genderSelect.value) {
                    showInlineError('gender', 'Please select a gender');
                } else {
                    clearInlineErrors();
                }
            });
        }

        // Form submission
        registerForm.addEventListener('submit', (e) => {
            e.preventDefault();

            // Prevent duplicate submissions
            if (isSubmitting) {
                console.warn('Form submission already in progress');
                return;
            }

            clearTimeout(submitDebounceTimer);
            clearInlineErrors();
            clearFormAlerts();

            // Client-side validation
            const fullName = fullNameInput?.value.trim() || '';
            const email = emailInput?.value.trim() || '';
            const phone = phoneInput?.value.trim() || '';
            const password = regPassword?.value || '';
            const confirmPassword = confirmPass?.value || '';
            const gender = genderSelect?.value || '';
            const address = addressInput?.value.trim() || '';

            let hasError = false;

            if (!fullName || fullName.length < 2) {
                showInlineError('fullName', 'Name must be at least 2 characters');
                hasError = true;
            }

            if (!email || !email.match(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)) {
                showInlineError('email', 'Please enter a valid email address');
                hasError = true;
            }

            if (!phone || !phone.match(/^[\d\s\-\+\(\)]+$/)) {
                showInlineError('phone', 'Please enter a valid phone number');
                hasError = true;
            }

            if (!password || password.length < 8) {
                showInlineError('password', 'Password must be at least 8 characters');
                hasError = true;
            }

            if (password !== confirmPassword) {
                showInlineError('confirmPassword', 'Passwords do not match');
                hasError = true;
            }

            if (!gender) {
                showInlineError('gender', 'Please select a gender');
                hasError = true;
            }

            if (!address || address.length < 5) {
                showInlineError('address', 'Address must be at least 5 characters');
                hasError = true;
            }

            if (hasError) return;

            // Set submitting state
            isSubmitting = true;
            const submitBtn = registerForm.querySelector('.auth-submit-btn');
            submitBtn.classList.add('auth-submit-btn--loading');
            submitBtn.disabled = true;

            // Prepare form data
            const formData = new URLSearchParams(new FormData(registerForm));
            const actionUrl = registerForm.getAttribute('action');

            const resetSubmitState = () => {
                isSubmitting = false;
                submitBtn.classList.remove('auth-submit-btn--loading');
                submitBtn.disabled = false;
            };

            // Convert FormData to plain object for API call
            const formDataObj = {};
            for (const [key, value] of formData.entries()) {
                formDataObj[key] = value;
            }
            
            FashionStoreAPI.post('/register', formDataObj)
            .then(data => {
                if (!data) return;

                if (data.success) {
                    showFormAlert(data.message || 'Registration successful! Redirecting to login...', 'success');

                    setTimeout(() => {
                        window.location.href = data.redirect || `${contextPath}/login?registered=true`;
                    }, 1500);
                } else {
                    showFormAlert(data.message || 'Registration failed.');
                    resetSubmitState();
                }
            })
            .catch(err => {
                console.error('Registration AJAX error:', err);
                showFormAlert(err.message || 'A secure connection failure occurred. Please try again.');
                resetSubmitState();
            });
        });

        registerForm.addEventListener('submit', () => {
            submitDebounceTimer = setTimeout(() => {
                isSubmitting = false;
            }, 1000);
        });
    }
});
