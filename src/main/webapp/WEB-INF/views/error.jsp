<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "System Error");
    request.setAttribute("_pageCSS", "");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>
<body>
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<div class="error-page">
    <svg class="error-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
        <line x1="12" y1="9" x2="12" y2="13"/>
        <line x1="12" y1="17" x2="12.01" y2="17"/>
    </svg>
    <h1 class="error-title">Something Went Wrong</h1>
    <p class="error-subtitle">We're sorry — an unexpected error occurred. Our team has been notified. Please try again shortly.</p>
    <a href="<%= request.getContextPath() %>/home" class="back-home">← Back to Home</a>
</div>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />
</body>
</html>
