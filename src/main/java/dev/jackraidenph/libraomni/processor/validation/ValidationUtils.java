package dev.jackraidenph.libraomni.processor.validation;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.*;

class ValidationUtils {

    private static final String OBJECT_STR = Object.class.getName();

    private static boolean isObject(Element e) {
        return e.toString().equals(OBJECT_STR);
    }

    public static List<String> assignableTo(Element e) {
        TypeElement type = (TypeElement) e;
        List<String> hierarchy = new ArrayList<>();

        try {
            do {
                for (TypeMirror i : type.getInterfaces()) {
                    hierarchy.add(i.toString());
                }
                hierarchy.add(type.toString());
                type = ((TypeElement) ((DeclaredType) type.getSuperclass()).asElement());
            } while (!isObject(type));
        } catch (ClassCastException castException) {
            throw new IllegalArgumentException(castException);
        }

        hierarchy.add(OBJECT_STR);

        return hierarchy;
    }

    public static Element getType(Element e) {
        return ((DeclaredType) switch (e) {
            case TypeElement typeElement -> typeElement.asType();
            case ExecutableElement executableElement -> executableElement.getReturnType();
            case VariableElement variableElement -> variableElement.asType();
            case null, default -> throw new UnsupportedOperationException();
        }).asElement();
    }

    public static boolean elementImplementsOrExtendsAny(
            Element e,
            String... classNames
    ) {
        if (e.toString().equals(OBJECT_STR)) {
            return true;
        }
        return !Collections.disjoint(Set.of(classNames), assignableTo(getType(e)));
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
