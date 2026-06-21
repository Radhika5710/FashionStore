<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.fashionstore.model.User" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Account Settings");
    request.setAttribute("_pageCSS", "account");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body>

<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<%
    User user = (User) session.getAttribute("customerAuth");
%>

<main class="account-page">
    <div class="container">
        <div class="account-header">
            <h1>Account Settings</h1>
            <p class="account-greeting">Manage your account preferences</p>
        </div>

        <div class="account-layout">
            <jsp:include page="/WEB-INF/views/partials/account-sidebar.jsp" />

            <!-- Main Content -->
            <div class="account-content">
                <!-- Notification Settings -->
                <section class="account-section">
                    <div class="section-header">
                        <h2>Notification Preferences</h2>
                    </div>

                    <form action="<%= request.getContextPath() %>/account/profile" method="POST" class="fs-form-grid account-form">
                        <input type="hidden" name="action" value="updateSettings">
                        <input type="hidden" name="csrfToken" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>">

                        <div class="fs-form-group checkbox-group">
                            <label class="fs-form-checkbox">
                                <input type="checkbox" name="emailNotifications" <%= Boolean.TRUE.equals(request.getAttribute("emailNotifications")) ? "checked" : "" %>>
                                <span class="checkbox-text">
                                    <span class="checkbox-title">Email Notifications</span>
                                    <span class="checkbox-hint">Receive order updates and promotional emails</span>
                                </span>
                            </label>
                        </div>

                        <div class="fs-form-group checkbox-group">
                            <label class="fs-form-checkbox">
                                <input type="checkbox" name="smsNotifications" <%= Boolean.TRUE.equals(request.getAttribute("smsNotifications")) ? "checked" : "" %>>
                                <span class="checkbox-text">
                                    <span class="checkbox-title">SMS Notifications</span>
                                    <span class="checkbox-hint">Receive text messages for important updates</span>
                                </span>
                            </label>
                        </div>

                        <div class="fs-form-group checkbox-group">
                            <label class="fs-form-checkbox">
                                <input type="checkbox" name="orderUpdates" <%= Boolean.TRUE.equals(request.getAttribute("orderUpdates")) ? "checked" : "" %>>
                                <span class="checkbox-text">
                                    <span class="checkbox-title">Order Updates</span>
                                    <span class="checkbox-hint">Get notified about your order status</span>
                                </span>
                            </label>
                        </div>

                        <div class="fs-form-group checkbox-group">
                            <label class="fs-form-checkbox">
                                <input type="checkbox" name="promotionalEmails" <%= Boolean.TRUE.equals(request.getAttribute("promotionalEmails")) ? "checked" : "" %>>
                                <span class="checkbox-text">
                                    <span class="checkbox-title">Promotional Emails</span>
                                    <span class="checkbox-hint">Receive exclusive offers and discounts</span>
                                </span>
                            </label>
                        </div>

                        <div class="fs-form-group checkbox-group">
                            <label class="fs-form-checkbox">
                                <input type="checkbox" name="newsletterSubscription" <%= Boolean.TRUE.equals(request.getAttribute("newsletterSubscription")) ? "checked" : "" %>>
                                <span class="checkbox-text">
                                    <span class="checkbox-title">Newsletter</span>
                                    <span class="checkbox-hint">Subscribe to our weekly newsletter</span>
                                </span>
                            </label>
                        </div>

                        <div class="fs-form-actions fs-form-actions--full-width">
                            <button type="submit" class="fs-btn fs-btn--primary">Save Preferences</button>
                        </div>
                    </form>
                </section>

                <!-- Security Settings -->
                <section class="account-section">
                    <div class="section-header">
                        <h2>Security</h2>
                    </div>

                    <% if (request.getAttribute("error") != null) { %>
                        <div class="alert alert-error">
                            <%= request.getAttribute("error") %>
                        </div>
                    <% } %>

                    <% if (request.getAttribute("success") != null) { %>
                        <div class="alert alert-success">
                            <%= request.getAttribute("success") %>
                        </div>
                    <% } %>

                    <form action="<%= request.getContextPath() %>/account/profile" method="POST" class="fs-form-grid account-form">
                        <input type="hidden" name="action" value="changePassword">
                        <input type="hidden" name="csrfToken" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>">

                        <div class="fs-form-group">
                            <label for="currentPassword">Current Password *</label>
                            <div class="fs-password-field">
                                <input type="password" id="currentPassword" name="currentPassword" required class="fs-form-input">
                                <button type="button" class="fs-password-field__toggle" aria-label="Show current password" aria-pressed="false">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                            </div>
                        </div>

                        <div class="fs-form-group">
                            <label for="newPassword">New Password *</label>
                            <div class="fs-password-field">
                                <input type="password" id="newPassword" name="newPassword" required
                                       minlength="8"
                                       maxlength="50"
                                       class="fs-form-input">
                                <button type="button" class="fs-password-field__toggle" aria-label="Show new password" aria-pressed="false">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                            </div>
                            <span class="form-hint">Minimum 8 characters</span>
                        </div>

                        <div class="fs-form-group">
                            <label for="confirmPassword">Confirm New Password *</label>
                            <div class="fs-password-field">
                                <input type="password" id="confirmPassword" name="confirmPassword" required
                                       minlength="8"
                                       maxlength="50"
                                       class="fs-form-input">
                                <button type="button" class="fs-password-field__toggle" aria-label="Show confirm password" aria-pressed="false">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                </button>
                            </div>
                        </div>

                        <div class="fs-form-actions fs-form-actions--full-width">
                            <button type="submit" class="fs-btn fs-btn--primary">Change Password</button>
                        </div>
                    </form>
                </section>

                <!-- Display Settings -->
                <section class="account-section">
                    <div class="section-header">
                        <h2>Display Preferences</h2>
                    </div>

                    <form action="<%= request.getContextPath() %>/account/profile" method="POST" class="fs-form-grid account-form">
                        <input type="hidden" name="action" value="updateSettings">
                        <input type="hidden" name="csrfToken" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>">

                        <div class="fs-form-group">
                            <label for="language">Language</label>
                            <select id="language" name="language" class="fs-form-select">
                                <option value="en" <%= "en".equals(request.getAttribute("language")) ? "selected" : "" %>>English</option>
                                <option value="hi" <%= "hi".equals(request.getAttribute("language")) ? "selected" : "" %>>Hindi</option>
                                <option value="es" <%= "es".equals(request.getAttribute("language")) ? "selected" : "" %>>Spanish</option>
                            </select>
                        </div>

                        <div class="fs-form-group">
                            <label for="currency">Currency</label>
                            <select id="currency" name="currency" class="fs-form-select">
                                <option value="INR" <%= "INR".equals(request.getAttribute("currency")) ? "selected" : "" %>>₹ Indian Rupee (INR)</option>
                                <option value="USD" <%= "USD".equals(request.getAttribute("currency")) ? "selected" : "" %>>$ US Dollar (USD)</option>
                                <option value="EUR" <%= "EUR".equals(request.getAttribute("currency")) ? "selected" : "" %>>€ Euro (EUR)</option>
                                <option value="GBP" <%= "GBP".equals(request.getAttribute("currency")) ? "selected" : "" %>>£ British Pound (GBP)</option>
                            </select>
                        </div>

                        <div class="fs-form-group">
                            <label for="theme">Theme</label>
                            <select id="theme" name="themePreference" class="fs-form-select">
                                <option value="auto" <%= "auto".equals(request.getAttribute("themePreference")) ? "selected" : "" %>>Auto (System)</option>
                                <option value="light" <%= "light".equals(request.getAttribute("themePreference")) ? "selected" : "" %>>Light</option>
                                <option value="dark" <%= "dark".equals(request.getAttribute("themePreference")) ? "selected" : "" %>>Dark</option>
                            </select>
                        </div>

                        <div class="fs-form-actions fs-form-actions--full-width">
                            <button type="submit" class="fs-btn fs-btn--primary">Save Preferences</button>
                        </div>
                    </form>
                </section>

                <!-- Privacy Controls -->
                <section class="account-section">
                    <div class="section-header">
                        <h2>Privacy</h2>
                    </div>

                    <form action="<%= request.getContextPath() %>/account/profile" method="POST" class="fs-form-grid account-form">
                        <input type="hidden" name="action" value="updateSettings">
                        <input type="hidden" name="csrfToken" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>">

                        <div class="fs-form-group checkbox-group">
                            <label class="fs-form-checkbox">
                                <input type="checkbox" name="profileVisible" <%= Boolean.TRUE.equals(request.getAttribute("profileVisible")) ? "checked" : "" %>>
                                <span class="checkbox-text">
                                    <span class="checkbox-title">Public Profile</span>
                                    <span class="checkbox-hint">Allow your profile to be visible to other users</span>
                                </span>
                            </label>
                        </div>

                        <div class="fs-form-group checkbox-group">
                            <label class="fs-form-checkbox">
                                <input type="checkbox" name="activityTracking" <%= Boolean.TRUE.equals(request.getAttribute("activityTracking")) ? "checked" : "" %>>
                                <span class="checkbox-text">
                                    <span class="checkbox-title">Activity Tracking</span>
                                    <span class="checkbox-hint">Allow us to personalize your experience based on browsing activity</span>
                                </span>
                            </label>
                        </div>

                        <div class="fs-form-group checkbox-group">
                            <label class="fs-form-checkbox">
                                <input type="checkbox" name="thirdPartySharing" <%= Boolean.TRUE.equals(request.getAttribute("thirdPartySharing")) ? "checked" : "" %>>
                                <span class="checkbox-text">
                                    <span class="checkbox-title">Third-Party Data Sharing</span>
                                    <span class="checkbox-hint">Share anonymized data with trusted partners for analytics</span>
                                </span>
                            </label>
                        </div>

                        <div class="fs-form-actions fs-form-actions--full-width">
                            <button type="submit" class="fs-btn fs-btn--primary">Save Privacy Settings</button>
                        </div>
                    </form>
                </section>

                <!-- Account Actions -->
                <section class="account-section account-danger-zone">
                    <div class="section-header">
                        <h2>Danger Zone</h2>
                    </div>

                    <div class="danger-actions">
                        <div class="danger-action">
                            <div class="danger-action-info">
                                <h3>Deactivate Account</h3>
                                <p>Temporarily deactivate your account. You can reactivate it later.</p>
                            </div>
                            <button class="fs-btn fs-btn--outline" type="button" id="deactivate-account-btn">Deactivate</button>
                        </div>

                        <div class="danger-action">
                            <div class="danger-action-info">
                                <h3>Delete Account</h3>
                                <p>Permanently delete your account and all associated data. This action cannot be undone.</p>
                            </div>
                            <button class="fs-btn fs-btn--danger" type="button" id="delete-account-btn">Delete Account</button>
                        </div>
                    </div>
                </section>
            </div>
        </div>
    </div>
</main>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
