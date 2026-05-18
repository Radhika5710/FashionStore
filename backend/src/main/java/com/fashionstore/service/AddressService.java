package com.fashionstore.service;

import com.fashionstore.dao.AddressDAO;
import com.fashionstore.model.Address;
import com.fashionstore.validation.AddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AddressService {
    private static final Logger logger = LoggerFactory.getLogger(AddressService.class);
    private final AddressDAO addressDAO;

    public AddressService() {
        // Default constructor - DAO will be set via setter injection
        this.addressDAO = null;
    }

    public AddressService(AddressDAO addressDAO) {
        this.addressDAO = addressDAO;
    }

    public void setAddressDAO(AddressDAO addressDAO) {
        if (this.addressDAO == null) {
            try {
                java.lang.reflect.Field field = AddressService.class.getDeclaredField("addressDAO");
                field.setAccessible(true);
                field.set(this, addressDAO);
            } catch (Exception e) {
                logger.error("Failed to set addressDAO", e);
            }
        }
    }

    // CRUD Operations
    public boolean addAddress(Address address) {
        if (address == null) {
            logger.warn("Cannot add null address");
            return false;
        }
        AddressValidator.sanitize(address);
        Map<String, String> errors = AddressValidator.validate(address);
        if (!errors.isEmpty()) {
            logger.warn("Address validation failed: {}", errors);
            return false;
        }
        return addressDAO.addAddress(address);
    }

    public boolean updateAddress(Address address) {
        if (address == null || address.getAddressId() <= 0) {
            logger.warn("Cannot update null or invalid address");
            return false;
        }
        AddressValidator.sanitize(address);
        Map<String, String> errors = AddressValidator.validate(address);
        if (!errors.isEmpty()) {
            logger.warn("Address validation failed: {}", errors);
            return false;
        }
        return addressDAO.updateAddress(address);
    }

    /**
     * Validate without persisting. Returns field-level error map (empty if valid).
     */
    public Map<String, String> validate(Address address) {
        AddressValidator.sanitize(address);
        return AddressValidator.validate(address);
    }

    public boolean deleteAddress(int addressId, int userId) {
        if (addressId <= 0 || userId <= 0) {
            logger.warn("Invalid addressId or userId");
            return false;
        }
        
        // Check if this is the only address
        int addressCount = addressDAO.getAddressCount(userId);
        if (addressCount <= 1) {
            logger.warn("Cannot delete the only address for user {}", userId);
            return false;
        }
        
        // Check if this is a default address
        Address address = addressDAO.getAddressById(addressId, userId);
        if (address != null && address.isDefault()) {
            logger.warn("Cannot delete default address for user {}", userId);
            return false;
        }
        
        return addressDAO.deleteAddress(addressId, userId);
    }

    public Address getAddressById(int addressId, int userId) {
        if (addressId <= 0 || userId <= 0) {
            logger.warn("Invalid addressId or userId");
            return null;
        }
        return addressDAO.getAddressById(addressId, userId);
    }

    public List<Address> getAddressesByUserId(int userId) {
        if (userId <= 0) {
            logger.warn("Invalid userId");
            return List.of();
        }
        return addressDAO.getAddressesByUserId(userId);
    }

    // Default Address Management
    public Address getDefaultAddress(int userId, String addressType) {
        if (userId <= 0 || addressType == null || addressType.trim().isEmpty()) {
            logger.warn("Invalid userId or addressType");
            return null;
        }
        return addressDAO.getDefaultAddress(userId, addressType);
    }

    public boolean setDefaultAddress(int addressId, int userId) {
        if (addressId <= 0 || userId <= 0) {
            logger.warn("Invalid addressId or userId");
            return false;
        }
        
        // Verify address belongs to user
        if (!addressDAO.addressExists(addressId, userId)) {
            logger.warn("Address {} does not belong to user {}", addressId, userId);
            return false;
        }
        
        return addressDAO.setDefaultAddress(addressId, userId);
    }

    // Utility Methods
    public int getAddressCount(int userId) {
        if (userId <= 0) {
            logger.warn("Invalid userId");
            return 0;
        }
        return addressDAO.getAddressCount(userId);
    }

    public boolean addressExists(int addressId, int userId) {
        if (addressId <= 0 || userId <= 0) {
            logger.warn("Invalid addressId or userId");
            return false;
        }
        return addressDAO.addressExists(addressId, userId);
    }

    // Validation now delegated to AddressValidator (server-side, field-level).
}
