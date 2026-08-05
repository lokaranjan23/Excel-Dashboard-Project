package com.filemanagement.util;

import org.springframework.stereotype.Component;

@Component
public class StringUtil {

    public static String toTitleCase(String value) {

        if (value == null || value.isBlank()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase()
                + value.substring(1).toLowerCase();
    }
}
