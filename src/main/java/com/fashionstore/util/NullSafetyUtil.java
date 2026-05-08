package com.fashionstore.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for null-safe operations throughout the application
 * Prevents NullPointerExceptions with defensive programming patterns
 */
public class NullSafetyUtil {
    
    /**
     * Safely get request parameter with default value
     */
    public static String safeGetParameter(HttpServletRequest request, String name, String defaultValue) {
        if (request == null || name == null) {
            return defaultValue;
        }
        String value = request.getParameter(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Safely get request parameter as integer with default value
     */
    public static int safeGetIntParameter(HttpServletRequest request, String name, int defaultValue) {
        String value = safeGetParameter(request, name, null);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely get request parameter as double with default value
     */
    public static double safeGetDoubleParameter(HttpServletRequest request, String name, double defaultValue) {
        String value = safeGetParameter(request, name, null);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely get session attribute with null check
     */
    @SuppressWarnings("unchecked")
    public static <T> T safeGetSessionAttribute(HttpSession session, String name, Class<T> type) {
        if (session == null || name == null) {
            return null;
        }
        Object value = session.getAttribute(name);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Safely get session attribute with default value
     */
    public static <T> T safeGetSessionAttribute(HttpSession session, String name, Class<T> type, T defaultValue) {
        T value = safeGetSessionAttribute(session, name, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Safely get request attribute with null check
     */
    @SuppressWarnings("unchecked")
    public static <T> T safeGetRequestAttribute(HttpServletRequest request, String name, Class<T> type) {
        if (request == null || name == null) {
            return null;
        }
        Object value = request.getAttribute(name);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Safely get string with empty check
     */
    public static String safeString(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
    
    /**
     * Safely get first element from list
     */
    public static <T> T safeFirstElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
    
    /**
     * Safely get first element with default
     */
    public static <T> T safeFirstElement(List<T> list, T defaultValue) {
        T first = safeFirstElement(list);
        return first != null ? first : defaultValue;
    }
    
    /**
     * Safely check if collection is null or empty
     */
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    /**
     * Safely check if string is null or empty
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Safely execute function with null check
     */
    public static <T, R> R safeApply(T input, Function<T, R> function, R defaultValue) {
        if (input == null) {
            return defaultValue;
        }
        try {
            R result = function.apply(input);
            return result != null ? result : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely execute supplier with exception handling
     */
    public static <T> T safeGet(Supplier<T> supplier, T defaultValue) {
        try {
            T result = supplier.get();
            return result != null ? result : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely convert string to integer
     */
    public static Integer safeParseInt(String value, Integer defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely convert string to double
     */
    public static Double safeParseDouble(String value, Double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely convert string to long
     */
    public static Long safeParseLong(String value, Long defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely truncate string to max length
     */
    public static String safeTruncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
    
    /**
     * Safely get collection size
     */
    public static int safeSize(Collection<?> collection) {
        return collection != null ? collection.size() : 0;
    }
    
    /**
     * Safely get string length
     */
    public static int safeLength(String value) {
        return value != null ? value.length() : 0;
    }
    
    /**
     * Safely equals comparison
     */
    public static boolean safeEquals(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
    
    /**
     * Safely compare strings ignoring case
     */
    public static boolean safeEqualsIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }
    
    /**
     * Safely return empty list instead of null
     */
    public static <T> List<T> safeList(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
    
    /**
     * Safely get from Optional with default
     */
    public static <T> T safeOptional(Optional<T> optional, T defaultValue) {
        return optional != null ? optional.orElse(defaultValue) : defaultValue;
    }
    
    /**
     * Validate that value is not null or throw exception
     */
    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        return value;
    }
    
    /**
     * Validate that string is not null or empty
     */
    public static String requireNonEmpty(String value, String fieldName) {
        if (isNullOrEmpty(value)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }
    
    /**
     * Validate that number is positive
     */
    public static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
    
    /**
     * Validate that number is non-negative
     */
    public static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
        return value;
    }
    
    /**
     * Check if any value in varargs is null
     */
    public static boolean anyNull(Object... values) {
        if (values == null) {
            return true;
        }
        for (Object value : values) {
            if (value == null) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if all values in varargs are null
     */
    public static boolean allNull(Object... values) {
        if (values == null) {
            return true;
        }
        for (Object value : values) {
            if (value != null) {
                return false;
            }
        }
        return true;
    }
}
