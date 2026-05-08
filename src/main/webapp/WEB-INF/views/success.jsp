<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Order Confirmed");
    request.setAttribute("_pageCSS", "success");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>
<body>
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<div class="success-page">
    <div class="success-container">
        <!-- Animated Success Icon -->
        <div class="success-icon-wrapper">
            <svg class="success-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path class="check-path" d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline class="check-mark" points="22 4 12 14.01 9 11.01"/>
            </svg>
        </div>
        
        <h1 class="success-title">Order Placed Successfully!</h1>
        <p class="success-subtitle">Thank you for your purchase. Your order has been confirmed and will be dispatched shortly.</p>
        
        <div class="success-details">
            <div class="success-detail-item">
                <svg class="detail-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                    <line x1="12" y1="22.08" x2="12" y2="12"/>
                </svg>
                <span>Order confirmed</span>
            </div>
            <div class="success-detail-item">
                <svg class="detail-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="1" y="3" width="15" height="13"/>
                    <polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/>
                    <circle cx="5.5" cy="18.5" r="2.5"/>
                    <circle cx="18.5" cy="18.5" r="2.5"/>
                </svg>
                <span>Processing for shipment</span>
            </div>
            <div class="success-detail-item">
                <svg class="detail-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
                <span>Secure payment processed</span>
            </div>
        </div>
        
        <div class="success-actions">
            <a href="<%= request.getContextPath() %>/orders" class="btn btn-primary">View My Orders</a>
            <a href="<%= request.getContextPath() %>/products" class="btn btn-secondary">Continue Shopping</a>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

<script>
// Animate success icon on page load
document.addEventListener('DOMContentLoaded', function() {
    const successIcon = document.querySelector('.success-icon');
    if (successIcon) {
        successIcon.style.animation = 'successPulse 0.6s ease-out';
    }
});
</script>
</body>
</html>
