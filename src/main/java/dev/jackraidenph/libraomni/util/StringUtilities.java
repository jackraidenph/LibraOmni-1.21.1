package dev.jackraidenph.libraomni.util;

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
}
