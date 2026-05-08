package com.fashionstore.util;

import java.util.regex.Pattern;

public class XSSUtil {

    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("src=['\"]javascript:(.*?)['\"]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+=\"[^\"]*\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+='[^']*'", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(<[^>]+)(style=[\\\"\\'].*?expression\\(.*?\\);[\\\"\\'])([^>]*)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(<[^>]+)(style=[\\\"\\'].*?behaviour\\(.*?\\);[\\\"\\'])([^>]*)", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Sanitize input to prevent XSS attacks
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        String clean = input;

        // Remove dangerous patterns
        for (Pattern pattern : XSS_PATTERNS) {
            clean = pattern.matcher(clean).replaceAll("");
        }

        // HTML encode the output
        clean = escapeHtml(clean);

        return clean;
    }

    /**
     * Sanitize input for use in URL parameters
     */
    public static String sanitizeForURL(String input) {
        if (input == null) {
            return null;
        }
        return escapeHtml(input).replaceAll(" ", "+");
    }

    /**
     * Sanitize input for use in JavaScript
     */
    public static String sanitizeForJS(String input) {
        if (input == null) {
            return null;
        }
        return escapeJavaScript(input);
    }

    /**
     * Validate if input contains potential XSS
     */
    public static boolean containsXSS(String input) {
        if (input == null) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Strip HTML tags from input
     */
    public static String stripHTML(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "");
    }

    /**
     * Simple HTML escaping
     */
    private static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * Simple JavaScript escaping
     */
    private static String escapeJavaScript(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("'", "\\'")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
