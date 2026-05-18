package com.fashionstore.service;

import com.fashionstore.model.Invoice;

import java.util.List;

/**
 * InvoiceService - MVC Service Layer Interface
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL invoice business logic in service layer
 * - ALL validation in service layer
 * - Controllers only handle request/response
 * - DAO layer only handles database access
 */
public interface InvoiceService {
    
    /**
     * Create invoice for an order
     */
    Invoice createInvoice(int orderId);
    
    /**
     * Get invoice by ID
     */
    Invoice getInvoiceById(int invoiceId);
    
    /**
     * Get invoice by order ID
     */
    Invoice getInvoiceByOrderId(int orderId);
    
    /**
     * Get invoices for a user
     */
    List<Invoice> getInvoicesByUserId(int userId);
    
    /**
     * Update invoice
     */
    boolean updateInvoice(Invoice invoice);
    
    /**
     * Delete invoice
     */
    boolean deleteInvoice(int invoiceId);
    
    /**
     * Generate invoice PDF
     */
    byte[] generateInvoicePdf(int invoiceId);
}
