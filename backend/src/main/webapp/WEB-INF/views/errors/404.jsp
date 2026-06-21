<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 - Page Not Found | FashionStore</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/design-tokens.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/reset.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/base.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components/buttons.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="min-h-screen bg-gray-50 flex items-center justify-center px-4">
    <main class="site-main error-page premium-fade">
        <div class="error-container premium-reveal">
            <div class="mx-auto w-16 h-16 bg-yellow-100 rounded-full flex items-center justify-center mb-4">
                <svg class="w-8 h-8 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 12h6m-6-4h.01M9 16h.01"/>
                </svg>
            </div>
            <div class="error-code">404</div>
            <span class="accent-tag accent-tag--margin-bottom">Page Not Found</span>
            <h1 class="error-title editorial-title">Lost in the collection</h1>
            <p class="error-subtitle">The page you're looking for doesn't exist or has been moved. Let's get you back on track.</p>
            <c:if test="${error != null}">
                <div class="bg-gray-50 rounded-lg p-4 mb-6 text-left">
                    <p class="text-sm font-medium text-gray-700 mb-2">Error Details:</p>
                    <div class="text-xs text-gray-600 space-y-1">
                        <p><strong>Error Code:</strong> ${error.error}</p>
                        <p><strong>Type:</strong> ${error.type}</p>
                        <p><strong>Time:</strong> <span class="timestamp">${error.timestamp}</span></p>
                        <c:if test="${error.path != null}">
                            <p><strong>Requested Path:</strong> ${error.path}</p>
                        </c:if>
                    </div>
                        </div>
                </div>
            </c:if>

            <!-- Action Buttons -->
            <div class="error-actions">
                <a href="<%= request.getContextPath() %>/home" class="btn btn-primary btn-lg">Back to Home</a>
                <a href="<%= request.getContextPath() %>/products" class="btn btn-outline btn-lg">Browse Products</a>
            </div>

            <!-- Popular Links -->
            <div class="error-links">
                <p class="error-links-title">Quick Navigation:</p>
                <div class="error-links-grid">
                    <a href="<%= request.getContextPath() %>/products" class="error-link">Products</a>
                    <a href="<%= request.getContextPath() %>/cart" class="error-link">Cart</a>
                    <a href="<%= request.getContextPath() %>/account" class="error-link">Account</a>
                    <a href="<%= request.getContextPath() %>/home" class="error-link">Home</a>
                </div>
            </div>
        </div>
    </main>
</body>
</html>
