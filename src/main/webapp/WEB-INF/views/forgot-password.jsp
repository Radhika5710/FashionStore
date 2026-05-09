<%@ page contentType="text/html;charset=UTF-8" %>
<%
    request.setAttribute("_pageTitle", "Forgot Password");
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
                <h1>Forgot Password</h1>
                <p>Enter your email address and we'll send you a link to reset your password.</p>
            </div>

            <% if (request.getAttribute("error") != null) { %>
                <div class="alert alert-error" role="alert" aria-live="assertive">
                    <%= request.getAttribute("error") %>
                </div>
            <% } %>

            <% if (request.getAttribute("success") != null) { %>
                <div class="alert alert-success" role="alert" aria-live="polite">
                    <%= request.getAttribute("success") %>
                </div>
                <% if (request.getAttribute("resetLink") != null) { %>
                    <div class="alert alert-info" role="alert" aria-live="polite">
                        <strong>Development Mode - Reset Link:</strong><br>
                        <a href="<%= request.getAttribute("resetLink") %>" target="_blank" rel="noopener noreferrer"><%= request.getAttribute("resetLink") %></a>
                    </div>
                <% } %>
            <% } %>

            <form class="auth-form" action="<%= request.getContextPath() %>/forgot-password" method="post" novalidate>
                <div class="form-group">
                    <label for="email" class="form-label">Email Address</label>
                    <input type="email" id="email" name="email" required 
                           class="form-control"
                           placeholder="Enter your email address"
                           autocomplete="email"
                           aria-describedby="email-help"
                           aria-required="true">
                    <span id="email-help" class="form-help">We'll send a password reset link to this email</span>
                </div>

                <button type="submit" class="btn btn-primary btn-block" aria-describedby="submit-help">
                    Send Reset Link
                </button>
                <span id="submit-help" class="sr-only">Sends password reset link to your email address</span>
            </form>

            <div class="auth-footer">
                <p>Remember your password? <a href="<%= request.getContextPath() %>/login" aria-label="Go to sign in page">Sign in</a></p>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
