package com.documania.backend.common.util;

public final class TextUtils {

    private TextUtils() {}

    /** Retourne null si la valeur est vide/blank, sinon la valeur trimée. */
    public static String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
