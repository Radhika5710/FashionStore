package com.fashionstore.serviceimpl;

import com.fashionstore.dao.InvoiceDAO;
import com.fashionstore.daoimpl.InvoiceDAOImpl;
import com.fashionstore.model.Invoice;
import com.fashionstore.model.Order;
import com.fashionstore.service.InvoiceService;
import com.fashionstore.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * InvoiceServiceImpl - MVC Service Layer Implementation
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL invoice business logic in service layer
 * - ALL validation in service layer
 * - Controllers only handle request/response
 * - DAO layer only handles database access
 */
public class InvoiceServiceImpl implements InvoiceService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceServiceImpl.class);
    
    private final InvoiceDAO invoiceDAO;
    private final OrderService orderService;
    
    public InvoiceServiceImpl() {
        this.invoiceDAO = new InvoiceDAOImpl();
        this.orderService = new com.fashionstore.serviceimpl.OrderServiceImpl();
    }
    
    public InvoiceServiceImpl(InvoiceDAO invoiceDAO, OrderService orderService) {
        this.invoiceDAO = invoiceDAO;
        this.orderService = orderService;
    }
    
    @Override
    public Invoice createInvoice(int orderId) {
        // Business logic: Validate order exists
        Order order = orderService.getOrderById(orderId, 0); // 0 for admin
        if (order == null) {
            logger.error("Order not found for invoice creation: {}", orderId);
            return null;
        }
        
        // Business logic: Check if invoice already exists
        Invoice existingInvoice = invoiceDAO.getInvoiceByOrderId(orderId);
        if (existingInvoice != null) {
            logger.info("Invoice already exists for order: {}", orderId);
            return existingInvoice;
        }
        
        // Business logic: Create invoice with order details
        Invoice invoice = new Invoice();
        invoice.setOrderId(orderId);
        invoice.setUserId(order.getUserId());
        invoice.setAmount(java.math.BigDecimal.valueOf(order.getTotalAmount()));
        invoice.setInvoiceNumber(generateInvoiceNumber());
        
        boolean created = invoiceDAO.createInvoice(invoice);
        if (created) {
            logger.info("Invoice created for order: {}", orderId);
            return invoice;
        }
        
        logger.error("Failed to create invoice for order: {}", orderId);
        return null;
    }
    
    @Override
    public Invoice getInvoiceById(int invoiceId) {
        return invoiceDAO.getInvoiceById(invoiceId);
    }
    
    @Override
    public Invoice getInvoiceByOrderId(int orderId) {
        return invoiceDAO.getInvoiceByOrderId(orderId);
    }
    
    @Override
    public List<Invoice> getInvoicesByUserId(int userId) {
        return invoiceDAO.getInvoicesByUserId(userId);
    }
    
    @Override
    public boolean updateInvoice(Invoice invoice) {
        if (invoice == null || invoice.getInvoiceId() <= 0) {
            logger.error("Invalid invoice data");
            return false;
        }
        return invoiceDAO.updateInvoice(invoice);
    }
    
    @Override
    public boolean deleteInvoice(int invoiceId) {
        return invoiceDAO.deleteInvoice(invoiceId);
    }
    
    @Override
    public byte[] generateInvoicePdf(int invoiceId) {
        // Business logic: Generate PDF invoice
        // This would typically use a PDF library like iText or Apache PDFBox
        logger.warn("PDF generation not implemented yet for invoice: {}", invoiceId);
        return null;
    }
    
    /**
     * Generate unique invoice number
     */
    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis();
    }
}
