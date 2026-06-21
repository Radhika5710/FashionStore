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

<main class="shell section-block" id="main-content">
    <div class="fs-orders-header">
        <span class="fs-orders-header__tag">Your Account</span>
        <h1 class="editorial-heading">My Orders</h1>
        <p class="text-secondary">Track your latest purchases and delivery progress in real-time.</p>
    </div>

    <% if (orders != null && !orders.isEmpty()) { %>

        <% for (Order order : orders) { %>

            <article class="fs-order-card">
                <div class="fs-order-card__header">
                    <div class="fs-order-card__info">
                        <h3>Order #<%= order.getOrderId() %></h3>
                        <p class="text-secondary">Placed on <%= order.getOrderDate() != null ? new java.text.SimpleDateFormat("MMM dd, yyyy").format(order.getOrderDate()) : "Recently" %></p>
                    </div>
                <%
                    String status = (order.getStatus() == null || order.getStatus().isBlank()) ? "Pending" : order.getStatus();
                    String badgeClass = "fs-badge--pending";
                    String statusIcon = "⏳";
                    if ("Shipped".equalsIgnoreCase(status)) {
                        badgeClass = "fs-badge--shipped";
                        statusIcon = "🚚";
                    } else if ("Delivered".equalsIgnoreCase(status)) {
                        badgeClass = "fs-badge--delivered";
                        statusIcon = "✓";
                    } else if ("Cancelled".equalsIgnoreCase(status)) {
                        badgeClass = "fs-badge--cancelled";
                        statusIcon = "✕";
                    } else if ("Packed".equalsIgnoreCase(status)) {
                        badgeClass = "fs-badge--packed";
                        statusIcon = "📦";
                    }
                %>
                    <div class="fs-order-card__status">
                        <span class="fs-badge <%= badgeClass %>">
                            <span><%= statusIcon %></span>
                            <%= status %>
                        </span>
                    </div>
                </div>

                <div class="fs-order-card__summary">
                    <div class="fs-order-card__total">
                        <span class="text-secondary">Total Amount</span>
                        <span>₹<%= String.format("%.2f", order.getTotalAmount()) %></span>
                    </div>
                    <div class="fs-order-card__count">
                        <span class="text-secondary"><%= order.getItems() != null ? order.getItems().size() : 0 %> items</span>
                    </div>
                </div>

                <div class="fs-order-card__timeline" aria-label="Order tracking timeline">
                    <div class="fs-timeline-step fs-timeline-step--done">
                        <div class="fs-timeline-step__marker"></div>
                        <span class="fs-timeline-step__label">Processing</span>
                    </div>
                    <div class="fs-timeline-connector <%= "Packed".equalsIgnoreCase(status) || "Shipped".equalsIgnoreCase(status) || "Delivered".equalsIgnoreCase(status) ? "fs-timeline-connector--done" : "" %>"></div>
                    <div class="fs-timeline-step <%= "Packed".equalsIgnoreCase(status) || "Shipped".equalsIgnoreCase(status) || "Delivered".equalsIgnoreCase(status) ? "fs-timeline-step--done" : "" %>">
                        <div class="fs-timeline-step__marker"></div>
                        <span class="fs-timeline-step__label">Packed</span>
                    </div>
                    <div class="fs-timeline-connector <%= "Shipped".equalsIgnoreCase(status) || "Delivered".equalsIgnoreCase(status) ? "fs-timeline-connector--done" : "" %>"></div>
                    <div class="fs-timeline-step <%= "Shipped".equalsIgnoreCase(status) || "Delivered".equalsIgnoreCase(status) ? "fs-timeline-step--done" : "" %>">
                        <div class="fs-timeline-step__marker"></div>
                        <span class="fs-timeline-step__label">Shipped</span>
                    </div>
                    <div class="fs-timeline-connector <%= "Delivered".equalsIgnoreCase(status) ? "fs-timeline-connector--done" : "" %>"></div>
                    <div class="fs-timeline-step <%= "Delivered".equalsIgnoreCase(status) ? "fs-timeline-step--done" : "" %>">
                        <div class="fs-timeline-step__marker"></div>
                        <span class="fs-timeline-step__label">Delivered</span>
                    </div>
                </div>

                <div class="fs-order-card__items">
                    <h4>Items in this order</h4>
                    <%
                        List<OrderItem> items = order.getItems();
                    %>

                    <% if (items != null && !items.isEmpty()) { %>
                        <div class="fs-order-card__items-list">
                        <% for (OrderItem item : items) { %>
                            <div class="fs-order-item-row">
                                <div class="fs-order-item-row__details">
                                    <span>Product #<%= item.getProductId() %></span>
                                    <span class="text-secondary">Qty: <%= item.getQuantity() %></span>
                                </div>
                                <span>₹<%= String.format("%.2f", item.getPrice()) %></span>
                            </div>
                        <% } %>
                        </div>
                    <% } else { %>
                        <p class="text-secondary">No items found for this order</p>
                    <% } %>
                </div>

                <div class="fs-order-card__actions">
                    <a class="fs-btn fs-btn--outline" href="<%= request.getContextPath() %>/products">Shop Again</a>
                    <% if ("Delivered".equalsIgnoreCase(status) || "Shipped".equalsIgnoreCase(status)) { %>
                        <button class="fs-btn fs-btn--outline" type="button" id="download-invoice-btn">Download Invoice</button>
                        <button class="fs-btn fs-btn--outline" type="button" id="invoice-btn">Invoice</button>
                    <% } %>
                </div>

            </article>

        <% } %>

    <% } else { %>

        <div class="fs-empty-state">
            <div class="fs-empty-state__visual">📦</div>
            <h2 class="editorial-heading">No orders yet</h2>
            <p class="text-secondary">Start shopping to track your orders here.</p>
            <a href="<%= request.getContextPath() %>/products" class="fs-btn fs-btn--primary">Start Shopping</a>
        </div>

    <% } %>

</main>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
