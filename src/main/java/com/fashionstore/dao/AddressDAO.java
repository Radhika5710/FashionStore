package com.fashionstore.dao;

import com.fashionstore.model.Address;
import java.util.List;

public interface AddressDAO {
    // CRUD operations
    boolean addAddress(Address address);
    boolean updateAddress(Address address);
    boolean deleteAddress(int addressId, int userId);
    Address getAddressById(int addressId, int userId);
    List<Address> getAddressesByUserId(int userId);
    
    // Default address management
    Address getDefaultAddress(int userId, String addressType);
    boolean setDefaultAddress(int addressId, int userId);
    
    // Utility methods
    int getAddressCount(int userId);
    boolean addressExists(int addressId, int userId);
}
