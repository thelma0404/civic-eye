package com.civic.common.util;

import java.util.Locale;

/** Small helpers for cleaning user input. */
public final class TextUtil {

    private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private TextUtil() {
    }

    /** Trims the value and turns blank strings into null. */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalizeEmail(String email) {
        String cleaned = clean(email);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
