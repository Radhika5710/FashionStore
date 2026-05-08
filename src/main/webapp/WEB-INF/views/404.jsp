<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "404 – Page Not Found");
    request.setAttribute("_pageCSS", "");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>
<body>
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<div class="error-page">
    <div class="error-code">404</div>
    <h1 class="error-title">Page Not Found</h1>
    <p class="error-subtitle">The page you're looking for doesn't exist or has been moved. Let's get you back on track.</p>
    <a href="<%= request.getContextPath() %>/home" class="back-home">
        ← Back to Home
    </a>
</div>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />
</body>
</html>
