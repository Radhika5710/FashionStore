package com.fashionstore.dao;

import com.fashionstore.model.Invoice;
import java.util.List;

public interface InvoiceDAO {
    boolean createInvoice(Invoice invoice);
    Invoice getInvoiceById(int invoiceId);
    Invoice getInvoiceByOrderId(int orderId);
    List<Invoice> getInvoicesByUserId(int userId);
    boolean updateInvoice(Invoice invoice);
    boolean deleteInvoice(int invoiceId);
}
