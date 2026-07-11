package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.meta.Composed;
import dev.jackraidenph.libraomni.common.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.ModIdGetter;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotatedConstructProxy extends AbstractObjectProxy<AnnotatedConstruct> {

    private final AnnotatedConstructCache cache;
    private final Elements elementUtils;
    private final ModIdGetter modIdGetter;

    public AnnotatedConstructProxy(AnnotatedConstruct original, Elements elements, ModIdGetter modIdGetter) {
        super(original);
        elementUtils = elements;
        this.modIdGetter = modIdGetter;
        this.cache = new AnnotatedConstructCache();
    }

    private <A extends Annotation> A[] getAnnotationsByTypeProxy(Class<A> clazz) {
        if (ProxyFactory.ONLY_DIRECT.contains(clazz)) {
            //noinspection unchecked
            A[] arr = (A[]) Array.newInstance(clazz, 1);
            arr[0] = proxiedObject.getAnnotation(clazz);
            return arr;
        }
        List<Annotation> annotations = cache.annotationMap.get(clazz);
        if (annotations != null && !annotations.isEmpty()) {
            //noinspection unchecked
            A[] arr = (A[]) Array.newInstance(clazz, annotations.size());
            return annotations.toArray(arr);
        }
        //noinspection unchecked
        return (A[]) Array.newInstance(clazz, 0);
    }

    private <A extends Annotation> A getAnnotationProxy(Class<A> clazz) {
        A[] arr = getAnnotationsByTypeProxy(clazz);
        if (arr.length == 0) {
            return null;
        }
        if (arr.length > 1) {
            throw new UnsupportedOperationException("Please use #get(Declared)AnnotationsByType to get multiple instances of a @Repeatable annotation [%s], from element [%s]".formatted(clazz, proxiedObject));
        }
        return arr[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        return switch (name) {
            case "getAnnotationMirrors" -> cache.annotationMirrorsMap.values().stream().flatMap(List::stream).toList();
            case "getAnnotation" -> getAnnotationProxy((Class<? extends Annotation>) args[0]);
            case "getAnnotationsByType" -> getAnnotationsByTypeProxy((Class<? extends Annotation>) args[0]);
            case null -> throw new IllegalStateException();
            default -> super.invoke(proxy, method, args);
        };
    }

    /// CACHE IMPL

    private class AnnotatedConstructCache {
        protected final Map<TypeElement, List<AnnotationMirror>> annotationMirrorsMap = new HashMap<>();
        protected final Map<Class<? extends Annotation>, List<Annotation>> annotationMap = new HashMap<>();

        private AnnotatedConstructCache() {
            cacheRecursive(proxiedObject);
        }

        private void cacheRecursive(AnnotatedConstruct original) {
            for (AnnotationMirror annotation : elementUtils.getAllAnnotationMirrors((Element) original)) {
                cacheStep(annotation, 0, null, original);
            }
        }

        private void cacheStep(AnnotationMirror currentMirror, int inContainerIndex, AttributeReplacements delegates, AnnotatedConstruct construct) {
            TypeElement currentElement;
            if (AnnotationMirrorUtil.isRepeatableContainer(currentMirror)) {
                //If current AnnotationMirror is a container for @Repeatable annotations - ignore it, unwrap its contents and continue with them
                int i = 0;
                for (AnnotationMirror inContainer : AnnotationMirrorUtil.unwrapRepeatableContainer(currentMirror)) {
                    cacheStep(inContainer, i++, delegates, construct);
                }
                //Do not go further, we are ignoring the container
                return;
            }

            //Just add the mirror
            currentElement = addAnnotationMirror(currentMirror);
            //But check that the class of the annotation's type is actually loaded at this time
            Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(elementUtils.getBinaryName(currentElement).toString(), Annotation.class);
            if (clazz != null) {
                //Correlate current mirror's index in its unwrapped container with annotation's
                Annotation[] annotation = construct.getAnnotationsByType(clazz);
                addAnnotation(delegates, annotation[inContainerIndex]);
            }

            //Do not process non-Composed annotations further
            if (currentElement.getAnnotation(Composed.class) == null) {
                return;
            }

            for (AnnotationMirror metaMirror : elementUtils.getAllAnnotationMirrors(currentElement)) {
                //Do not process only-direct annotations or recursive types
                if (AnnotationMirrorUtil.isOnlyDirect(elementUtils, metaMirror)
                        //Prevent self-recursion
                        || metaMirror.getAnnotationType().equals(currentMirror.getAnnotationType())) {
                    continue;
                }
                if (isRepeatable(currentMirror) && !isRepeatable(metaMirror)) {
                    throw new IllegalStateException("Type [%s] is marked as @Repeatable, but its meta-annotation [%s] is not".formatted(currentElement, metaMirror.getAnnotationType()));
                }
                cacheStep(metaMirror, 0, fromMeta(currentMirror, metaMirror, delegates), currentElement);
            }
        }

        private AttributeReplacements fromMeta(AnnotationMirror current, AnnotationMirror meta, AttributeReplacements context) {
            String childName = AnnotationMirrorUtil.toTypeElement(meta).getQualifiedName().toString();
            return ProxyFactory.mapDelegatesFromAnnotationMirror(elementUtils, childName, current, context);
        }

        private boolean isRepeatable(AnnotationMirror mirror) {
            return AnnotationMirrorUtil.findAnnotationMirror(
                    () -> elementUtils.getAllAnnotationMirrors(AnnotationMirrorUtil.toTypeElement(mirror)),
                    Repeatable.class.getName()
            ) != null;
        }

        private TypeElement addAnnotationMirror(AnnotationMirror mirror) {
            if (mirror == null) {
                return null;
            }
            TypeElement typeElement = AnnotationMirrorUtil.toTypeElement(mirror);
            if (!AnnotationMirrorUtil.isOnlyDirect(elementUtils, mirror)) {
                annotationMirrorsMap.computeIfAbsent(typeElement, k -> new ArrayList<>()).add(mirror);
            }
            return typeElement;
        }

        private void addAnnotation(AttributeReplacements delegates, Annotation annotation) {
            if (ProxyFactory.ONLY_DIRECT.contains(annotation.annotationType())) {
                return;
            }

            boolean delegated = delegates != null && !delegates.isEmpty();
            Annotation proxyOrSelf = delegated ? ProxyFactory.makeAnnotationProxy(annotation, delegates, proxiedObject, modIdGetter) : annotation;
            List<Annotation> annotations = annotationMap.computeIfAbsent(annotation.annotationType(), k -> new ArrayList<>());
            annotations.add(proxyOrSelf);
        }
    }
}
