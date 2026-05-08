<%@ page contentType="text/html;charset=UTF-8" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Login");
    request.setAttribute("_pageCSS", "auth");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body>

<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<main class="auth-page">
    <section class="auth-card">
        <h1>Welcome back</h1>
        <p>Login to continue shopping your favorite fashion picks.</p>

        <% if (request.getAttribute("error") != null) { %>
            <p class="auth-error">
                <%= request.getAttribute("error") %>
            </p>
        <% } %>

        <form action="<%= request.getContextPath() %>/login" method="post" class="auth-form">
            <input type="hidden" name="csrf_token" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>" />
            <label for="email">Email</label>
            <input type="email" id="email" name="email" placeholder="you@example.com" autocomplete="email" required>

            <label for="password">Password</label>
            <input type="password" id="password" name="password" placeholder="Enter your password" autocomplete="current-password" required>

            <button type="submit" class="btn btn-primary">Login</button>
        </form>

        <p class="auth-links">
            New to FashionStore?
            <a href="<%= request.getContextPath() %>/register">Create account</a>
        </p>
    </section>
</main>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>