package dev.jackraidenph.libraomni.annotation.runprocessing.proceesor.management;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.jackraidenph.libraomni.annotation.runprocessing.ReferenceMapDeserializationHelper;

import javax.lang.model.element.ElementKind;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ProcessableElementsStorage {

    private final Gson gson = new Gson();

    private final List<AnnotatedElement> annotatedElements = new ArrayList<>();

    public ProcessableElementsStorage(InputStream referenceMapStream) {
        this.buildFromReferenceMap(referenceMapStream);
    }

    private void buildFromReferenceMap(InputStream referenceMap) {
        try {
            String str = new String(referenceMap.readAllBytes(), StandardCharsets.UTF_8);

            JsonObject jsonObject = gson.fromJson(str, JsonObject.class);
            Map<String, List<JsonElement>> annotatedObjectsMap = new HashMap<>();
            for (Entry<String, JsonElement> e : jsonObject.asMap().entrySet()) {
                List<JsonElement> elementList = e.getValue().getAsJsonArray().asList();
                annotatedObjectsMap.put(e.getKey(), elementList);
            }

            for (Entry<String, List<JsonElement>> e : annotatedObjectsMap.entrySet()) {
                Class<? extends Annotation> annotation =
                        (Class<? extends Annotation>) ReferenceMapDeserializationHelper.deserializeClass(e.getKey());

                for (JsonElement element : e.getValue()) {
                    Map<String, JsonElement> annotatedObjectProperties = element.getAsJsonObject().asMap();

                    ElementKind elementKind = ElementKind.valueOf(annotatedObjectProperties.get("kind").getAsString());
                    String qualifier = annotatedObjectProperties.get("qualifier").getAsString();

                    Map<String, JsonElement> additional = annotatedObjectProperties.get("additionalParameters")
                            .getAsJsonObject()
                            .asMap();
                    List<JsonElement> parameterTypesElements = additional.containsKey("parameterTypes") ? additional.get("parameterTypes").getAsJsonArray().asList() : null;
                    String clazz = additional.containsKey("class") ? additional.get("class").getAsString() : null;

                    this.addAnnotatedElement(elementKind, annotation, qualifier, clazz, parameterTypesElements);
                }
            }

        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new IllegalArgumentException(jsonSyntaxException);
        }
    }

    public List<AnnotatedElement> getAnnotatedElements() {
        return new ArrayList<>(this.annotatedElements);
    }

    private void addAnnotatedElement(
            ElementKind elementKind,
            Class<? extends Annotation> annotation,
            String qualifier,
            String outerClass,
            List<JsonElement> parameterTypes
    ) {
        switch (elementKind) {
            case CLASS, ANNOTATION_TYPE -> this.addAnnotatedClass(annotation, qualifier);
            case FIELD -> this.addAnnotatedField(annotation, qualifier, outerClass);
            case METHOD -> this.addAnnotatedMethod(annotation, qualifier, outerClass, parameterTypes);
            case CONSTRUCTOR -> this.addAnnotatedConstructor(annotation, outerClass, parameterTypes);
        }
    }

    private void addAnnotatedClass(Class<? extends Annotation> annotation, String qualifier) {
        this.annotatedElements.add(
                new AnnotatedElement(
                        annotation,
                        ReferenceMapDeserializationHelper.deserializeClass(qualifier)
                )
        );
    }

    private void addAnnotatedField(Class<? extends Annotation> annotation, String qualifier, String clazz) {
        this.annotatedElements.add(
                new AnnotatedElement(
                        annotation,
                        ReferenceMapDeserializationHelper.deserializeField(qualifier, clazz)
                )
        );
    }

    private void addAnnotatedMethod(
            Class<? extends Annotation> annotation,
            String qualifier,
            String clazz,
            List<JsonElement> parameterTypesElements
    ) {
        List<String> parameterTypes = new ArrayList<>();
        for (JsonElement jsonElement : parameterTypesElements) {
            parameterTypes.add(jsonElement.getAsString());
        }

        this.annotatedElements.add(
                new AnnotatedElement(
                        annotation,
                        ReferenceMapDeserializationHelper.deserializeMethod(qualifier, clazz, parameterTypes)
                )
        );
    }

    private void addAnnotatedConstructor(
            Class<? extends Annotation> annotation,
            String clazz,
            List<JsonElement> parameterTypesElements
    ) {
        List<String> parameterTypes = new ArrayList<>();
        for (JsonElement jsonElement : parameterTypesElements) {
            parameterTypes.add(jsonElement.getAsString());
        }

        this.annotatedElements.add(
                new AnnotatedElement(
                        annotation,
                        ReferenceMapDeserializationHelper.deserializeConstructor(clazz, parameterTypes)
                )
        );
    }

    public record AnnotatedElement(Class<? extends Annotation> annotation, Object element) {

    }

}
