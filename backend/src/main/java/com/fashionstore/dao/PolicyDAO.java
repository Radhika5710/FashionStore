package com.fashionstore.dao;

import com.fashionstore.model.Policy;
import java.util.List;

public interface PolicyDAO {
    Policy getPolicyByType(String policyType);
    List<Policy> getAllPolicies();
    List<Policy> getActivePolicies();
    boolean updatePolicy(Policy policy);
}
