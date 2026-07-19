package dev.jackraidenph.libraomni.data.cache;

import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import dev.jackraidenph.libraomni.data.proxy.SyntheticAnnotation;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

public class AnnotationCache implements AnnotatedElement {

    private final Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();
    private final Map<Class<? extends Annotation>, List<Annotation>> declaredAnnotations = new HashMap<>();

    public AnnotationCache(AnnotatedElement annotatedElement) {
        cache(annotatedElement.getAnnotations(), false);
        cache(annotatedElement.getDeclaredAnnotations(), true);
    }

    private Map<Class<? extends Annotation>, List<Annotation>> getStorage(boolean declared) {
        return declared ? declaredAnnotations : annotations;
    }

    private void cache(Annotation[] annotations, boolean declared) {
        for (Annotation a : annotations) {
            Class<? extends Annotation> type = a.annotationType();
            UnfoldsInto unfold = type.getAnnotation(UnfoldsInto.class);

            if ((unfold == null || unfold.retainSelf()) && !AnnotationProcessorConstants.UNFOLD_UNSUPPORTED.contains(type)) {
                getStorage(declared).computeIfAbsent(type, v -> new ArrayList<>()).add(a);
            }

            if (unfold != null) {
                Annotation[] children = unfoldAnnotations(a, unfold);
                cache(children, declared);
            }
        }
    }

    private Annotation[] unfoldAnnotations(Annotation parent, UnfoldsInto unfold) {
        Map<Class<?>, Map<String, Object>> replacements = CacheUtil.getReplacementValues(parent);

        List<Annotation> res = new ArrayList<>();

        for (Class<? extends Annotation> clazz : unfold.value()) {
            //Deny unfolding into special service annotationts
            if (AnnotationProcessorConstants.UNFOLD_UNSUPPORTED.contains(clazz)) {
                throw new IllegalArgumentException("Annotation type [%s] does not support unfolding into".formatted(clazz.getName()));
            }

            Map<String, Object> attributes = replacements.get(clazz);

            //Unroll repeatable containers, don't add the container itself
            if (SafeReflectionUtil.isRepeatableContainer(clazz)) {
                System.out.println("REPEATABLE");
                break;
            }

            Annotation instance = SyntheticAnnotation.create(clazz, attributes);
            res.add(instance);
        }

        return res.toArray(Annotation[]::new);
    }

    private @NotNull <T extends Annotation> T[] byClass(@NotNull Class<T> annotationClass, boolean declared) {
        return getStorage(declared).getOrDefault(annotationClass, List.of()).toArray(i -> SafeReflectionUtil.genericArray(annotationClass, i));
    }

    @Override
    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass) {
        T[] byClass = getAnnotationsByType(annotationClass);
        if (byClass.length == 0) {
            return null;
        }
        return byClass[0];
    }

    @Override
    public @NotNull <T extends Annotation> T[] getAnnotationsByType(@NotNull Class<T> annotationClass) {
        return byClass(annotationClass, false);
    }

    @Override
    public @NotNull <T extends Annotation> T[] getDeclaredAnnotationsByType(@NotNull Class<T> annotationClass) {
        return byClass(annotationClass, true);
    }

    private Annotation[] toArray(boolean declared) {
        return getStorage(declared).values().stream()
                .flatMap(Collection::stream)
                .toArray(Annotation[]::new);
    }

    @Override
    public @NotNull Annotation[] getAnnotations() {
        return toArray(false);
    }

    @Override
    public @NotNull Annotation[] getDeclaredAnnotations() {
        return toArray(true);
    }
}
