<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.fashionstore.model.Product" %>
<%@ page import="com.fashionstore.model.Category" %>
<%@ page import="java.util.List" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    Object _modeObj = request.getAttribute("mode");
    String _mode = (_modeObj != null) ? _modeObj.toString() : "";
    String _formTitle = "edit".equals(_mode) ? "Edit Product" : "Add Product";
    request.setAttribute("_pageTitle", "Admin – " + _formTitle);
    request.setAttribute("_pageCSS", "admin");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>
<body>

<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<%
    Product p = null;
    Object pObj = request.getAttribute("product");
    if (pObj instanceof Product) {
        p = (Product) pObj;
    }
    List<Category> categories = (List<Category>) request.getAttribute("categories");
    String mode = (_modeObj != null) ? _modeObj.toString() : "";
    if (p == null) p = new Product();
%>

<main class="form-container">
    <h1><%= "edit".equals(mode) ? "Edit Product" : "Add Product" %></h1>

    <div class="form-card">

        <!-- UI FEEDBACK -->
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-danger"><%= request.getAttribute("error") %></div>
        <% } %>

        <form action="<%= request.getContextPath() %>/admin/products" method="post" onsubmit="return validateForm()">
            <input type="hidden" name="csrf_token" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>">
            <input type="hidden" name="action" value="<%= mode %>">
            <% if ("edit".equals(mode)) { %>
                <input type="hidden" name="productId" value="<%= p.getProductId() %>">
            <% } %>

            <div class="admin-form-group">
                <label for="productName">Product Name</label>
                <input type="text" id="productName" name="productName"
                       value="<%= p.getProductName() != null ? p.getProductName() : "" %>"
                       placeholder="e.g. Classic White Tee" required>
            </div>

            <div class="admin-form-group">
                <label for="categoryId">Category</label>
                <select id="categoryId" name="categoryId" required>
                    <option value="" <%= p.getCategoryId() <= 0 ? "selected" : "" %>>Select category</option>
                    <% if (categories != null) { %>
                        <% for (Category c : categories) { %>
                            <option value="<%= c.getCategoryId() %>" <%= p.getCategoryId() == c.getCategoryId() ? "selected" : "" %>><%= c.getCategoryName() %></option>
                        <% } %>
                    <% } %>
                </select>
            </div>

            <div class="admin-form-group">
                <label for="description">Description</label>
                <textarea id="description" name="description" rows="4"
                          placeholder="Describe the product..." required><%= p.getDescription() != null ? p.getDescription() : "" %></textarea>
            </div>

            <div class="admin-form-group">
                <label for="price">Price (₹)</label>
                <input type="number" id="price" name="price" step="0.01" min="0.01"
                       value="<%= p.getPrice() %>" required>
            </div>

            <div class="admin-form-group">
                <label for="discountPercent">Discount (%)</label>
                <input type="number" id="discountPercent" name="discountPercent" step="0.01" min="0" max="100"
                       value="<%= p.getDiscountPercent() %>" required>
            </div>

            <div class="admin-form-group">
                <label for="imageUrl">Image URL</label>
                <input type="url" id="imageUrl" name="imageUrl"
                       value="<%= p.getImageUrl() != null ? p.getImageUrl() : "" %>"
                       placeholder="https://..." required>
            </div>

            <div class="admin-form-group">
                <label for="brand">Brand</label>
                <input type="text" id="brand" name="brand"
                       value="<%= p.getBrand() != null ? p.getBrand() : "" %>"
                       placeholder="e.g. Nike, Zara, FashionStore">
            </div>

            <div class="admin-form-group">
                <fieldset>
                    <legend>Visibility & Badges</legend>
                    <label class="form-checkbox">
                        <input type="checkbox" name="active" <%= p.isActive() || !"edit".equals(mode) ? "checked" : "" %>>
                        <span>Active</span>
                    </label>
                    <label class="form-checkbox">
                        <input type="checkbox" name="isNew" <%= p.isNew() ? "checked" : "" %>>
                        <span>New</span>
                    </label>
                    <label class="form-checkbox">
                        <input type="checkbox" name="isSale" <%= p.isSale() ? "checked" : "" %>>
                        <span>Sale</span>
                    </label>
                    <label class="form-checkbox">
                        <input type="checkbox" name="isTrending" <%= p.isTrending() ? "checked" : "" %>>
                        <span>Trending</span>
                    </label>
                </fieldset>
            </div>

            <div class="admin-form-group">
                <fieldset>
                    <legend>Inventory (Stock per Size)</legend>
                    <div class="size-stock-grid">
                        <%
                            String[] labels = {"S", "M", "L", "XL"};
                            for (String label : labels) {
                                int stock = 0;
                                if (p.getSizes() != null) {
                                    for (com.fashionstore.model.ProductSize s : p.getSizes()) {
                                        if (label.equals(s.getSizeLabel())) {
                                            stock = s.getStockQuantity();
                                            break;
                                        }
                                    }
                                }
                                String inputId = "stock_" + label;
                        %>
                            <div class="size-stock-row">
                                <label for="<%= inputId %>"><%= label %></label>
                                <input type="number" id="<%= inputId %>" name="<%= inputId %>" min="0" value="<%= stock %>" required>
                            </div>
                        <% } %>
                    </div>
                </fieldset>
            </div>

            <button type="submit" class="submit-btn">
                <%= "edit".equals(mode) ? "Update Product" : "Add Product" %>
            </button>
        </form>

    </div><!-- /.form-card -->
</main>

<script>
function validateForm() {
    const price = document.getElementById('price').value;
    if (parseFloat(price) <= 0) {
        alert('Price must be greater than zero.');
        document.getElementById('price').focus();
        return false;
    }
    return true;
}
</script>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
