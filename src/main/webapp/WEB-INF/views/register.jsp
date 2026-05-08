<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Create Account");
    request.setAttribute("_pageCSS", "auth");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>
<body>

<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<main class="auth-page">
    <section class="auth-card">
        <h1>Create your account</h1>
        <p>Join FashionStore to track orders, manage your cart, and checkout faster.</p>

        <% String error = (String) request.getAttribute("error"); %>
        <% if (error != null) { %>
            <p class="auth-error"><%= error %></p>
        <% } %>

        <form action="<%= request.getContextPath() %>/register" method="post" class="auth-form">
            <input type="hidden" name="csrf_token" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>">
            <label for="fullName">Full Name</label>
            <input type="text" id="fullName" name="fullName" value="<%= request.getAttribute("fullName") != null ? request.getAttribute("fullName") : "" %>" autocomplete="name" required>

            <label for="reg-email">Email</label>
            <input type="email" id="reg-email" name="email" value="<%= request.getAttribute("email") != null ? request.getAttribute("email") : "" %>" autocomplete="email" required>

            <label for="phone">Phone</label>
            <input type="tel" id="phone" name="phone" value="<%= request.getAttribute("phone") != null ? request.getAttribute("phone") : "" %>" autocomplete="tel" required>

            <label for="reg-password">Password</label>
            <input type="password" id="reg-password" name="password" placeholder="Create password" autocomplete="new-password" required>

            <label for="confirmPassword">Confirm Password</label>
            <input type="password" id="confirmPassword" name="confirmPassword" placeholder="Confirm password" autocomplete="new-password" required>

            <label for="gender">Gender</label>
            <select id="gender" name="gender" required>
                <option value="">Select</option>
                <option value="Male" <%= "Male".equals(request.getAttribute("gender")) ? "selected" : "" %>>Male</option>
                <option value="Female" <%= "Female".equals(request.getAttribute("gender")) ? "selected" : "" %>>Female</option>
                <option value="Other" <%= "Other".equals(request.getAttribute("gender")) ? "selected" : "" %>>Other</option>
            </select>

            <label for="address">Address</label>
            <input type="text" id="address" name="address" value="<%= request.getAttribute("address") != null ? request.getAttribute("address") : "" %>" autocomplete="street-address" required>

            <button type="submit" class="btn btn-primary">Create Account</button>
        </form>

        <p class="auth-links">
            Already have an account?
            <a href="<%= request.getContextPath() %>/login">Login</a>
        </p>
    </section>
</main>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
