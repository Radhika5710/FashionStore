package com.fashionstore.registry;

import com.fashionstore.dao.*;
import com.fashionstore.daoimpl.*;
import com.fashionstore.service.*;
import com.fashionstore.serviceimpl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceRegistry - Simple MVC Service Registry
 * 
 * Provides centralized access to core services and DAOs used by controllers.
 * Minimal, stable initialization - no unnecessary enterprise complexity.
 */
public class ServiceRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    
    private static volatile ServiceRegistry instance;
    
    // Core services actually used in controllers
    private CartService cartService;
    private CheckoutService checkoutService;
    private ProductService productService;
    private UserService userService;
    private ProductReviewService productReviewService;
    private SavedItemService savedItemService;
    private OrderService orderService;
    private CouponService couponService;
    private AddressService addressService;
    private CategoryService categoryService;
    private WishlistService wishlistService;
    private PaymentRecoveryService paymentRecoveryService;
    private RecentlyViewedService recentlyViewedService;
    
    // Core DAOs
    private CartDAO cartDAO;
    private ProductDAO productDAO;
    private UserDAO userDAO;
    private SavedItemDAO savedItemDAO;
    private ProductReviewDAO productReviewDAO;
    private OrderDAO orderDAO;
    private OrderItemDAO orderItemDAO;
    private AddressDAO addressDAO;
    private CouponDAO couponDAO;
    private CategoryDAO categoryDAO;
    private WishlistDAO wishlistDAO;
    private ProductSizeDAO productSizeDAO;
    private RecentlyViewedDAO recentlyViewedDAO;
    
    private ServiceRegistry() {
        initializeDAOs();
        initializeServices();
        injectDependencies();
        logger.info("ServiceRegistry initialized");
    }
    
    public static ServiceRegistry getInstance() {
        if (instance == null) {
            synchronized (ServiceRegistry.class) {
                if (instance == null) {
                    instance = new ServiceRegistry();
                }
            }
        }
        return instance;
    }
    
    private void initializeServices() {
        this.cartService = new CartServiceImpl();
        this.checkoutService = new CheckoutServiceImpl();
        this.productService = new ProductService();
        this.userService = new UserService();
        this.productReviewService = new ProductReviewServiceImpl();
        this.savedItemService = new SavedItemServiceImpl();
        this.orderService = new OrderServiceImpl();
        this.couponService = new CouponServiceImpl();
        this.addressService = new AddressService();
        this.categoryService = new CategoryService();
        this.wishlistService = new WishlistService();
        this.paymentRecoveryService = new PaymentRecoveryService();
        this.recentlyViewedService = new RecentlyViewedServiceImpl();
    }
    
    private void initializeDAOs() {
        this.cartDAO = new CartDAOImpl();
        this.productDAO = new ProductDAOImpl();
        this.userDAO = new UserDAOImpl();
        this.savedItemDAO = new SavedItemDAOImpl();
        this.productReviewDAO = new ProductReviewDAOImpl();
        this.orderDAO = new OrderDAOImpl();
        this.orderItemDAO = new OrderItemDAOImpl();
        this.addressDAO = new AddressDAOImpl();
        this.couponDAO = new CouponDAOImpl();
        this.categoryDAO = new CategoryDAOImpl();
        this.wishlistDAO = new WishlistDAOImpl();
        this.productSizeDAO = new ProductSizeDAOImpl();
        this.recentlyViewedDAO = new RecentlyViewedDAOImpl();
    }

    private void injectDependencies() {
        // Inject DAOs into CartServiceImpl
        if (cartService instanceof CartServiceImpl) {
            ((CartServiceImpl) cartService).setCartDAO(cartDAO);
            ((CartServiceImpl) cartService).setProductDAO(productDAO);
        }

        // Inject dependencies into CheckoutServiceImpl
        if (checkoutService instanceof CheckoutServiceImpl) {
            ((CheckoutServiceImpl) checkoutService).setCartService(cartService);
            ((CheckoutServiceImpl) checkoutService).setCartDAO(cartDAO);
            ((CheckoutServiceImpl) checkoutService).setProductDAO(productDAO);
            ((CheckoutServiceImpl) checkoutService).setAddressService(addressService);
            ((CheckoutServiceImpl) checkoutService).setCouponService(couponService);
        }

        // Inject DAOs into ProductReviewServiceImpl
        if (productReviewService instanceof ProductReviewServiceImpl) {
            ((ProductReviewServiceImpl) productReviewService).setReviewDAO(productReviewDAO);
        }

        // Inject DAOs into SavedItemServiceImpl
        if (savedItemService instanceof SavedItemServiceImpl) {
            ((SavedItemServiceImpl) savedItemService).setSavedItemDAO(savedItemDAO);
        }

        // Inject DAOs into OrderServiceImpl
        if (orderService instanceof OrderServiceImpl) {
            ((OrderServiceImpl) orderService).setOrderDAO(orderDAO);
            ((OrderServiceImpl) orderService).setOrderItemDAO(orderItemDAO);
        }

        // Inject DAOs into AddressService
        addressService.setAddressDAO(addressDAO);

        // Inject DAOs into CouponServiceImpl
        if (couponService instanceof CouponServiceImpl) {
            ((CouponServiceImpl) couponService).setCouponDAO(couponDAO);
        }

        // Inject DAOs into WishlistService
        wishlistService.setWishlistDAO(wishlistDAO);

        // Inject DAOs into RecentlyViewedService
        if (recentlyViewedService instanceof RecentlyViewedServiceImpl) {
            ((RecentlyViewedServiceImpl) recentlyViewedService).setRecentlyViewedDAO(recentlyViewedDAO);
        }
    }
    
    public CartService getCartService() {
        return cartService;
    }
    
    public CheckoutService getCheckoutService() {
        return checkoutService;
    }
    
    public ProductService getProductService() {
        return productService;
    }
    
    public UserService getUserService() {
        return userService;
    }
    
    public ProductReviewService getProductReviewService() {
        return productReviewService;
    }
    
    public SavedItemService getSavedItemService() {
        return savedItemService;
    }
    
    public OrderService getOrderService() {
        return orderService;
    }
    
    public CouponService getCouponService() {
        return couponService;
    }

    public AddressService getAddressService() {
        return addressService;
    }

    public CategoryService getCategoryService() {
        return categoryService;
    }

    public WishlistService getWishlistService() {
        return wishlistService;
    }

    public PaymentRecoveryService getPaymentRecoveryService() {
        return paymentRecoveryService;
    }

    public RecentlyViewedService getRecentlyViewedService() {
        return recentlyViewedService;
    }

    public CartDAO getCartDAO() {
        return cartDAO;
    }

    public ProductDAO getProductDAO() {
        return productDAO;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public SavedItemDAO getSavedItemDAO() {
        return savedItemDAO;
    }

    public ProductReviewDAO getProductReviewDAO() {
        return productReviewDAO;
    }

    public OrderDAO getOrderDAO() {
        return orderDAO;
    }

    public OrderItemDAO getOrderItemDAO() {
        return orderItemDAO;
    }

    public AddressDAO getAddressDAO() {
        return addressDAO;
    }

    public CouponDAO getCouponDAO() {
        return couponDAO;
    }

    public CategoryDAO getCategoryDAO() {
        return categoryDAO;
    }

    public WishlistDAO getWishlistDAO() {
        return wishlistDAO;
    }

    public ProductSizeDAO getProductSizeDAO() {
        return productSizeDAO;
    }

    public RecentlyViewedDAO getRecentlyViewedDAO() {
        return recentlyViewedDAO;
    }
}
