<%@ page contentType="text/html;charset=UTF-8" %>
<%
    request.setAttribute("_pageTitle", "Reset Password");
    request.setAttribute("_pageCSS", "auth");
%>
<!DOCTYPE html>
<html lang="en">
<head>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>
<body>

<div class="auth-page">
    <div class="auth-container">
        <div class="auth-card">
            <div class="auth-header">
                <h1>Reset Password</h1>
                <p>Enter your new password below.</p>
            </div>

            <% if (request.getAttribute("error") != null) { %>
                <div class="alert alert-error" role="alert" aria-live="assertive">
                    <%= request.getAttribute("error") %>
                </div>
            <% } %>

            <form class="auth-form" action="<%= request.getContextPath() %>/reset-password?token=<%= request.getAttribute("token") %>" method="post" novalidate>
                <input type="hidden" name="token" value="<%= request.getAttribute("token") %>">

                <div class="form-group">
                    <label for="password" class="form-label">New Password</label>
                    <input type="password" id="password" name="password" required 
                           class="form-control"
                           placeholder="Enter new password (minimum 8 characters)"
                           minlength="8"
                           autocomplete="new-password"
                           aria-describedby="password-help password-requirements"
                           aria-required="true">
                    <span id="password-help" class="form-help">Create a strong password for your account</span>
                </div>

                <div class="form-group">
                    <label for="confirmPassword" class="form-label">Confirm Password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required 
                           class="form-control"
                           placeholder="Confirm new password"
                           minlength="8"
                           autocomplete="new-password"
                           aria-describedby="confirm-help"
                           aria-required="true">
                    <span id="confirm-help" class="form-help">Re-enter your new password to confirm</span>
                </div>

                <div class="password-requirements" id="password-requirements" role="region" aria-label="Password requirements">
                    <p>Password must:</p>
                    <ul>
                        <li>Be at least 8 characters long</li>
                        <li>Contain at least one uppercase letter</li>
                        <li>Contain at least one lowercase letter</li>
                        <li>Contain at least one number</li>
                    </ul>
                </div>

                <button type="submit" class="btn btn-primary btn-block" aria-describedby="submit-help">
                    Reset Password
                </button>
                <span id="submit-help" class="sr-only">Updates your account password</span>
            </form>

            <div class="auth-footer">
                <p>Remember your password? <a href="<%= request.getContextPath() %>/login" aria-label="Go to sign in page">Sign in</a></p>
            </div>
        </div>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');
    const form = document.querySelector('.auth-form');

    form.addEventListener('submit', function(e) {
        if (password.value !== confirmPassword.value) {
            e.preventDefault();
            alert('Passwords do not match');
            password.focus();
            return false;
        }

        if (password.value.length < 8) {
            e.preventDefault();
            alert('Password must be at least 8 characters long');
            password.focus();
            return false;
        }

        const hasUpperCase = /[A-Z]/.test(password.value);
        const hasLowerCase = /[a-z]/.test(password.value);
        const hasNumber = /[0-9]/.test(password.value);

        if (!hasUpperCase || !hasLowerCase || !hasNumber) {
            e.preventDefault();
            alert('Password must contain at least one uppercase letter, one lowercase letter, and one number');
            password.focus();
            return false;
        }
    });
});
</script>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
