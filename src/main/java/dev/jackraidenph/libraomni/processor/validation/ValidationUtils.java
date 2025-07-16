package dev.jackraidenph.libraomni.processor.validation;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
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
            if (!(type.getSuperclass() instanceof DeclaredType declaredType) || !(declaredType.asElement() instanceof TypeElement typeElement)) {
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

    public static List<ExecutableElement> methodsInElement(TypeElement element) {
        return element.getEnclosedElements().stream()
                .filter(e -> e.getKind().isExecutable())
                .map(e -> (ExecutableElement) e)
                .toList();
    }

    public static List<ExecutableElement> abstractMethods(TypeElement typeElement) {
        return methodsInElement(typeElement).stream()
                .filter(m -> m.getModifiers().contains(Modifier.ABSTRACT))
                .toList();
    }

    public static boolean isFunctionalInterface(TypeElement typeElement) {
        return typeElement.getKind().isInterface() && abstractMethods(typeElement).size() == 1;
    }

    public static ExecutableElement getFunction(TypeElement typeElement) {
        if (!isFunctionalInterface(typeElement)) {
            throw new IllegalArgumentException("Not a functional interface");
        }

        return abstractMethods(typeElement).getFirst();
    }

    private static final String DEFERRED_HOLDER_CLASS = "net.neoforged.neoforge.registries.DeferredHolder";

    public static Element tryResolveDeferredHolder(Element e) {
        if (!(e instanceof VariableElement variableElement)) {
            return null;
        }

        if (!(variableElement.asType() instanceof DeclaredType declaredType)) {
            return null;
        }

        if (!(declaredType.asElement().toString().equals(DEFERRED_HOLDER_CLASS))) {
            return null;
        }

        return ((DeclaredType) declaredType.getTypeArguments().getFirst()).asElement();
    }

    public static TypeMirror resolveFunctionalReturnType(Element e, Types types) {
        if (!(e.getKind().isField() && e.getModifiers().contains(Modifier.STATIC))) {
            return null;
        }

        VariableElement variableElement = (VariableElement) e;
        TypeMirror typeMirror = variableElement.asType();

        if (!(typeMirror instanceof DeclaredType declaredType) || !(declaredType.asElement() instanceof TypeElement typeElement)) {
            return null;
        }

        if (!ValidationUtils.isFunctionalInterface(typeElement)) {
            return null;
        }

        return ValidationUtils.getFunctionalInterfaceReturnType(types, declaredType);
    }

    private static TypeMirror getFunctionalInterfaceReturnType(Types types, DeclaredType typeElement) {
        TypeMirror memberType = types.asMemberOf(typeElement, ValidationUtils.getFunction((TypeElement) typeElement.asElement()));
        return ((ExecutableType) memberType).getReturnType();
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
