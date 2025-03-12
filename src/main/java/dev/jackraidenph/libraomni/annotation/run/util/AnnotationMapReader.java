package dev.jackraidenph.libraomni.annotation.run.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.util.ResourceUtilities;

import dev.jackraidenph.libraomni.annotation.compile.util.SerializationHelper;

import javax.lang.model.element.ElementKind;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AnnotationMapReader {

    private AnnotationMapReader() {

    }

    private static final Type CLASS_MAP_TYPE = new TypeToken<Map<String, Map<String, List<String>>>>() {
    }.getType();

    private static final Gson GSON = new GsonBuilder().create();

    @SuppressWarnings("unchecked")
    public static void readElementsToStorage(String resourceLocation, ElementStorage elementStorage) throws NoSuchFieldException, NoSuchMethodException {
        Map<String, Map<String, List<String>>> annotationInfo = readFileToMap(resourceLocation);

        Set<AnnotatedElement> elements = new HashSet<>();

        //Read annotations
        for (String annotationClassString : annotationInfo.keySet()) {
            //Get annotated kinds
            Set<String> elementKindStrings = annotationInfo.get(annotationClassString).keySet();
            //For each kid
            for (String elementKindString : elementKindStrings) {
                ElementKind elementKind = ElementKind.valueOf(elementKindString);
                //Get annotated elements of the kind
                for (String objectString : annotationInfo.get(annotationClassString).get(elementKindString)) {
                    AnnotatedElement annotatedElement = parseAnnotatedElement(elementKind, objectString);
                    elements.add(annotatedElement);
                }
            }
        }

        elementStorage.set(elements);
    }

    private static AnnotatedElement parseAnnotatedElement(ElementKind elementKind, String objectString)
            throws NoSuchMethodException, NoSuchFieldException {
        return (AnnotatedElement) SerializationHelper.parse(elementKind, objectString);
    }

    private static Map<String, Map<String, List<String>>> readFileToMap(String resourceLocation) {
        try (InputStream inputStream = ResourceUtilities.openResourceStream(resourceLocation)) {
            if (inputStream == null) {
                throw new IOException("Failed to open " + resourceLocation);
            }
            String contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            return GSON.fromJson(contents, CLASS_MAP_TYPE);
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to read " + resourceLocation, ioException);
        }
    }

}
