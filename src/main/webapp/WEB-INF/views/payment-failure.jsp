<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.fashionstore.model.Order" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Payment Failed");
    request.setAttribute("_pageCSS", "success");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body>

<!-- NAVBAR -->
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<%
    Order order = (Order) request.getAttribute("order");
%>

<main class="error-page">
    <div class="container">
        <div class="error-container">
            <div class="error-icon">
                <svg width="80" height="80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
            </div>
            
            <h1 class="error-title">Payment Failed</h1>
            <p class="error-message">We couldn't process your payment. Please try again or choose a different payment method.</p>
            
            <% if (order != null) { %>
            <div class="order-summary">
                <h2 class="display-3">Order Details</h2>
                <div class="order-info">
                    <div class="info-item">
                        <span class="info-label">Order ID:</span>
                        <span class="info-value">#<%= order.getOrderId() %></span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">Payment Method:</span>
                        <span class="info-value"><%= order.getPaymentMethod() %></span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">Total Amount:</span>
                        <span class="info-value">₹<%= String.format("%.2f", order.getTotalAmount()) %></span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">Status:</span>
                        <span class="info-value status-failed">Payment Failed</span>
                    </div>
                </div>
            </div>
            <% } %>
            
            <div class="error-actions">
                <a href="<%= request.getContextPath() %>/cart" class="btn btn-primary">Try Again</a>
                <a href="<%= request.getContextPath() %>/products" class="btn btn-outline">Continue Shopping</a>
            </div>
        </div>
    </div>
</main>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
