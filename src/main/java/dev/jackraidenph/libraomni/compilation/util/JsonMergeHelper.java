package dev.jackraidenph.libraomni.compilation.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jackraidenph.libraomni.util.CommonGson;

import java.util.Map.Entry;

public class JsonMergeHelper {

    public static String mergeJson(String json0, String json1, ConflictPolicy policy) {
        if (policy == ConflictPolicy.THROW) {
            throw new IllegalStateException();
        }

        JsonObject obj0 = CommonGson.DEFAULT.fromJson(json0, JsonObject.class);
        JsonObject obj1 = CommonGson.DEFAULT.fromJson(json1, JsonObject.class);

        return CommonGson.DEFAULT.toJson(mergeObjects(obj0, obj1, policy));
    }

    private static JsonObject mergeObjects(JsonObject obj0, JsonObject obj1, ConflictPolicy policy) {
        for (Entry<String, JsonElement> kvPair : obj1.entrySet()) {
            String rightKey = kvPair.getKey();
            JsonElement rightVal = kvPair.getValue();

            JsonElement left = obj0.get(rightKey);
            if (left == null) {
                obj0.add(rightKey, rightVal);
                continue;
            }

            if (left.isJsonArray()) {
                JsonArray leftArray = left.getAsJsonArray();
                if (rightVal.isJsonArray()) {
                    for (JsonElement e : rightVal.getAsJsonArray()) {
                        leftArray.add(e);
                    }
                } else {
                    leftArray.add(rightKey);
                }
                continue;
            }

            if (left.isJsonObject()) {
                if (!rightVal.isJsonObject()) {
                    throw new IllegalStateException(
                            "Tried to merge JsonObject with non-JsonObject [%s]: [%s], [%s]"
                                    .formatted(rightKey, left, rightVal)
                    );
                }

                obj0.remove(rightKey);
                obj0.add(rightKey, mergeObjects(left.getAsJsonObject(), rightVal.getAsJsonObject(), policy));
                continue;
            }

            if (policy == ConflictPolicy.MERGE_KEYS_PREFER_NEW) {
                obj0.remove(rightKey);
                obj0.add(rightKey, rightVal);
            }
        }

        return obj0;
    }

    public enum ConflictPolicy {
        THROW,
        OVERWRITE_FILE,
        MERGE_KEYS_PREFER_EXISTING,
        MERGE_KEYS_PREFER_NEW
    }
}
