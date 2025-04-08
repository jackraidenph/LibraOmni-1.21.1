package dev.jackraidenph.libraomni.annotation.compilation;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

public class ValidationUtils {

    public static boolean elementExtendsAny(Element e, String... classNames) {
        if (!e.getKind().isDeclaredType()) {
            return false;
        }

        Set<String> classNamesSet = Set.of(classNames);

        TypeElement typeElement = (TypeElement) e;
        final String objectName = Object.class.getName();
        String currentSuperclass = typeElement.getSuperclass().toString();
        while (!currentSuperclass.equals(objectName)) {
            if (classNamesSet.contains(currentSuperclass)) {
                return true;
            }
            typeElement = ((TypeElement) ((DeclaredType) typeElement.getSuperclass()).asElement());
            currentSuperclass = typeElement.getSuperclass().toString();
        }
        return false;
    }

    public static boolean elementImplementsAny(Element e, String... classNames) {
        if (!e.getKind().isDeclaredType()) {
            return false;
        }

        Set<String> classNamesSet = Set.of(classNames);

        TypeElement typeElement = (TypeElement) e;
        for (TypeMirror typeMirror : typeElement.getInterfaces()) {
            TypeElement interfaceType = ((TypeElement) ((DeclaredType) typeMirror).asElement());
            if (classNamesSet.contains(interfaceType.getQualifiedName().toString())) {
                return true;
            }
        }

        return false;
    }

    public static boolean constructorMatches(Element e, String... typeParameters) {
        if (!e.getKind().equals(ElementKind.CLASS)) {
            return false;
        }

        TypeElement typeElement = (TypeElement) e;
        List<? extends Element> constructors = typeElement.getEnclosedElements();
        constructors.removeIf(enc -> !enc.getKind().equals(ElementKind.CONSTRUCTOR));

        for (Element constructor : constructors) {
            ExecutableElement executable = (ExecutableElement) constructor;
            List<String> params = executable.getParameters()
                    .stream()
                    .map(tp -> tp.asType().toString())
                    .toList();

            if (typeParameters.length != params.size()) {
                continue;
            }

            boolean matches = true;
            for (int i = 0; i < typeParameters.length; i++) {
                if (!typeParameters[i].equals(params.get(i))) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return true;
            }
        }

        return false;
    }
}
