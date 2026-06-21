<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
<c:set var="_pageTitle" value="Catalog" scope="request"/>
<c:set var="_pageCSS" value="products" scope="request"/>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
<script src="${pageContext.request.contextPath}/assets/js/modules/filter-sidebar.js"></script>
</head>

<body>

<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<main class="fs-catalog-page" id="catalog-main">

<nav class="shell fs-catalog-nav" aria-label="Product categories">
    <a href="${pageContext.request.contextPath}/products"
       class="fs-pill ${empty param.categoryId and empty param.tag ? 'fs-pill--active' : ''}">All</a>

    <a href="${pageContext.request.contextPath}/products?tag=deals"
       class="fs-pill ${param.tag eq 'deals' or param.tag eq 'sale' ? 'fs-pill--active' : ''}">Deals</a>

    <c:forEach var="category" items="${categories}">
        <a href="${pageContext.request.contextPath}/products?category=${category.categorySlug}"
           class="fs-pill ${categoryId eq category.categoryId ? 'fs-pill--active' : ''}">${category.categoryName}</a>
    </c:forEach>
</nav>

<section class="shell section-block">
    <nav class="breadcrumb" aria-label="Breadcrumb">
        <a href="${pageContext.request.contextPath}/home">Home</a>
        <span>/</span>
        <a href="${pageContext.request.contextPath}/products">Catalog</a>
        <c:if test="${not empty categorySlug}">
            <span>/</span>
            <span><c:out value="${categorySlug}"/></span>
        </c:if>
    </nav>
    <div class="fs-catalog-hero">
        <div class="fs-catalog-hero__content">
            <span class="fs-catalog-hero__eyebrow">FashionStore Catalog</span>
            <h1 class="editorial-heading">Shop the complete edit</h1>
            <p>Refined essentials, premium footwear, and polished accessories filtered by real category mapping.</p>
        </div>
        <form class="fs-catalog-hero__search" action="${pageContext.request.contextPath}/products" method="get">
            <input type="search" name="search" value="${param.search}" placeholder="Search products or brands" aria-label="Search products">
            <c:if test="${not empty categoryId}">
                <input type="hidden" name="category" value="${categorySlug}">
            </c:if>
            <button type="submit" class="fs-btn fs-btn--primary">Search</button>
        </form>
    </div>
</section>

<div class="fs-filter-overlay" id="filter-overlay" aria-hidden="true"></div>

<div class="shell fs-catalog-layout">

    <aside class="fs-filter-sidebar" id="filter-sidebar" aria-label="Product filters">
        <button class="fs-filter-sidebar__close" id="filter-close-btn" aria-label="Close filters">
            <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
            Close Filters
        </button>

        <form action="${pageContext.request.contextPath}/products" method="get" aria-label="Filter products">
            <input type="hidden" name="search" value="${param.search}">
            <c:if test="${not empty categoryId}">
                <input type="hidden" name="category" value="${categorySlug}">
            </c:if>
            <c:if test="${not empty param.tag}">
                <input type="hidden" name="tag" value="${param.tag}">
            </c:if>
            <c:if test="${not empty param.sortBy}">
                <input type="hidden" name="sortBy" value="${param.sortBy}">
            </c:if>

            <div class="fs-filter-group">
                <h3 class="fs-filter-group__title">Price Range</h3>
                <div class="fs-filter-group__price-inputs">
                    <input type="number" name="minPrice" placeholder="Min" class="fs-form-input" value="${param.minPrice}">
                    <span>—</span>
                    <input type="number" name="maxPrice" placeholder="Max" class="fs-form-input" value="${param.maxPrice}">
                </div>
            </div>

            <div class="fs-filter-group">
                <h3 class="fs-filter-group__title">Size</h3>
                <div class="fs-filter-group__checkbox-list">
                    <c:forEach var="size" items="${sizes}">
                        <label class="fs-form-checkbox">
                            <input type="checkbox" name="size" value="${size}" ${fn:contains(selectedSizes, size) ? 'checked' : ''}>
                            <span>${size}</span>
                        </label>
                    </c:forEach>
                </div>
            </div>

            <button type="submit" class="fs-btn fs-btn--primary fs-btn--full-width">Apply Filters</button>
            <a href="${pageContext.request.contextPath}/products" class="fs-btn fs-btn--outline fs-btn--full-width">Clear All</a>
        </form>
    </aside>

    <main class="fs-catalog-main">
        <c:if test="${not empty param.search or not empty param.minPrice or not empty param.maxPrice or not empty param.brand or not empty param.sortBy or not empty param.size or not empty param.tag}">
        <div class="fs-active-filters">
            <span class="fs-active-filters__label">Active Filters:</span>
            <c:if test="${not empty param.search}">
                <span class="fs-filter-chip">
                    Search: "${param.search}"
                    <a href="${pageContext.request.contextPath}/products?category=${categorySlug}&tag=${param.tag}" class="fs-filter-chip__remove" aria-label="Remove search filter">✕</a>
                </span>
            </c:if>
            <c:if test="${not empty param.minPrice and not empty param.maxPrice}">
                <span class="fs-filter-chip">
                    ₹${param.minPrice} - ₹${param.maxPrice}
                    <a href="${pageContext.request.contextPath}/products?search=${param.search}&category=${categorySlug}&tag=${param.tag}&brand=${param.brand}&sortBy=${param.sortBy}" class="fs-filter-chip__remove" aria-label="Remove price filter">✕</a>
                </span>
            </c:if>
            <c:if test="${not empty param.brand}">
                <span class="fs-filter-chip">
                    Brand: ${param.brand}
                    <a href="${pageContext.request.contextPath}/products?search=${param.search}&category=${categorySlug}&tag=${param.tag}&minPrice=${param.minPrice}&maxPrice=${param.maxPrice}&sortBy=${param.sortBy}" class="fs-filter-chip__remove" aria-label="Remove brand filter">✕</a>
                </span>
            </c:if>
            <c:if test="${not empty param.sortBy}">
                <span class="fs-filter-chip">
                    Sort: ${param.sortBy}
                    <a href="${pageContext.request.contextPath}/products?search=${param.search}&category=${categorySlug}&tag=${param.tag}&minPrice=${param.minPrice}&maxPrice=${param.maxPrice}&brand=${param.brand}" class="fs-filter-chip__remove" aria-label="Remove sort filter">✕</a>
                </span>
            </c:if>
            <c:if test="${not empty param.tag}">
                <span class="fs-filter-chip">
                    Tag: ${param.tag}
                    <a href="${pageContext.request.contextPath}/products?search=${param.search}&category=${categorySlug}" class="fs-filter-chip__remove" aria-label="Remove tag filter">✕</a>
                </span>
            </c:if>
            <a href="${pageContext.request.contextPath}/products" class="fs-btn fs-btn--outline fs-btn--small">Clear All</a>
        </div>
        </c:if>
        
        <div class="fs-catalog-toolbar">
            <div class="fs-catalog-toolbar__info">
                <span><strong>${fn:length(products)}</strong> styles shown</span>
                <c:if test="${not empty param.search}"><span>for "${param.search}"</span></c:if>
            </div>
            <form action="${pageContext.request.contextPath}/products" method="get" class="fs-catalog-toolbar__sort">
                <c:if test="${not empty param.search}"><input type="hidden" name="search" value="${param.search}"></c:if>
                <c:if test="${not empty categoryId}"><input type="hidden" name="category" value="${categorySlug}"></c:if>
                <c:if test="${not empty param.tag}"><input type="hidden" name="tag" value="${param.tag}"></c:if>
                <c:if test="${not empty param.minPrice}"><input type="hidden" name="minPrice" value="${param.minPrice}"></c:if>
                <c:if test="${not empty param.maxPrice}"><input type="hidden" name="maxPrice" value="${param.maxPrice}"></c:if>
                <c:forEach var="size" items="${paramValues.size}">
                    <input type="hidden" name="size" value="${size}">
                </c:forEach>
                <label for="sortBy">Sort by</label>
                <select id="sortBy" name="sortBy" onchange="this.form.submit()" class="fs-form-select">
                    <option value="" ${empty param.sortBy ? 'selected' : ''}>Newest</option>
                    <option value="popular" ${param.sortBy eq 'popular' ? 'selected' : ''}>Trending</option>
                    <option value="price_asc" ${param.sortBy eq 'price_asc' ? 'selected' : ''}>Price low to high</option>
                    <option value="price_desc" ${param.sortBy eq 'price_desc' ? 'selected' : ''}>Price high to low</option>
                    <option value="name_asc" ${param.sortBy eq 'name_asc' ? 'selected' : ''}>Name A-Z</option>
                </select>
            </form>
        </div>
        
        <button class="fs-filter-toggle" id="filter-toggle-btn" aria-controls="filter-sidebar" aria-expanded="false">
            <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 010 2H4a1 1 0 01-1-1zm3 6a1 1 0 011-1h10a1 1 0 010 2H7a1 1 0 01-1-1zm3 6a1 1 0 011-1h4a1 1 0 010 2h-4a1 1 0 01-1-1z"/>
            </svg>
            Filters
        </button>

        <div class="product-card-grid">
            <c:if test="${not empty products}">
                <c:forEach var="product" items="${products}">
                    <article class="product-card">
                        <div class="product-card__media">
                            <a href="${pageContext.request.contextPath}/product?id=${product.productId}">
                                <img data-src="${product.imageUrl}" alt="${product.productName}" loading="lazy" class="lazy-load" onerror="this.src='${pageContext.request.contextPath}/assets/images/placeholder-product.jpg'; this.onerror=null;">
                            </a>
                            
                            <div class="product-card__badges">
                                <c:if test="${product.isNew}">
                                    <span class="product-card__badge product-card__badge--new">New</span>
                                </c:if>
                                <c:if test="${product.sale}">
                                    <span class="product-card__badge product-card__badge--sale">Sale</span>
                                </c:if>
                                <c:if test="${product.trending}">
                                    <span class="product-card__badge product-card__badge--trending">Trending</span>
                                </c:if>
                            </div>
                            
                            <button class="product-card__wishlist" data-product-id="${product.productId}" aria-label="Add ${product.productName} to wishlist">
                                <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                                </svg>
                            </button>
                        </div>
                        <div class="product-card__body">
                            <span class="product-card__eyebrow">${not empty product.brand ? product.brand : product.categoryName}</span>
                            <a href="${pageContext.request.contextPath}/product?id=${product.productId}" class="product-card__title">${product.productName}</a>
                            <div class="product-card__footer">
                                <p class="product-card__price">₹<fmt:formatNumber value="${product.price}" pattern="0.00"/></p>
                                <c:if test="${product.discountPercent > 0 and product.discountPercent < 100}">
                                    <fmt:parseNumber var="originalPrice" value="${product.price / (1 - (product.discountPercent / 100.0))}" integerOnly="false"/>
                                    <span class="product-card__price-original">₹<fmt:formatNumber value="${originalPrice}" pattern="0.00"/></span>
                                </c:if>
                            </div>
                            <div class="product-card__actions">
                                <a href="${pageContext.request.contextPath}/product?id=${product.productId}" class="product-card__btn product-card__btn--primary">View Details</a>
                                <button class="product-card__btn product-card__btn--outline" data-product-id="${product.productId}" aria-label="Add ${product.productName} to cart">
                                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"></path>
                                    </svg>
                                </button>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </c:if>
            <c:if test="${empty products}">
                <div class="fs-empty-state">
                    <svg class="fs-empty-state__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                    <h3 class="fs-empty-state__title">No products found</h3>
                    <p class="fs-empty-state__description">We couldn't find any products matching your criteria. Try adjusting your filters or search terms.</p>
                    <div class="fs-empty-state__actions">
                        <a href="${pageContext.request.contextPath}/products" class="fs-btn fs-btn--primary">Clear filters</a>
                        <a href="${pageContext.request.contextPath}/products" class="fs-btn fs-btn--outline">View all products</a>
                    </div>
                </div>
            </c:if>
        </div>
            
        <c:if test="${totalPages gt 1}">
            <div class="fs-pagination">
                <c:if test="${currentPage gt 1}">
                    <a href="${pageContext.request.contextPath}/products?page=${currentPage - 1}&search=${param.search}&category=${categorySlug}&tag=${param.tag}&minPrice=${param.minPrice}&maxPrice=${param.maxPrice}&brand=${param.brand}&sortBy=${param.sortBy}" 
                       class="fs-pagination__link" aria-label="Previous page">
                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                        </svg>
                        Prev
                    </a>
                </c:if>
                <c:if test="${currentPage le 1}">
                    <span class="fs-pagination__link fs-pagination__link--disabled">
                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                        </svg>
                        Prev
                    </span>
                </c:if>
                
                <c:forEach var="i" begin="1" end="${totalPages}">
                    <c:if test="${i eq currentPage}">
                        <span class="fs-pagination__link fs-pagination__link--active" aria-current="page">${i}</span>
                    </c:if>
                    <c:if test="${i ne currentPage and (i eq 1 or i eq totalPages or (i ge currentPage - 1 and i le currentPage + 1))}">
                        <a href="${pageContext.request.contextPath}/products?page=${i}&search=${param.search}&category=${categorySlug}&tag=${param.tag}&minPrice=${param.minPrice}&maxPrice=${param.maxPrice}&brand=${param.brand}&sortBy=${param.sortBy}" 
                           class="fs-pagination__link">${i}</a>
                    </c:if>
                    <c:if test="${i ne currentPage and (i eq currentPage - 2 or i eq currentPage + 2)}">
                        <span class="fs-pagination__ellipsis">...</span>
                    </c:if>
                </c:forEach>
                
                <c:if test="${currentPage lt totalPages}">
                    <a href="${pageContext.request.contextPath}/products?page=${currentPage + 1}&search=${param.search}&category=${categorySlug}&tag=${param.tag}&minPrice=${param.minPrice}&maxPrice=${param.maxPrice}&brand=${param.brand}&sortBy=${param.sortBy}" 
                       class="fs-pagination__link" aria-label="Next page">
                        Next
                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                        </svg>
                    </a>
                </c:if>
                <c:if test="${currentPage ge totalPages}">
                    <span class="fs-pagination__link fs-pagination__link--disabled">
                        Next
                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                        </svg>
                    </span>
                </c:if>
            </div>
        </c:if>
        </main>

    </div>
</div>

</main>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

<div class="modal-overlay" id="quickViewModal" aria-hidden="true" role="dialog" aria-modal="true">
    <div class="modal-content">
        <button class="modal-close modal-close--positioned" id="quickview-close-btn" aria-label="Close modal">
            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
        </button>
        <div class="modal-body" id="modalContent"></div>
    </div>
</div>

</body>
</html>
