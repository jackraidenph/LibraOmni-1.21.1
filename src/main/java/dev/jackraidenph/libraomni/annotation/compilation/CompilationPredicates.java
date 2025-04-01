package dev.jackraidenph.libraomni.annotation.compilation;

import dev.jackraidenph.libraomni.annotation.compilation.AbstractCompilationProcessor.CompilationPredicate;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

class CompilationPredicates {

    protected static CompilationPredicate<Element> mustAlsoBeAnnotatedWith(String... annotations) {
        return new CompilationPredicate<>(
                e -> appliedAnnotations(e).containsAll(Set.of(annotations)),
                "Annotation must be used alongside with " + Set.of(annotations)
        );
    }

    private static Set<String> appliedAnnotations(Element element) {
        List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
        Set<String> names = new HashSet<>();
        for (AnnotationMirror annotationMirror : mirrors) {
            names.add(
                    ((TypeElement) annotationMirror.getAnnotationType().asElement())
                            .getQualifiedName().toString()
            );
        }
        return names;
    }

    protected static CompilationPredicate<Element> parentMustBeAnnotatedWith(String... annotations) {
        return new CompilationPredicate<>(
                e -> appliedAnnotations(e.getEnclosingElement()).containsAll(Set.of(annotations)),
                "Parent element must be annotated with " + Set.of(annotations)
        );
    }

    protected static CompilationPredicate<Element> mustHaveMatchingConstructor(String... typeParameters) {
        return new CompilationPredicate<>(
                e -> {
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
                },
                typeParameters.length == 0
                        ? "Element must contain a no-arg constructor"
                        : "Element must contain a constructor with type parameters " + Set.of(typeParameters)
        );
    }

    protected static CompilationPredicate<Element> mustExtend(String... classNames) {
        return new CompilationPredicate<>(
                e -> {
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
                },
                "Annotation must be applied to a type extending any of %s".formatted(Arrays.toString(classNames))
        );
    }

    private static Predicate<TypeElement> directlyExtends(String className) {
        return e -> {
            TypeMirror superClass = e.getSuperclass();
            return superClass.toString().equals(className);
        };
    }
}
