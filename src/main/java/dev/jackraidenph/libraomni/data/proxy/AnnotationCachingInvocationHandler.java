package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.Composed;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

public abstract class AnnotationCachingInvocationHandler<T> extends ObjectPreservingInvocationHandler<T> implements AnnotationAccessor<T> {

    protected final Map<Class<? extends Annotation>, List<Annotation>> annotationMap = new HashMap<>();
    private final AnnotationAccessor<T> accessor;

    public AnnotationCachingInvocationHandler(T original, AnnotationAccessor<T> accessor) {
        super(original);
        this.accessor = accessor;
        cacheRecursive(accessor);
    }

    @Override
    public T annotatedObject() {
        return original;
    }

    @Override
    public Collection<Annotation> getAllAnnotations() {
        return accessor.getAllAnnotations();
    }

    protected Annotation[] getProxiedRecursiveAnnotations() {
        return annotationMap.values().stream().flatMap(List::stream).toArray(Annotation[]::new);
    }

    private void cacheRecursive(AnnotationAccessor<T> accessor) {
        for (Annotation annotation : accessor.getAllAnnotations()) {
            step(annotation);
        }
    }

    private static final Map<ElementKind, ElementType> KIND_TYPE_MAP = Map.ofEntries(
            Map.entry(ElementKind.CLASS, ElementType.TYPE),
            Map.entry(ElementKind.RECORD, ElementType.TYPE),
            Map.entry(ElementKind.INTERFACE, ElementType.TYPE),
            Map.entry(ElementKind.ENUM, ElementType.TYPE),
            Map.entry(ElementKind.ANNOTATION_TYPE, ElementType.ANNOTATION_TYPE),
            Map.entry(ElementKind.FIELD, ElementType.FIELD),
            Map.entry(ElementKind.ENUM_CONSTANT, ElementType.FIELD),
            Map.entry(ElementKind.METHOD, ElementType.METHOD),
            Map.entry(ElementKind.CONSTRUCTOR, ElementType.CONSTRUCTOR),
            Map.entry(ElementKind.TYPE_PARAMETER, ElementType.TYPE_PARAMETER),
            Map.entry(ElementKind.LOCAL_VARIABLE, ElementType.LOCAL_VARIABLE),
            Map.entry(ElementKind.EXCEPTION_PARAMETER, ElementType.LOCAL_VARIABLE),
            Map.entry(ElementKind.BINDING_VARIABLE, ElementType.LOCAL_VARIABLE),
            Map.entry(ElementKind.MODULE, ElementType.MODULE),
            Map.entry(ElementKind.PACKAGE, ElementType.PACKAGE),
            Map.entry(ElementKind.PARAMETER, ElementType.PARAMETER),
            Map.entry(ElementKind.RECORD_COMPONENT, ElementType.RECORD_COMPONENT)
    );

    private boolean annotationApplicableTo(Annotation annotation, T e) {
        ElementType type = e instanceof AnnotatedElement annotatedElement
                ? SafeReflectionUtil.getElementType(annotatedElement)
                : KIND_TYPE_MAP.get(((Element) e).getKind());

        Target target = annotation.annotationType().getAnnotation(Target.class);
        if (target == null) {
            return true;
        }

        for (ElementType targetVal : target.value()) {
            if (targetVal.equals(type)) {
                return true;
            }
        }

        return false;
    }

    private void step(Annotation parentAnnotation) {
        Class<? extends Annotation> type = parentAnnotation.annotationType();
        if (annotationApplicableTo(parentAnnotation, original)) {
            annotationMap.computeIfAbsent(type, k -> new ArrayList<>()).add(parentAnnotation);
        }

        Composed composed = type.getAnnotation(Composed.class);

        if (composed == null) {
            return;
        }

        for (Annotation metaAnnotation : type.getAnnotations()) {
            if (!(metaAnnotation instanceof Composed)) {
                Annotation metaProxy = ProxyFactory.proxifyAnnotation(metaAnnotation, parentAnnotation);
                step(metaProxy);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        return switch (name) {
            case "getAllAnnotations" -> getAllAnnotations();
            case "getAnnotationByClass" -> getAnnotationByClass((Class<? extends Annotation>) args[0]);
            case "getAnnotationsByClass" -> getAnnotationsByClass((Class<? extends Annotation>) args[0]);
            case "isAnnotationPresent" -> isAnnotationPresent((Class<? extends Annotation>) args[0]);
            case "annotatedObject" -> annotatedObject();
            default -> super.invoke(proxy, method, args);
        };

    }
}
