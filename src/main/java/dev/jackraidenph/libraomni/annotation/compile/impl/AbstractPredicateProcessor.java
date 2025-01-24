package dev.jackraidenph.libraomni.annotation.compile.impl;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public abstract class AbstractPredicateProcessor extends AbstractCompileTimeProcessor {

    public AbstractPredicateProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    @Override
    public boolean onRound(RoundEnvironment roundEnvironment) {
        for (Class<? extends Annotation> annotation : this.getSupportedAnnotationClasses()) {
            for (Element e : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                for (PredicateWithDescription<Element> predicate : this.getPredicatesAndDescriptions()) {
                    if (!predicate.predicate().test(e)) {
                        this.getProcessingEnvironment().getMessager().printError("""
                                Annotation conditions are not satisfied
                                Annotation: %s
                                Details: %s
                                """.formatted(annotation.toString(), predicate.description()), e);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public List<PredicateWithDescription<Element>> getPredicatesAndDescriptions() {
        return List.of();
    }

    public record PredicateWithDescription<T>(Predicate<T> predicate, String description) {
        public static PredicateWithDescription<Element> mustBeOn(ElementKind... elementKinds) {
            return new PredicateWithDescription<>(
                    e -> {
                        for (ElementKind kind : elementKinds) {
                            if (e.getKind().equals(kind)) {
                                return true;
                            }
                        }
                        return false;
                    },
                    "Annotation must be applied to %s".formatted(Arrays.asList(elementKinds))
            );
        }

        public static PredicateWithDescription<Element> mustExtend(String... classNames) {
            return new PredicateWithDescription<>(
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

        public static PredicateWithDescription<Element> MUST_BE_ON_CLASS = mustBeOn(ElementKind.CLASS);
        public static PredicateWithDescription<Element> MUST_BE_ON_FIELD = mustBeOn(ElementKind.FIELD);
        public static PredicateWithDescription<Element> MUST_BE_ON_METHOD = mustBeOn(ElementKind.METHOD);
        public static PredicateWithDescription<Element> MUST_BE_ON_CONSTRUCTOR = mustBeOn(ElementKind.CONSTRUCTOR);
    }
}
