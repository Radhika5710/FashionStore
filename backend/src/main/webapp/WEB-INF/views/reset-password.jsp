<%@ page contentType="text/html;charset=UTF-8" %>
<%
    request.setAttribute("_pageTitle", "Reset Password");
    request.setAttribute("_pageCSS", "auth");
%>
<!DOCTYPE html>
<html lang="en">
<head>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
<script src="<%= request.getContextPath() %>/assets/js/auth.js" defer></script>
</head>

<body class="auth-page">

<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<main class="auth-main">
    <div class="auth-container">
        <div class="auth-card">
            <div class="auth-card__header">
                <span class="auth-card__tag">Reset Password</span>
                <h1 class="auth-card__title">Create New Password</h1>
                <p class="auth-card__subtitle">Enter your new password below.</p>
            </div>

            <% if (request.getAttribute("error") != null) { %>
                <div class="auth-alert auth-alert--error" role="alert">
                    <span class="auth-alert__icon">⚠️</span>
                    <span class="auth-alert__message"><%= request.getAttribute("error") %></span>
                </div>
            <% } %>

            <form id="resetPasswordForm" class="auth-form" action="<%= request.getContextPath() %>/reset-password?token=<%= request.getAttribute("token") %>" method="post" novalidate>
                <input type="hidden" name="token" value="<%= request.getAttribute("token") %>">
                <% if (request.getAttribute("csrfToken") != null) { %>
                <input type="hidden" name="csrfToken" value="<%= request.getAttribute("csrfToken") %>">
                <% } %>

                <div class="auth-field auth-field--password">
                    <label for="password" class="auth-field__label">New Password</label>
                    <div class="auth-password-wrapper">
                        <input 
                            type="password" 
                            id="password" 
                            name="password" 
                            required 
                            class="auth-field__input"
                            placeholder="Enter new password (minimum 8 characters)"
                            minlength="8"
                            autocomplete="new-password"
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
                    <span class="text-sm text-secondary" style="font-size: var(--text-xs); color: var(--color-text-tertiary); margin-top: var(--space-1);">Create a strong password for your account</span>
                    <span class="auth-field__error" id="password-error"></span>
                </div>

                <div class="auth-field auth-field--password">
                    <label for="confirmPassword" class="auth-field__label">Confirm Password</label>
                    <div class="auth-password-wrapper">
                        <input 
                            type="password" 
                            id="confirmPassword" 
                            name="confirmPassword" 
                            required 
                            class="auth-field__input"
                            placeholder="Confirm new password"
                            minlength="8"
                            autocomplete="new-password"
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
                    <span class="text-sm text-secondary" style="font-size: var(--text-xs); color: var(--color-text-tertiary); margin-top: var(--space-1);">Re-enter your new password to confirm</span>
                    <span class="auth-field__error" id="confirmPassword-error"></span>
                </div>

                <div class="auth-password-requirements">
                    <p class="auth-password-requirements__title">Password must:</p>
                    <ul class="auth-password-requirements__list">
                        <li class="auth-password-requirements__item" data-requirement="length">Be at least 8 characters long</li>
                        <li class="auth-password-requirements__item" data-requirement="uppercase">Contain at least one uppercase letter</li>
                        <li class="auth-password-requirements__item" data-requirement="lowercase">Contain at least one lowercase letter</li>
                        <li class="auth-password-requirements__item" data-requirement="number">Contain at least one number</li>
                    </ul>
                </div>

                <button type="submit" class="auth-submit-btn">
                    <span class="auth-submit-btn__text">Reset Password</span>
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
                    Remember your password?
                    <a href="<%= request.getContextPath() %>/login" class="auth-link">Sign in</a>
                </p>
            </div>
        </div>
    </div>
</main>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

<script src="<%= request.getContextPath() %>/assets/js/auth.js" defer></script>

</body>
</html>
