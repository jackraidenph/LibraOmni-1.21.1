package dev.jackraidenph.libraomni.processor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jackraidenph.libraomni.common.CommonGson;

import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import static dev.jackraidenph.libraomni.processor.Resource.JSON_EXT;

public class JsonMergeHelper {

    public static Resource mergeJson(Resource existing, Resource other, JsonMergeConflictPolicy policy) {
        if (existing == null || policy == JsonMergeConflictPolicy.OVERWRITE) {
            return other;
        }

        if (!existing.getExtension().equals(JSON_EXT) || !other.getExtension().equals(JSON_EXT)) {
            throw new IllegalArgumentException("Tried to merge non-JSON resources");
        }

        if (policy == JsonMergeConflictPolicy.THROW) {
            throw new IllegalStateException("Duplicate resource [%s]".formatted(existing));
        }

        String json0 = new String(existing.getContents(), StandardCharsets.UTF_8);
        String json1 = new String(other.getContents(), StandardCharsets.UTF_8);

        JsonObject obj0 = CommonGson.DEFAULT.fromJson(json0, JsonObject.class);
        JsonObject obj1 = CommonGson.DEFAULT.fromJson(json1, JsonObject.class);

        return Resource.json(mergeObjects(obj0, obj1, policy))
                .setNameRoot(existing.getNameRoot())
                .setDirectory(existing.getDirectory())
                .build();
    }

    private static JsonObject mergeObjects(JsonObject obj0, JsonObject obj1, JsonMergeConflictPolicy policy) {
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

            if (policy == JsonMergeConflictPolicy.PREFER_NEW) {
                obj0.remove(rightKey);
                obj0.add(rightKey, rightVal);
            }
        }

        return obj0;
    }

    public enum JsonMergeConflictPolicy {
        THROW,
        OVERWRITE,
        PREFER_EXISTING,
        PREFER_NEW
    }
}
