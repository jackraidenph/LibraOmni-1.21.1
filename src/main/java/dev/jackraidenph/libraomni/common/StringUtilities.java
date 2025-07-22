package dev.jackraidenph.libraomni.common;

public class StringUtilities {
    public static String snakeCase(String string) {
        if (string == null) {
            return null;
        }

        if (string.isBlank()) {
            return "";
        }

        return string
                .strip()
                .replaceAll("\\s+", "_")
                .replaceAll("(\\p{Lower})([\\p{Upper}\\d])", "$1_$2")
                .toLowerCase();
    }

    public static String firstNotBlank(String... variants) {
        for (String variant : variants) {
            if (variant != null && variant.isBlank()) {
                return variant;
            }
        }

        return "";
    }

    public static String quote(String str) {
        return "\"" + str + "\"";
    }
}
