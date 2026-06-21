<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <jsp:include page="/WEB-INF/views/partials/head.jsp" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>500 - Server Error | FashionStore</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/design-tokens.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/reset.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/base.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
</head>
<body class="min-h-screen bg-gray-50 flex items-center justify-center px-4">
    <div class="max-w-md w-full bg-white rounded-lg shadow-lg p-6 text-center">
        <!-- Error Icon -->
        <div class="mx-auto w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mb-4">
            <svg class="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.432-3.054L16.632 8.432c.772-1.387 2.476-1.387 3.248 0l3.096 5.414c.772 1.387-.892 3.054-2.432 3.054H4.432c-1.54 0-2.502-1.667-1.432-3.054L7.168 8.432c-.772-1.387.892-3.054 2.432-3.054z"/>
            </svg>
        </div>

        <!-- Error Message -->
        <h1 class="text-2xl font-bold text-gray-900 mb-2">System Error</h1>
        <p class="text-gray-600 mb-6">
            We're sorry, but something went wrong on our end. Our team has been notified and is working to fix this issue.
        </p>

        <!-- Error Details (for debugging) -->
        <c:if test="${error != null}">
            <div class="bg-gray-50 rounded-lg p-4 mb-6 text-left">
                <p class="text-sm font-medium text-gray-700 mb-2">Error Details:</p>
                <div class="text-xs text-gray-600 space-y-1">
                    <p><strong>Error Code:</strong> ${error.error}</p>
                    <p><strong>Type:</strong> ${error.type}</p>
                    <p><strong>Time:</strong> <span class="timestamp">${error.timestamp}</span></p>
                    <c:if test="${error.path != null}">
                        <p><strong>Path:</strong> ${error.path}</p>
                    </c:if>
                </div>
            </div>
        </c:if>

        <!-- Action Buttons -->
        <div class="space-y-3">
            <a href="${pageContext.request.contextPath}/" 
               class="w-full bg-primary text-white py-3 px-4 rounded-lg font-medium hover:bg-primary-dark transition-colors">
                Go to Homepage
            </a>
            <button type="button" class="fs-btn fs-btn--outline" id="back-btn">
                Go Back
            </button>
        </div>

        <!-- Support Information -->
        <div class="mt-6 pt-6 border-t border-gray-200">
            <p class="text-sm text-gray-500">
                If this problem persists, please contact our support team:
            </p>
            <div class="mt-2 space-y-1">
                <p class="text-sm text-gray-600">
                    <strong>Email:</strong> support@fashionstore.com
                </p>
                <p class="text-sm text-gray-600">
                    <strong>Phone:</strong> +1 (800) 123-4567
                </p>
            </div>
        </div>
    </div>
</body>
</html>
