package com.fashionstore.validation;

import com.fashionstore.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Validator {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    /** Letters (any script), spaces, apostrophe, hyphen, period — avoids locking out real names. */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\s'.-]{2,80}$");
    
    private final List<String> errors = new ArrayList<>();
    
    public Validator validateEmail(String email, String fieldName) {
        if (email == null || email.trim().isEmpty()) {
            errors.add(fieldName + " is required");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add(fieldName + " is invalid");
        }
        return this;
    }
    
    public Validator validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(fieldName + " is required");
        }
        return this;
    }
    
    public Validator validateMinLength(String value, int minLength, String fieldName) {
        if (value != null && value.length() < minLength) {
            errors.add(fieldName + " must be at least " + minLength + " characters");
        }
        return this;
    }
    
    public Validator validateMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            errors.add(fieldName + " must not exceed " + maxLength + " characters");
        }
        return this;
    }
    
    public Validator validatePhone(String phone, String fieldName) {
        // Relaxed phone validation - just check if not empty
        if (phone != null && phone.trim().isEmpty()) {
            errors.add(fieldName + " is required");
        }
        return this;
    }
    
    public Validator validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            errors.add(fieldName + " is required");
        } else if (!NAME_PATTERN.matcher(name).matches()) {
            errors.add(fieldName + " must contain only letters and spaces");
        }
        return this;
    }
    
    public Validator validatePassword(String password, String fieldName) {
        if (password == null || password.isEmpty()) {
            errors.add(fieldName + " is required");
            return this;
        }

        if (password.length() < 6) {
            errors.add(fieldName + " must be at least 6 characters long");
        }

        // Relaxed password requirements - only check length
        // Removed uppercase, lowercase, and digit requirements for easier registration

        return this;
    }
    
    public Validator validatePositive(int value, String fieldName) {
        if (value <= 0) {
            errors.add(fieldName + " must be positive");
        }
        return this;
    }
    
    public Validator validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            errors.add(fieldName + " cannot be negative");
        }
        return this;
    }
    
    public Validator validateMatch(String value1, String value2, String fieldName) {
        if (value1 == null || value2 == null || !value1.equals(value2)) {
            errors.add(fieldName + " do not match");
        }
        return this;
    }

    /**
     * Optional postal-style address: when provided, enforce length and reject dangerous control characters.
     */
    public Validator validateOptionalAddress(String address, String fieldName, int maxLength) {
        if (address == null || address.trim().isEmpty()) {
            return this;
        }
        String t = address.trim();
        if (t.length() > maxLength) {
            errors.add(fieldName + " must not exceed " + maxLength + " characters");
            return this;
        }
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c < 32 && c != '\t' && c != '\n' && c != '\r') {
                errors.add(fieldName + " contains invalid characters");
                return this;
            }
        }
        return this;
    }

    /**
     * Validates quantity for cart/order lines (upper bound aligned with {@link com.fashionstore.util.ValidationUtil}).
     */
    public Validator validateProductQuantity(int quantity, String fieldName) {
        if (quantity < 1) {
            errors.add(fieldName + " must be at least 1");
        } else if (quantity > ValidationUtil.MAX_PRODUCT_QUANTITY_PER_LINE) {
            errors.add(fieldName + " cannot exceed " + ValidationUtil.MAX_PRODUCT_QUANTITY_PER_LINE);
        }
        return this;
    }
    
    /**
     * Validates address fields (consolidated from AddressValidator)
     */
    public Validator validateAddress(String fullName, String phone, String addressLine1, String city, String state, String postalCode, String country) {
        Pattern phoneInPattern = Pattern.compile("^[6-9]\\d{9}$");
        Pattern phoneGenericPattern = Pattern.compile("^[+]?\\d{7,15}$");
        Pattern pinPattern = Pattern.compile("^\\d{6}$");
        Pattern postalGenericPattern = Pattern.compile("^[A-Za-z0-9 -]{3,12}$");
        Pattern namePattern = Pattern.compile("^[\\p{L} .'-]{2,100}$");
        
        // Validate full name
        if (fullName == null || fullName.trim().isEmpty()) {
            errors.add("Full name is required");
        } else if (fullName.length() < 2 || fullName.length() > 100) {
            errors.add("Full name must be between 2 and 100 characters");
        } else if (!namePattern.matcher(fullName).matches()) {
            errors.add("Full name contains invalid characters");
        }
        
        // Validate phone
        if (phone == null || phone.trim().isEmpty()) {
            errors.add("Phone number is required");
        } else if ("India".equalsIgnoreCase(country)) {
            if (!phoneInPattern.matcher(phone).matches()) {
                errors.add("Enter a valid 10-digit Indian mobile number");
            }
        } else if (!phoneGenericPattern.matcher(phone).matches()) {
            errors.add("Enter a valid phone number (7-15 digits)");
        }
        
        // Validate address line 1
        if (addressLine1 == null || addressLine1.trim().isEmpty()) {
            errors.add("Address line 1 is required");
        } else if (addressLine1.length() > 255) {
            errors.add("Address line 1 must be 255 characters or less");
        }
        
        // Validate city
        if (city == null || city.trim().isEmpty()) {
            errors.add("City is required");
        } else if (city.length() > 100) {
            errors.add("City must be 100 characters or less");
        }
        
        // Validate state
        if (state == null || state.trim().isEmpty()) {
            errors.add("State is required");
        } else if (state.length() > 100) {
            errors.add("State must be 100 characters or less");
        }
        
        // Validate postal code
        if (postalCode == null || postalCode.trim().isEmpty()) {
            errors.add("Postal code is required");
        } else if ("India".equalsIgnoreCase(country)) {
            if (!pinPattern.matcher(postalCode).matches()) {
                errors.add("Enter a valid 6-digit Indian PIN code");
            }
        } else if (!postalGenericPattern.matcher(postalCode).matches()) {
            errors.add("Enter a valid postal code");
        }
        
        // Validate country
        if (country == null || country.trim().isEmpty()) {
            errors.add("Country is required");
        } else if (country.length() > 100) {
            errors.add("Country must be 100 characters or less");
        }
        
        return this;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
    
    public static Validator create() {
        return new Validator();
    }
}
