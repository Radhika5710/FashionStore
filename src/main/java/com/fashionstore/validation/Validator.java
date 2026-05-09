package com.fashionstore.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Validator {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]{2,50}$");
    
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
        if (phone != null && !phone.trim().isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            errors.add(fieldName + " must be a valid 10-digit Indian phone number");
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
        
        if (password.length() < 8) {
            errors.add(fieldName + " must be at least 8 characters long");
        }
        
        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpperCase = true;
            if (Character.isLowerCase(c)) hasLowerCase = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        
        if (!hasUpperCase) {
            errors.add(fieldName + " must contain at least one uppercase letter");
        }
        if (!hasLowerCase) {
            errors.add(fieldName + " must contain at least one lowercase letter");
        }
        if (!hasDigit) {
            errors.add(fieldName + " must contain at least one digit");
        }
        
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
