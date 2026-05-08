<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.fashionstore.model.Order" %>
<%@ page import="com.fashionstore.model.OrderItem" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "My Orders");
    request.setAttribute("_pageCSS", "orders");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body>

<!-- NAVBAR -->
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<%
    List<Order> orders = new ArrayList<>();

    Object obj = request.getAttribute("orders");

    if (obj != null && obj instanceof List<?>) {
        @SuppressWarnings("unchecked")
        List<Order> temp = (List<Order>) obj;
        orders = temp;
    }
%>

<main class="orders-wrap">
    <div class="orders-head">
        <h1>My Orders</h1>
        <p>Track your latest purchases and delivery progress.</p>
    </div>

    <% if (orders != null && !orders.isEmpty()) { %>

        <% for (Order order : orders) { %>

            <article class="order-card">
                <div class="order-top">
                    <h3>Order #<%= order.getOrderId() %></h3>
                <%
                    String status = (order.getStatus() == null || order.getStatus().isBlank()) ? "Pending" : order.getStatus();
                    String badgeClass = "badge-pending";
                    if ("Shipped".equalsIgnoreCase(status)) badgeClass = "badge-shipped";
                    else if ("Delivered".equalsIgnoreCase(status)) badgeClass = "badge-delivered";
                    else if ("Cancelled".equalsIgnoreCase(status)) badgeClass = "badge-cancelled";
                %>
                    <span class="status-badge <%= badgeClass %>"><%= status %></span>
                </div>

                <p class="order-meta"><strong>Total:</strong> ₹<%= String.format("%.2f", order.getTotalAmount()) %></p>

                <h4>Items:</h4>

                <%
                    List<OrderItem> items = order.getItems();
                %>

                <% if (items != null && !items.isEmpty()) { %>
                    <div class="items-grid">

                    <% for (OrderItem item : items) { %>

                        <div class="item-box">
                            <p><strong>Product ID:</strong> <%= item.getProductId() %></p>
                            <p><strong>Quantity:</strong> <%= item.getQuantity() %></p>
                            <p><strong>Price:</strong> ₹<%= String.format("%.2f", item.getPrice()) %></p>
                        </div>

                    <% } %>
                    </div>

                <% } else { %>

                    <p>No items found for this order</p>

                <% } %>

            </article>

        <% } %>

    <% } else { %>

        <div class="orders-empty">
            <p>No orders found yet.</p>
            <a href="<%= request.getContextPath() %>/products" class="btn btn-primary">Start Shopping</a>
        </div>

    <% } %>

</main>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>