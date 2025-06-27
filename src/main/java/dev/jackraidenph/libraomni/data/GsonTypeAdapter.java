package dev.jackraidenph.libraomni.data;

import com.google.gson.*;

import javax.lang.model.element.ElementKind;
import java.lang.reflect.Type;

public class GsonTypeAdapter {

    public static class AnnotatedReflectionDataSerializer implements JsonSerializer<AnnotatedReflectionData<?>> {

        public JsonElement serialize(AnnotatedReflectionData src, Type member, JsonSerializationContext context) {
            JsonElement element = context.serialize(src);
            JsonObject jsonObject = element.getAsJsonObject();
            jsonObject.add("kind", new JsonPrimitive(src.kind().toString()));
            return jsonObject;
        }
    }

    public static class AnnotatedReflectionDataDeserializer implements JsonDeserializer<AnnotatedReflectionData<?>> {
        @Override
        public AnnotatedReflectionData<?> deserialize(JsonElement json, Type member, JsonDeserializationContext context) {
            String kindStr = json.getAsJsonObject().get("kind").getAsString();
            ElementKind elementKind = ElementKind.valueOf(kindStr);

            return switch (elementKind) {
                case CLASS -> context.deserialize(json, ClassData.class);
                case FIELD -> context.deserialize(json, FieldData.class);
                case METHOD -> context.deserialize(json, MethodData.class);
                case CONSTRUCTOR -> context.deserialize(json, ConstructorData.class);
                default -> throw new UnsupportedOperationException();
            };
        }
    }

}
