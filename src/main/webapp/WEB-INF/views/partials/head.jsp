<%-- 
    head.jsp: Shared meta tags and font loading.
    USAGE: Call request.setAttribute("_pageTitle","...") and request.setAttribute("_pageCSS","css-name")
    before including this file. Do NOT put a page contentType directive here.
--%>
<%
    String _pageTitle = (String) request.getAttribute("_pageTitle");
    if (_pageTitle == null || _pageTitle.trim().isEmpty()) _pageTitle = "FashionStore";
    String _pageCSS   = (String) request.getAttribute("_pageCSS");
%>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="FashionStore – premium fashion marketplace with curated styles for every season.">
<title><%= _pageTitle %> – FashionStore</title>

<%-- Google Fonts: ONE place, no @import --%>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@400;500;600&display=swap" rel="stylesheet">

<%-- Design system ALWAYS loads first --%>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">

<%-- Page-level CSS (comma-separated filenames) --%>
<% if (_pageCSS != null && !_pageCSS.trim().isEmpty()) {
       for (String _css : _pageCSS.split(",")) {
           _css = _css.trim();
           if (!_css.isEmpty()) { %>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/<%= _css %>.css">
<%     }
   }
} %>

<%-- Main JavaScript (loaded in head for inline handlers) --%>
<script src="<%= request.getContextPath() %>/assets/js/main.js"></script>
