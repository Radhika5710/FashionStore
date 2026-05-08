<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "User Management");
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
            <a href="<%= request.getContextPath() %>/admin/orders" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                    <line x1="3" y1="6" x2="21" y2="6"/>
                    <path d="M16 10a4 4 0 0 1-8 0"/>
                </svg>
                Orders
            </a>
            <a href="<%= request.getContextPath() %>/admin/users" class="sidebar-link active">
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
            <h1 class="admin-title">User Management</h1>
            <p class="admin-subtitle">Manage user accounts and roles</p>
        </div>

        <!-- USERS TABLE -->
        <div class="admin-table-container">
            <table class="admin-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Phone</th>
                        <th>Role</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <% if (request.getAttribute("users") != null) { 
                       java.util.List<com.fashionstore.model.User> users = (java.util.List<com.fashionstore.model.User>) request.getAttribute("users");
                       for (com.fashionstore.model.User user : users) { %>
                    <tr>
                        <td><%= user.getUserId() %></td>
                        <td><%= user.getFullName() != null ? user.getFullName() : "N/A" %></td>
                        <td><%= user.getEmail() %></td>
                        <td><%= user.getPhone() != null ? user.getPhone() : "N/A" %></td>
                        <td>
                            <span class="status-badge <%= "admin".equals(user.getRole()) ? "status-shipped" : "status-pending" %>">
                                <%= user.getRole() != null ? user.getRole() : "user" %>
                            </span>
                        </td>
                        <td>
                            <div class="admin-actions">
                                <% if (!"admin".equals(user.getRole()) && !"disabled".equals(user.getRole())) { %>
                                <form action="<%= request.getContextPath() %>/admin/users" method="post" style="display:inline;">
                                    <input type="hidden" name="action" value="setAdmin">
                                    <input type="hidden" name="userId" value="<%= user.getUserId() %>">
                                    <button type="submit" class="admin-btn" onclick="return confirm('Promote this user to admin?')">Make Admin</button>
                                </form>
                                <form action="<%= request.getContextPath() %>/admin/users" method="post" style="display:inline;">
                                    <input type="hidden" name="action" value="disableUser">
                                    <input type="hidden" name="userId" value="<%= user.getUserId() %>">
                                    <button type="submit" class="admin-btn admin-btn-danger" onclick="return confirm('Disable this user account?')">Disable</button>
                                </form>
                                <% } else if ("disabled".equals(user.getRole())) { %>
                                <form action="<%= request.getContextPath() %>/admin/users" method="post" style="display:inline;">
                                    <input type="hidden" name="action" value="enableUser">
                                    <input type="hidden" name="userId" value="<%= user.getUserId() %>">
                                    <button type="submit" class="admin-btn" onclick="return confirm('Enable this user account?')">Enable</button>
                                </form>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                    <% } } else { %>
                    <tr>
                        <td colspan="6" style="text-align: center; color: var(--color-secondary);">No users found</td>
                    </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    </main>
</div>

</body>
</html>
