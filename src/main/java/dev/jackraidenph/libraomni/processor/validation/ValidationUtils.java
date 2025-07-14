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

    public static SequencedCollection<String> getAssignableTypes(Element e) {
        if (!(e instanceof TypeElement type)) {
            throw new IllegalArgumentException();
        }

        SequencedCollection<String> hierarchy = new LinkedHashSet<>();

        do {
            hierarchy.addLast(type.toString());
            for (TypeMirror i : type.getInterfaces()) {
                hierarchy.addLast(i.toString());
            }
            if (!(type.getSuperclass() instanceof DeclaredType declaredType) || !(declaredType instanceof TypeElement typeElement)) {
                break;
            }
            type = typeElement;
        } while (!isObject(type));

        hierarchy.addLast(OBJECT_STR);

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
        return !Collections.disjoint(Set.of(classNames), getAssignableTypes(getType(e)));
    }

    public static boolean constructorMatches(Element e, String... typeParameters) {
        if (!e.getKind().equals(ElementKind.CLASS)) {
            return false;
        }

        return e.getEnclosedElements().stream()
                .filter(enc -> enc.getKind().equals(ElementKind.CONSTRUCTOR))
                .map(enc -> ((ExecutableElement) enc).getParameters())
                .anyMatch((params) -> {
                    if (typeParameters.length != params.size()) {
                        return false;
                    }

                    for (int i = 0; i < typeParameters.length; i++) {
                        if (!getAssignableTypes(params.get(i)).contains(typeParameters[i])) {
                            return false;
                        }
                    }

                    return true;
                });
    }
}
