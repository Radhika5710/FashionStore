<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.fashionstore.model.Order" %>
<%@ page import="com.fashionstore.model.OrderItem" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Order Management");
    request.setAttribute("_pageCSS", "admin");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>
<body class="admin-dashboard">

<div class="admin-layout">
    <!-- SIDEBAR -->
    <aside class="admin-sidebar">
        <div class="sidebar-brand">FashionStore Admin</div>
        <nav class="sidebar-nav">
            <a href="<%= request.getContextPath() %>/admin/dashboard" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="7" height="7"/>
                    <rect x="14" y="3" width="7" height="7"/>
                    <rect x="14" y="14" width="7" height="7"/>
                    <rect x="3" y="14" width="7" height="7"/>
                </svg>
                Dashboard
            </a>
            <a href="<%= request.getContextPath() %>/admin/products" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                    <line x1="7" y1="7" x2="7.01" y2="7"/>
                </svg>
                Products
            </a>
            <a href="<%= request.getContextPath() %>/admin/orders" class="sidebar-link active">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                    <line x1="3" y1="6" x2="21" y2="6"/>
                    <path d="M16 10a4 4 0 0 1-8 0"/>
                </svg>
                Orders
            </a>
            <a href="<%= request.getContextPath() %>/admin/users" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                Users
            </a>
            <a href="<%= request.getContextPath() %>/products" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
                </svg>
                Back to Store
            </a>
        </nav>
    </aside>

    <!-- MAIN CONTENT -->
    <main class="admin-content">
        <div class="admin-header">
            <h1 class="admin-title">Order Management</h1>
            <p class="admin-subtitle">Manage orders, shipments, and refunds</p>
        </div>

        <!-- ORDER FILTERING -->
        <div class="glass-card" style="padding: var(--space-4); margin-bottom: var(--space-5);">
            <div style="display: flex; gap: var(--space-3); flex-wrap: wrap; align-items: center;">
                <div style="flex: 1; min-width: 200px;">
                    <label style="font-size: 11px; font-weight: 600; text-transform: uppercase; color: var(--color-secondary); margin-bottom: 4px; display: block;">Search Orders</label>
                    <input type="text" id="searchOrders" placeholder="Search by Order ID, Customer Name..." style="width: 100%; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 14px;">
                </div>
                <div style="min-width: 150px;">
                    <label style="font-size: 11px; font-weight: 600; text-transform: uppercase; color: var(--color-secondary); margin-bottom: 4px; display: block;">Status Filter</label>
                    <select id="statusFilter" style="width: 100%; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 14px; background: var(--color-surface);">
                        <option value="">All Status</option>
                        <option value="Pending">Pending</option>
                        <option value="Shipped">Shipped</option>
                        <option value="Delivered">Delivered</option>
                        <option value="Cancelled">Cancelled</option>
                    </select>
                </div>
                <div style="min-width: 150px;">
                    <label style="font-size: 11px; font-weight: 600; text-transform: uppercase; color: var(--color-secondary); margin-bottom: 4px; display: block;">Date Range</label>
                    <select id="dateFilter" style="width: 100%; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 14px; background: var(--color-surface);">
                        <option value="">All Time</option>
                        <option value="7">Last 7 Days</option>
                        <option value="30">Last 30 Days</option>
                        <option value="90">Last 90 Days</option>
                    </select>
                </div>
            </div>
        </div>

        <!-- ALERTS -->
        <% String message = (String) session.getAttribute("message"); %>
        <% if (message != null) { %>
            <div class="alert alert-success" style="margin-bottom: var(--space-4);"><%= message %></div>
            <% session.removeAttribute("message"); %>
        <% } %>
        <% String error = (String) session.getAttribute("error"); %>
        <% if (error != null) { %>
            <div class="alert alert-danger" style="margin-bottom: var(--space-4);"><%= error %></div>
            <% session.removeAttribute("error"); %>
        <% } %>

        <!-- ORDERS LIST -->
        <%
            List<Order> orders = new ArrayList<>();
            Object obj = request.getAttribute("orders");
            if (obj instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Order> temp = (List<Order>) obj;
                orders = temp;
            }
        %>

        <% if (orders.isEmpty()) { %>
            <div class="glass-card" style="padding: var(--space-7) var(--space-5); text-align: center;">
                <p style="color: var(--color-secondary);">No orders found.</p>
            </div>
        <% } %>

        <% for (Order order : orders) { %>
            <article class="glass-card" style="padding: var(--space-5); margin-bottom: var(--space-4);">
                <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
                    <div>
                        <strong style="font-size: 16px; color: var(--text-primary);">Order #<%= order.getOrderId() %></strong>
                        <div style="font-size:13px; color:var(--color-secondary); margin-top:4px;">
                            <%= order.getFullName() != null ? order.getFullName() : "User " + order.getUserId() %> | 
                            Total: ₹<%= String.format("%.2f", order.getTotalAmount()) %> |
                            <%= order.getOrderDate() != null ? new java.text.SimpleDateFormat("MMM dd, yyyy").format(order.getOrderDate()) : "" %>
                        </div>
                    </div>
                    <div>
                        <%
                            String status = order.getStatus() == null ? "Pending" : order.getStatus();
                            String css = "status-pending";
                            if ("Shipped".equalsIgnoreCase(status)) css = "status-shipped";
                            if ("Delivered".equalsIgnoreCase(status)) css = "status-delivered";
                            if ("Cancelled".equalsIgnoreCase(status)) css = "badge-cancelled";
                        %>
                        <span class="status-badge <%= css %>"><%= status %></span>
                    </div>
                </div>

                <!-- STATUS UPDATE & SHIPMENT SIMULATION -->
                <div style="margin-top: var(--space-4); padding-top: var(--space-4); border-top: 1px solid var(--color-border);">
                    <form action="<%= request.getContextPath() %>/admin/orders" method="post" class="status-form">
                        <input type="hidden" name="csrf_token" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>">
                        <input type="hidden" name="orderId" value="<%= order.getOrderId() %>">
                        <label for="status-<%= order.getOrderId() %>" class="sr-only">Update order status</label>
                        <select name="status" id="status-<%= order.getOrderId() %>">
                            <option value="Pending" <%= "Pending".equalsIgnoreCase(status) ? "selected" : "" %>>Pending</option>
                            <option value="Shipped" <%= "Shipped".equalsIgnoreCase(status) ? "selected" : "" %>>Shipped</option>
                            <option value="Delivered" <%= "Delivered".equalsIgnoreCase(status) ? "selected" : "" %>>Delivered</option>
                            <option value="Cancelled" <%= "Cancelled".equalsIgnoreCase(status) ? "selected" : "" %>>Cancelled</option>
                        </select>
                        <button type="submit" name="updateStatus" value="true">Update Status</button>
                        
                        <% if ("Shipped".equalsIgnoreCase(status)) { %>
                            <button type="submit" name="simulateDelivery" value="true" style="background: var(--color-success);">Simulate Delivery</button>
                        <% } %>
                        
                        <% if ("Delivered".equalsIgnoreCase(status)) { %>
                            <button type="submit" name="simulateRefund" value="true" style="background: var(--color-danger);">Simulate Refund</button>
                        <% } %>
                    </form>
                </div>

                <!-- ORDER ITEMS -->
                <div class="items">
                    <% List<OrderItem> items = order.getItems(); %>
                    <% if (items != null && !items.isEmpty()) { %>
                        <% for (OrderItem item : items) { %>
                            <div class="item-row">
                                Product #<%= item.getProductId() %> | Size: <%= item.getSizeLabel() %> | Qty: <%= item.getQuantity() %> | ₹<%= String.format("%.2f", item.getPrice()) %>
                            </div>
                        <% } %>
                    <% } else { %>
                        <div class="item-row">No items found.</div>
                    <% } %>
                </div>

                <!-- CUSTOMER DETAILS -->
                <div style="margin-top: var(--space-3); padding-top: var(--space-3); border-top: 1px dashed var(--color-border); font-size: 13px; color: var(--color-secondary);">
                    <div><strong>Shipping Address:</strong> <%= order.getAddress() %>, <%= order.getCity() %>, <%= order.getState() %> - <%= order.getZip() %></div>
                    <div style="margin-top: 4px;"><strong>Phone:</strong> <%= order.getPhone() %></div>
                    <div style="margin-top: 4px;"><strong>Payment Method:</strong> <%= order.getPaymentMethod() %></div>
                </div>
            </article>
        <% } %>
    </main>
</div>

<script>
// Order filtering functionality
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchOrders');
    const statusFilter = document.getElementById('statusFilter');
    const dateFilter = document.getElementById('dateFilter');
    
    if (searchInput && statusFilter) {
        function filterOrders() {
            const searchTerm = searchInput.value.toLowerCase();
            const statusValue = statusFilter.value;
            
            document.querySelectorAll('article.glass-card').forEach(card => {
                const orderId = card.querySelector('strong')?.textContent.toLowerCase() || '';
                const statusBadge = card.querySelector('.status-badge')?.textContent.toLowerCase() || '';
                const customerInfo = card.querySelector('div:nth-child(2)')?.textContent.toLowerCase() || '';
                
                const matchesSearch = orderId.includes(searchTerm) || customerInfo.includes(searchTerm);
                const matchesStatus = statusValue === '' || statusBadge === statusValue.toLowerCase();
                
                card.style.display = matchesSearch && matchesStatus ? 'block' : 'none';
            });
        }
        
        searchInput.addEventListener('input', filterOrders);
        statusFilter.addEventListener('change', filterOrders);
    }
});
</script>

</body>
</html>
