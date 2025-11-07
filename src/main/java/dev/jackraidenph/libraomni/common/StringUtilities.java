package dev.jackraidenph.libraomni.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

//    public static String firstNotBlank(String... variants) {
//        for (String variant : variants) {
//            if (variant != null && !variant.isBlank()) {
//                return variant;
//            }
//        }
//
//        return "";
//    }

    public static String quote(String str) {
        return "\"" + str + "\"";
    }

    public static String makeNamespacedId(@Nullable String namespace, @Nonnull String id) {
        return makeNamespacedId(namespace, null, id);
    }

    public static String makeNamespacedId(@Nullable String namespace, @Nullable String defaultNamespace, @Nonnull String id) {
        String res = id;
        if (namespace != null && !namespace.isBlank()) {
            res = namespace + ':' + id;
        } else if (defaultNamespace != null && !defaultNamespace.isBlank()) {
            res = defaultNamespace + ':' + id;
        }
        return res;
    }
}
