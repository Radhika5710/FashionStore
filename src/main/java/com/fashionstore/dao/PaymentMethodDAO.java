package com.fashionstore.dao;

import com.fashionstore.model.PaymentMethod;
import java.util.List;

public interface PaymentMethodDAO {
    
    // Basic CRUD operations
    boolean addPaymentMethod(PaymentMethod paymentMethod);
    boolean updatePaymentMethod(PaymentMethod paymentMethod);
    boolean deletePaymentMethod(int paymentMethodId, int userId);
    
    // Query operations
    PaymentMethod getPaymentMethodById(int paymentMethodId, int userId);
    List<PaymentMethod> getPaymentMethodsByUserId(int userId);
    PaymentMethod getDefaultPaymentMethod(int userId);
    
    // Utility operations
    boolean setDefaultPaymentMethod(int paymentMethodId, int userId);
    boolean paymentMethodExists(int paymentMethodId, int userId);
    int getPaymentMethodCount(int userId);
    
    // Gateway integration
    PaymentMethod getPaymentMethodByToken(String gatewayToken, String gateway);
    boolean updateGatewayToken(int paymentMethodId, String gatewayToken);
}
