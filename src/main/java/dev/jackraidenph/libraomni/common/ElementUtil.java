package dev.jackraidenph.libraomni.common;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

public final class ElementUtil {

    private static final String OBJECT_STR = Object.class.getName();

    // Might be deleted later

//    /**
//     * @param type TypeElement to retrieve type hierarchy from
//     * @return a SequencedCollection of type names in the hierarchy, starting from the type itself and ending with java.lang.Object, interfaces included
//     */
//    public static SequencedCollection<String> getTypeHierarchy(TypeElement type) {
//        SequencedCollection<String> hierarchy = new LinkedHashSet<>();
//
//        do {
//            hierarchy.addLast(type.toString());
//            for (TypeMirror i : type.getInterfaces()) {
//                hierarchy.addLast(i.toString());
//            }
//            if (!(type.getSuperclass() instanceof DeclaredType declaredType)
//                    || !(declaredType.asElement() instanceof TypeElement typeElement)) {
//                break;
//            }
//            type = typeElement;
//        } while (type.getKind().isDeclaredType());
//
//        hierarchy.addLast(OBJECT_STR);
//
//        return hierarchy;
//    }

    /**
     * @param e Element
     * @return Self if TypeElement, return type if ExecutableElement, or variable type if VariableElement
     * @throws UnsupportedOperationException if supplied Element is not a TypeElement, an ExecutableElement, or a VariableElement
     */
    public static TypeElement getReturnType(Element e) {
        return (TypeElement) ((DeclaredType) switch (e) {
            case TypeElement typeElement -> typeElement.asType();
            case ExecutableElement executableElement -> executableElement.getReturnType();
            case VariableElement variableElement -> variableElement.asType();
            case null, default -> throw new UnsupportedOperationException();
        }).asElement();
    }

    /**
     * @param typeElement TypeElement to retrieve directly enclosed methods
     * @return List of ExecutableElements, which is a list of directly enclosed methods
     */
    public static List<ExecutableElement> getMethodsInElement(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind().isExecutable())
                .map(e -> (ExecutableElement) e)
                .toList();
    }

    /**
     * @param typeElement TypeElement to retrieve all enclosed methods
     * @return List of ExecutableElements, which is a list of all enclosed methods
     */
    public static List<ExecutableElement> getDeclaredMethodsInElement(TypeElement typeElement, Elements elements) {
        return elements.getAllMembers(typeElement).stream()
                .filter(e -> e.getKind().isExecutable())
                .map(e -> (ExecutableElement) e)
                .toList();
    }

    /**
     * @param typeElement TypeElement to retrieve a list of abstract methods from
     * @return List of directly present abstract methods
     */
    public static List<ExecutableElement> getAbstractMethods(TypeElement typeElement) {
        return getMethodsInElement(typeElement).stream()
                .filter(m -> m.getModifiers().contains(Modifier.ABSTRACT))
                .toList();
    }

    /**
     * @param typeElement TypeElement to retrieve a list of abstract methods from
     * @return List of directly present abstract methods
     */
    public static List<ExecutableElement> getDeclaredAbstractMethods(TypeElement typeElement, Elements elements) {
        return getDeclaredMethodsInElement(typeElement, elements).stream()
                .filter(m -> m.getModifiers().contains(Modifier.ABSTRACT))
                .toList();
    }

    /**
     * @param typeElement TypeElement to check
     * @return Whether the TypeElement is a functional interface. Meaning, whether it's an interface with exactly 1 abstract method
     */
    public static boolean isFunctionalInterface(TypeElement typeElement) {
        return typeElement.getKind().isInterface() && getAbstractMethods(typeElement).size() == 1;
    }

    /**
     * @param typeElement Functional interface to retrieve the function from
     * @return The sole abstract method (function) of the functional interface, in the form of ExecutableElement
     */
    public static ExecutableElement getFunction(TypeElement typeElement) {
        if (!isFunctionalInterface(typeElement)) {
            throw new IllegalArgumentException("Not a functional interface");
        }

        return getAbstractMethods(typeElement).getFirst();
    }

    /**
     * Resolves a functional interface's return type
     *
     * @param declaredType A DeclaredType of a functional interface
     * @param types        Java's set of utilities operating on Types
     * @return A TypeMirror of functional interface's function return type
     */
    private static TypeMirror getFunctionalInterfaceReturnType(DeclaredType declaredType, Types types) {
        TypeMirror memberType = types.asMemberOf(
                declaredType,
                ElementUtil.getFunction((TypeElement) declaredType.asElement())
        );
        return ((ExecutableType) memberType).getReturnType();
    }

    /**
     * @param typeElement TypeElement which's assignability to check
     * @param classNames  Class names to check assignability to
     * @return Whether the TypeElement is assignable to any of classes provided by className parameter
     */
    public static boolean isAssignableToAny(TypeElement typeElement, Elements elements, Types types, String... classNames) {
        if (typeElement.toString().equals(OBJECT_STR)) {
            return true;
        }
        for (String name : classNames) {
            TypeMirror typeMirrorToCheck = elements.getTypeElement(name).asType();
            if (types.isAssignable(typeElement.asType(), typeMirrorToCheck)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param constructor    ExecutableElement, a constructor, to check
     * @param typeParameters A sequence of canonical class names that must match constructor's type parameters
     * @return Whether the constructor matches a sequence of supplied type parameter canonical names
     */
    public static boolean constructorMatches(ExecutableElement constructor, Elements elements, Types types, String... typeParameters) {
        if (!constructor.getKind().equals(ElementKind.CONSTRUCTOR)) {
            return false;
        }
        List<? extends VariableElement> params = constructor.getParameters();
        for (int i = 0; i < params.size(); i++) {
            TypeMirror constructorParam = params.get(i).asType();
            String str = typeParameters[i];
            TypeMirror toCheckParam = elements.getTypeElement(str).asType();
            if (!types.isAssignable(toCheckParam, constructorParam)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param e              Element to check
     * @param typeParameters A sequence of canonical class names that must match constructor's type parameters
     * @return Whether the element contains a matching constructor
     */
    public static boolean hasConstructorWithParameters(Element e, Elements elements, Types types, String... typeParameters) {
        return e.getEnclosedElements().stream()
                .filter(enc -> enc.getKind().equals(ElementKind.CONSTRUCTOR))
                .map(enc -> ((ExecutableElement) enc))
                .anyMatch((constructor) -> constructorMatches(constructor, elements, types, typeParameters));
    }
}
