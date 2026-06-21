package com.fashionstore.dao;

import com.fashionstore.model.Payment;
import java.sql.Connection;
import java.util.List;

/**
 * DAO interface for Payment operations
 */
public interface PaymentDAO {

    int createPayment(Payment payment);

    /**
     * Create payment with connection for transaction support
     * This version is used within a transaction to ensure atomicity
     */
    int createPayment(Connection conn, Payment payment);
    
    /**
     * Create payment in transaction (alias for createPayment with connection)
     * This version is used within a transaction to ensure atomicity
     */
    int createPaymentInTransaction(Connection conn, Payment payment);

    Payment getPaymentById(int paymentId);

    Payment getPaymentByTransactionId(String transactionId);

    Payment getPaymentByOrderId(int orderId);

    Payment getPaymentByStripePaymentIntentId(String stripePaymentIntentId);

    boolean updatePaymentStatus(int paymentId, String status);

    boolean updatePaymentVerification(int paymentId, boolean verified, String webhookId);

    boolean updatePaymentFailureReason(int paymentId, String failureReason);

    boolean updateOrderPaymentStatus(int orderId, String paymentStatus, String transactionId);

    List<Payment> getPaymentsByOrderId(int orderId);

    List<Payment> getPaymentsByUserId(int userId);

    boolean logPaymentDetails(Payment payment);
}
