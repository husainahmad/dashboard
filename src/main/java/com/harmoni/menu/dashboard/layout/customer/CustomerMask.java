package com.harmoni.menu.dashboard.layout.customer;

/**
 * Privacy helpers for the customer list. Phone numbers keep a short prefix and
 * suffix so the list stays readable without exposing the full value.
 */
final class CustomerMask {

    private static final char MASK = '•';

    private CustomerMask() {
        throw new IllegalStateException("Utility class");
    }

    static String phone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String value = phone.trim();
        int length = value.length();
        if (length <= 4) {
            return mask(length);
        }
        if (length <= 7) {
            return value.substring(0, 2) + mask(length - 4) + value.substring(length - 2);
        }
        return value.substring(0, 4) + mask(length - 7) + value.substring(length - 3);
    }

    static String initials(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            return "?";
        }
        String[] parts = value.split("[\\s._-]+");
        if (parts.length > 1 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return value.length() > 1 ? value.substring(0, 2).toUpperCase() : value.toUpperCase();
    }

    private static String mask(int length) {
        return String.valueOf(MASK).repeat(Math.max(length, 0));
    }
}
