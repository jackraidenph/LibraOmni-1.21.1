package dev.jackraidenph.libraomni.annotation.compile.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.annotation.compile.util.dto.ExecutableData;
import dev.jackraidenph.libraomni.annotation.compile.util.dto.TypeData;
import dev.jackraidenph.libraomni.annotation.compile.util.dto.VariableData;

import javax.lang.model.element.*;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ElementData {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .registerTypeAdapterFactory(EmptySetToNullFactory.INSTANCE)
            .create();

    private static final String FILE_NAME_SUFFIX = "elements";

    private final Set<TypeData> classes = new HashSet<>();
    private final Set<VariableData> fields = new HashSet<>();
    private final Set<ExecutableData> methods = new HashSet<>();
    private final Set<ExecutableData> constructors = new HashSet<>();

    private final String modId;

    public ElementData(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    public boolean isEmpty() {
        return classes.isEmpty() && fields.isEmpty() && methods.isEmpty() && constructors.isEmpty();
    }

    public void output(OutputStream outputStream) throws IOException {
        String str = GSON.toJson(this);
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes);
    }

    public static ElementData fromJson(String str) {
        return GSON.fromJson(str, ElementData.class);
    }

    public String fileName() {
        return this.getModId() + "." + FILE_NAME_SUFFIX + ".json";
    }

    public void addElement(Element element) {
        switch (element) {
            case TypeElement typeElement -> this.addClass(typeElement);
            case VariableElement variableElement -> this.addField(variableElement);
            case ExecutableElement executableElement -> {
                if (executableElement.getKind().equals(ElementKind.METHOD)) {
                    this.addMethod(executableElement);
                } else if (executableElement.getKind().equals(ElementKind.CONSTRUCTOR)) {
                    this.addConstructor(executableElement);
                }

            }
            default -> throw new IllegalArgumentException("Not supported");
        }
    }

    public void addElements(Element... elements) {
        for (Element e : elements) {
            this.addElement(e);
        }
    }

    public void addClass(TypeElement typeElement) {
        this.classes.add(new TypeData(typeElement));
    }

    public void addField(VariableElement variableElement) {
        this.fields.add(new VariableData(variableElement));
    }

    public void addMethod(ExecutableElement executableElement) {
        this.methods.add(new ExecutableData(executableElement));
    }

    public void addConstructor(ExecutableElement executableElement) {
        this.constructors.add(new ExecutableData(executableElement));
    }

    public Set<Class<?>> getClasses() {
        return classes.stream().map(TypeData::asClass).collect(Collectors.toSet());
    }

    public Set<Field> getFields() {
        return fields.stream().map(VariableData::asField).collect(Collectors.toSet());
    }

    public Set<Method> getMethods() {
        return methods.stream().map(ExecutableData::asMethod).collect(Collectors.toSet());
    }

    public Set<Constructor<?>> getConstructors() {
        return constructors.stream().map(ExecutableData::asConstructor).collect(Collectors.toSet());
    }

    public Set<AnnotatedElement> getElements() {
        Set<AnnotatedElement> elements = new HashSet<>();
        elements.addAll(this.getClasses());
        elements.addAll(this.getFields());
        elements.addAll(this.getMethods());
        elements.addAll(this.getConstructors());

        return elements;
    }

}
