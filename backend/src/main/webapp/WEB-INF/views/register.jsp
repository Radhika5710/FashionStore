<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Register");
    request.setAttribute("_pageCSS", "auth");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body class="auth-page">

<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<main class="auth-main">
    <div class="auth-container">
        <div class="auth-card auth-card--register">
            <div class="auth-card__header">
                <span class="auth-card__tag">Create Account</span>
                <h1 class="auth-card__title">Join FashionStore</h1>
                <p class="auth-card__subtitle">Create an account to track orders, manage your cart, and checkout faster.</p>
            </div>

            <% if (request.getAttribute("error") != null) { %>
                <div class="auth-alert auth-alert--error" role="alert">
                    <span class="auth-alert__icon">⚠️</span>
                    <span class="auth-alert__message"><%= request.getAttribute("error") %></span>
                </div>
            <% } %>

            <form id="registerForm" class="auth-form" action="<%= request.getContextPath() %>/register" method="post" novalidate>
                <input type="hidden" name="csrfToken" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>" />
                
                <div class="auth-field">
                    <label for="fullName" class="auth-field__label">Full Name</label>
                    <input 
                        type="text" 
                        id="fullName" 
                        name="fullName" 
                        value="<%= request.getAttribute("fullName") != null ? request.getAttribute("fullName") : "" %>" 
                        autocomplete="name" 
                        placeholder="E.g., Alexander McQueen" 
                        required 
                        class="auth-field__input"
                    />
                    <span class="auth-field__error" id="fullName-error"></span>
                </div>

                <div class="auth-field">
                    <label for="email" class="auth-field__label">Email Address</label>
                    <input 
                        type="email" 
                        id="email" 
                        name="email" 
                        value="<%= request.getAttribute("email") != null ? request.getAttribute("email") : "" %>" 
                        autocomplete="email" 
                        placeholder="Enter your email" 
                        required 
                        class="auth-field__input"
                    />
                    <span class="auth-field__error" id="email-error"></span>
                </div>

                <div class="auth-field">
                    <label for="phone" class="auth-field__label">Phone Number</label>
                    <input 
                        type="tel" 
                        id="phone" 
                        name="phone" 
                        value="<%= request.getAttribute("phone") != null ? request.getAttribute("phone") : "" %>" 
                        autocomplete="tel" 
                        placeholder="E.g., +1 555-0199" 
                        required 
                        class="auth-field__input"
                    />
                    <span class="auth-field__error" id="phone-error"></span>
                </div>

                <div class="auth-field auth-field--password">
                    <label for="reg-password" class="auth-field__label">Password</label>
                    <div class="auth-password-wrapper">
                        <input 
                            type="password" 
                            id="reg-password" 
                            name="password" 
                            placeholder="Create password" 
                            autocomplete="new-password" 
                            required 
                            class="auth-field__input"
                        />
                        <button type="button" class="auth-toggle-password" aria-label="Show password" aria-pressed="false">
                            <svg class="auth-toggle-password__icon--show" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                            <svg class="auth-toggle-password__icon--hide auth-toggle-password__icon--hidden" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 0 5.06 5.94M9.9 4.24A9.12 9.12 0 0 1 12 5c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                <line x1="1" y1="1" x2="23" y2="23"></line>
                            </svg>
                        </button>
                    </div>
                    
                    <!-- Password Strength Meter -->
                    <div class="auth-strength-wrapper">
                        <div class="auth-strength-bar">
                            <div class="auth-strength-fill" id="password-strength-fill"></div>
                        </div>
                        <p class="auth-strength-text" id="password-strength-text">Strength: None</p>
                    </div>
                    
                    <!-- Password Requirements -->
                    <div class="auth-password-requirements">
                        <p class="auth-password-requirements__title">Password must contain:</p>
                        <ul class="auth-password-requirements__list">
                            <li class="auth-password-requirements__item" data-requirement="length">At least 6 characters</li>
                            <li class="auth-password-requirements__item" data-requirement="uppercase">One uppercase letter</li>
                            <li class="auth-password-requirements__item" data-requirement="lowercase">One lowercase letter</li>
                            <li class="auth-password-requirements__item" data-requirement="number">One number</li>
                            <li class="auth-password-requirements__item" data-requirement="special">One special character</li>
                        </ul>
                    </div>
                    
                    <span class="auth-field__error" id="password-error"></span>
                </div>

                <div class="auth-field auth-field--password">
                    <label for="confirmPassword" class="auth-field__label">Confirm Password</label>
                    <div class="auth-password-wrapper">
                        <input 
                            type="password" 
                            id="confirmPassword" 
                            name="confirmPassword" 
                            placeholder="Confirm password" 
                            autocomplete="new-password" 
                            required 
                            class="auth-field__input"
                        />
                        <button type="button" class="auth-toggle-password" aria-label="Show confirm password" aria-pressed="false">
                            <svg class="auth-toggle-password__icon--show" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                            <svg class="auth-toggle-password__icon--hide auth-toggle-password__icon--hidden" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 0 5.06 5.94M9.9 4.24A9.12 9.12 0 0 1 12 5c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                <line x1="1" y1="1" x2="23" y2="23"></line>
                            </svg>
                        </button>
                    </div>
                    <span class="auth-field__error" id="confirmPassword-error"></span>
                </div>

                <div class="auth-field">
                    <label for="gender" class="auth-field__label">Gender (Optional)</label>
                    <select id="gender" name="gender" class="auth-field__select">
                        <option value="">Select gender</option>
                        <option value="Male" <%= "Male".equals(request.getAttribute("gender")) ? "selected" : "" %>>Male</option>
                        <option value="Female" <%= "Female".equals(request.getAttribute("gender")) ? "selected" : "" %>>Female</option>
                        <option value="Other" <%= "Other".equals(request.getAttribute("gender")) ? "selected" : "" %>>Other</option>
                    </select>
                    <span class="auth-field__error" id="gender-error"></span>
                </div>

                <div class="auth-field">
                    <label for="address" class="auth-field__label">Shipping Address</label>
                    <input 
                        type="text" 
                        id="address" 
                        name="address" 
                        value="<%= request.getAttribute("address") != null ? request.getAttribute("address") : "" %>" 
                        autocomplete="street-address" 
                        placeholder="E.g., 123 Rue de Rivoli, Paris" 
                        required 
                        class="auth-field__input"
                    />
                    <span class="auth-field__error" id="address-error"></span>
                </div>

                <button type="submit" class="auth-submit-btn">
                    <span class="auth-submit-btn__text">Create Account</span>
                    <svg class="auth-submit-btn__spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10" stroke-opacity="0.25"></circle>
                        <path d="M12 2a10 10 0 0 1 10 10" stroke-dasharray="32" stroke-dashoffset="32">
                            <animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12 12" dur="1s" repeatCount="indefinite"/>
                        </path>
                    </svg>
                </button>
            </form>

            <div class="auth-card__footer">
                <p class="auth-card__footer-text">
                    Already have an account?
                    <a href="<%= request.getContextPath() %>/login" class="auth-link">Login</a>
                </p>
            </div>
        </div>
    </div>
</main>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

<script src="<%= request.getContextPath() %>/assets/js/auth.js" defer></script>

</body>
</html>
