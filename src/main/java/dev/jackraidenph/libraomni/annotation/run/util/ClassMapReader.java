package dev.jackraidenph.libraomni.annotation.run.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.run.util.ClassMapReader.ElementStorage.AnnotatedElement;
import org.jetbrains.annotations.Nullable;

import dev.jackraidenph.libraomni.annotation.compile.util.SerializationHelper;

import javax.lang.model.element.ElementKind;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClassMapReader {

    private static final Type CLASS_MAP_TYPE = new TypeToken<Map<String, Map<String, List<String>>>>() {
    }.getType();

    private final Gson GSON = new GsonBuilder().create();

    private final String modId, resourceLocation;

    public ClassMapReader(String modId, String resourceLocation) {
        this.modId = modId;
        this.resourceLocation = resourceLocation;
    }

    @SuppressWarnings("unchecked")
    public ElementStorage readElements() {
        Map<String, Map<String, List<String>>> annotationInfo = this.readFileToMap();

        Map<Class<? extends Annotation>, Set<AnnotatedElement<?>>> map = new HashMap<>();

        for (String annotationClassString : annotationInfo.keySet()) {
            Class<? extends Annotation> annotation =
                    (Class<? extends Annotation>) SerializationHelper.INSTANCE.toClass(annotationClassString);

            Set<AnnotatedElement<?>> annotatedAnnotatedElements = new HashSet<>();

            Set<String> elementKindStrings = annotationInfo.get(annotationClassString).keySet();
            for (String elementKindString : elementKindStrings) {
                ElementKind elementKind = ElementKind.valueOf(elementKindString);
                for (String objectString : annotationInfo.get(annotationClassString).get(elementKindString)) {
                    AnnotatedElement<?> annotatedElement = this.getElement(elementKind, objectString);
                    annotatedAnnotatedElements.add(annotatedElement);
                }
            }

            map.put(annotation, annotatedAnnotatedElements);
        }

        return new ElementStorage(map);
    }

    private @Nullable AnnotatedElement<?> getElement(ElementKind elementKind, String objectString) {
        final SerializationHelper helper = SerializationHelper.INSTANCE;
        return switch (elementKind) {
            case CLASS -> new AnnotatedElement<Class<?>>(elementKind, helper.toClass(objectString));
            case FIELD -> new AnnotatedElement<Field>(elementKind, helper.toField(objectString));
            case METHOD -> new AnnotatedElement<Method>(elementKind, helper.toMethod(objectString));
            case CONSTRUCTOR -> new AnnotatedElement<Constructor<?>>(elementKind, helper.toConstructor(objectString));
            default -> null;
        };
    }

    private Map<String, Map<String, List<String>>> readFileToMap() {
        try (InputStream inputStream = LibraOmni.Utility.openResourceStream(this.resourceLocation)) {
            if (inputStream == null) {
                throw new IOException("Failed to fetch resource " + this.resourceLocation);
            }
            String contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            return this.GSON.fromJson(contents, CLASS_MAP_TYPE);
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to read class map for " + this.modId, ioException);
        }
    }

    public static class ElementStorage {
        private final Map<Class<? extends Annotation>, Set<AnnotatedElement<?>>> annotatedElementsMap;

        private ElementStorage(Map<Class<? extends Annotation>, Set<AnnotatedElement<?>>> annotatedElementsMap) {
            this.annotatedElementsMap = annotatedElementsMap;
        }

        public Set<Class<? extends Annotation>> getAnnotations() {
            return this.annotatedElementsMap.keySet();
        }

        public Set<AnnotatedElement<?>> getElements(Class<? extends Annotation> annotation) {
            return Set.copyOf(this.annotatedElementsMap.get(annotation));
        }

        @Override
        public String toString() {
            return this.annotatedElementsMap.toString();
        }

        public record AnnotatedElement<T>(ElementKind elementKind, T element) {
            public boolean isSubclassOf(Class<?> clazz) {
                return this.elementKind.isClass() && clazz.isAssignableFrom((Class<?>) element);
            }

            public Class<?> getEnclosingClass() {
                if (this.element() instanceof Class<?> clazz) {
                    return clazz.getEnclosingClass();
                } else if (this.element() instanceof Member member) {
                    return member.getDeclaringClass();
                }

                return null;
            }
        }
    }
}
