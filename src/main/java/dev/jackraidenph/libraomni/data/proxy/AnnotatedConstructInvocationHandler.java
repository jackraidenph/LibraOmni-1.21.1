package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.common.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotatedConstructInvocationHandler extends ObjectPreservingInvocationHandler<AnnotatedConstruct> {

    protected final Map<TypeElement, List<AnnotationMirror>> annotationMirrorsMap = new HashMap<>();
    protected final Map<Class<? extends Annotation>, List<Annotation>> annotationMap = new HashMap<>();
    private final Elements elementUtils;

    public AnnotatedConstructInvocationHandler(AnnotatedConstruct original, Elements elements) {
        super(original);
        elementUtils = elements;
        cacheRecursive(original);
    }

    private void cacheRecursive(AnnotatedConstruct original) {
        for (AnnotationMirror annotation : elementUtils.getAllAnnotationMirrors((Element) original)) {
            cacheStep(original, null, annotation, 0);
        }
    }

    private TypeElement addAnnotationMirror(AnnotationMirror mirror) {
        if (mirror == null) {
            return null;
        }
        TypeElement typeElement = AnnotationMirrorUtil.toTypeElement(mirror);
        if (!isOnlyDirect(mirror)) {
            annotationMirrorsMap.computeIfAbsent(typeElement, k -> new ArrayList<>()).add(mirror);
        }
        return typeElement;
    }

    private boolean isOnlyDirect(AnnotationMirror mirror) {
        return ProxyFactory.ONLY_DIRECT.stream().anyMatch(c -> AnnotationMirrorUtil.compareWithClass(mirror, c, elementUtils));
    }

    private void addAnnotation(DelegateContainer delegates, Annotation annotation) {
        if (ProxyFactory.ONLY_DIRECT.contains(annotation.annotationType())) {
            return;
        }

        boolean delegated = delegates != null && !delegates.isEmpty();
        Annotation proxyOrSelf = delegated ? ProxyFactory.proxifyAnnotation(annotation, delegates) : annotation;
        List<Annotation> annotations = annotationMap.computeIfAbsent(annotation.annotationType(), k -> new ArrayList<>());
        if (!annotations.isEmpty()) {
            boolean repeatable = annotation.annotationType().getAnnotation(Repeatable.class) != null;
            if (!repeatable) {
                throw new IllegalStateException("Annotation [%s] is not repeatable, previous encounter: [%s]".formatted(annotation, annotations.getFirst()));
            }
        }
        annotations.add(proxyOrSelf);
    }

    //Check if the annotation is a container for @Repeatable annotations specified as in https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
    private boolean isRepeatableContainer(AnnotationMirror mirror) {
        Object attributeValue = AnnotationMirrorUtil.getElementValue(mirror, "value");
        //Must be an array of annotations
        if (!(attributeValue instanceof List<?> list) || list.isEmpty()) {
            return false;
        }
        if (!(list.getFirst() instanceof AnnotationMirror inContainerMirror)) {
            return false;
        }

        TypeElement type = AnnotationMirrorUtil.toTypeElement(inContainerMirror);
        AnnotationMirror repeatableMirror = AnnotationMirrorUtil.findAnnotationMirror(type::getAnnotationMirrors, Repeatable.class.getName());
        if (repeatableMirror == null) {
            return false;
        }
        TypeMirror inRepeatableMirror = (TypeMirror) AnnotationMirrorUtil.getElementValue(repeatableMirror, "value");
        if (inRepeatableMirror == null) {
            return false;
        }
        return inRepeatableMirror.equals(mirror.getAnnotationType());
    }

    private List<AnnotationMirror> unwrapContainer(AnnotationMirror annotation) {
        if (!isRepeatableContainer(annotation)) {
            throw new IllegalArgumentException("Not a container for @Repeatable");
        }
        try {
            AnnotationValue value = annotation.getElementValues()
                    .entrySet().stream()
                    .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Repeatable container doesn't contain 'value' attribute"))
                    .getValue();
            Object obj = value.getValue();
            if (!(obj instanceof List<?> arr)) {
                throw new IllegalArgumentException("Can't unwrap non-array");
            }
            if (arr.isEmpty()) {
                return List.of();
            }
            //noinspection unchecked
            return (List<AnnotationMirror>) arr;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Not an aray of AnnotationMirrors", e);
        }
    }

    private boolean isRepeatable(AnnotationMirror mirror) {
        return AnnotationMirrorUtil.findAnnotationMirror(
                () -> elementUtils.getAllAnnotationMirrors(AnnotationMirrorUtil.toTypeElement(mirror)),
                Repeatable.class.getName()
        ) != null;
    }

    private void cacheStep(AnnotatedConstruct construct, DelegateContainer delegates, AnnotationMirror current, int inContainerIndex) {
        TypeElement currentElement;
        if (isRepeatableContainer(current)) {
            int i = 0;
            for (AnnotationMirror inContainer : unwrapContainer(current)) {
                cacheStep(construct, delegates, inContainer, i++);
            }
            return;
        }

        currentElement = addAnnotationMirror(current);
        Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(elementUtils.getBinaryName(currentElement).toString(), Annotation.class);
        if (clazz != null) {
            Annotation[] annotation = construct.getAnnotationsByType(clazz);
            addAnnotation(delegates, annotation[inContainerIndex]);
        }

        if (currentElement.getAnnotation(Composed.class) == null) {
            return;
        }

        for (AnnotationMirror child : elementUtils.getAllAnnotationMirrors(currentElement)) {
            //Prevent self-recursion
            if (isOnlyDirect(child) || child.getAnnotationType().equals(current.getAnnotationType())) {
                continue;
            }
            if (isRepeatable(current) && !isRepeatable(child)) {
                throw new IllegalStateException("Type [%s] is marked as @Repeatable, but its meta-annotation [%s] is not".formatted(currentElement, child.getAnnotationType()));
            }
            String childName = AnnotationMirrorUtil.toTypeElement(child).getQualifiedName().toString();
            DelegateContainer container = ProxyFactory.mapDelegatesFromAnnotationMirror(elementUtils, childName, current, delegates);
            cacheStep(currentElement, container, child, 0);
        }
    }

    private <A extends Annotation> A[] byType(Class<A> clazz) {
        if (ProxyFactory.ONLY_DIRECT.contains(clazz)) {
            //noinspection unchecked
            A[] arr = (A[]) Array.newInstance(clazz, 1);
            arr[0] = original.getAnnotation(clazz);
            return arr;
        }
        List<Annotation> annotations = annotationMap.get(clazz);
        if (annotations != null && !annotations.isEmpty()) {
            //noinspection unchecked
            A[] arr = (A[]) Array.newInstance(clazz, annotations.size());
            return annotations.toArray(arr);
        }
        //noinspection unchecked
        return (A[]) Array.newInstance(clazz, 0);
    }

    private <A extends Annotation> A byTypeSingular(Class<A> clazz) {
        A[] arr = byType(clazz);
        if (arr.length == 0) {
            return null;
        }
        if (arr.length > 1) {
            throw new UnsupportedOperationException("Please use #get(Declared)AnnotationsByType to get multiple instances of a @Repeatable annotation");
        }
        return arr[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        return switch (name) {
            case "getAnnotationMirrors" -> annotationMirrorsMap.values().stream().flatMap(List::stream).toList();
            case "getAnnotation" -> byTypeSingular((Class<? extends Annotation>) args[0]);
            case "getAnnotationsByType" -> byType((Class<? extends Annotation>) args[0]);
            case null -> throw new IllegalStateException();
            default -> super.invoke(proxy, method, args);
        };
    }
}
