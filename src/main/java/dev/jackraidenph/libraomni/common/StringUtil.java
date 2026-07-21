package dev.jackraidenph.libraomni.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public final class StringUtil {

    private StringUtil() {
        
    }

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

    private static String[] splitNamespaceAndPath(String string, String defaultNamespace) {
        if (string.isBlank()) {
            return new String[0];
        } else {
            if (string.indexOf(':') < 0) {
                return new String[]{defaultNamespace, string};
            } else {
                return string.split(":");
            }
        }
    }

    private static String[] splitDirectoryAndFile(String root, String string) {
        int dirSeparatorIndex = string.lastIndexOf('/');
        if (dirSeparatorIndex > 0) {
            return new String[]{
                    root + '/' + string.substring(0, dirSeparatorIndex),
                    string.substring(dirSeparatorIndex + 1)
            };
        }
        return new String[]{root, string};
    }

    public static NamespaceDirectoryFile splitToNamespaceDirFilename(String str, String defaultNamespace, String dirRoot) {
        String[] namespacePath = splitNamespaceAndPath(str, defaultNamespace);
        String namespace = namespacePath[0];
        String[] dirFile = splitDirectoryAndFile(dirRoot, namespacePath[1]);
        String dir = dirFile[0];
        String file = dirFile[1];
        return new NamespaceDirectoryFile(namespace, dir, file);
    }

    public record NamespaceDirectoryFile(String namespace, String directory, String file) {
    }

    public static String intArrayToHex(int[] arr) {
        String[] res = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            res[i] = Integer.toHexString(arr[i]);
        }
        return Arrays.toString(res);
    }

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
