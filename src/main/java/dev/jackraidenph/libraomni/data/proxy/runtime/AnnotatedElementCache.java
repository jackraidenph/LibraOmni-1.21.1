package dev.jackraidenph.libraomni.data.proxy.runtime;

import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;
import dev.jackraidenph.libraomni.compilation.CompileConstants;
import dev.jackraidenph.libraomni.data.proxy.CacheUtil;
import dev.jackraidenph.libraomni.data.proxy.UnfoldingCache;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.util.TransformerUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

public class AnnotatedElementCache implements UnfoldingCache<Annotation> {

    private final Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();
    private final Map<Class<? extends Annotation>, List<Annotation>> declaredAnnotations = new HashMap<>();

    public final AnnotatedElement annotatedElement;

    private boolean processingDeclared;

    public AnnotatedElementCache(AnnotatedElement annotatedElement) {
        this.annotatedElement = annotatedElement;

        processingDeclared = false;
        cache(List.of(annotatedElement.getAnnotations()));

        processingDeclared = true;
        cache(List.of(annotatedElement.getDeclaredAnnotations()));
    }

    private Map<Class<? extends Annotation>, List<Annotation>> getStorage(boolean declared) {
        return declared ? declaredAnnotations : annotations;
    }

    @Override
    public List<? extends Annotation> unfold(Annotation toUnfold, UnfoldsInto unfoldInfo) {
        Map<Class<?>, Map<String, Object>> replacements = CacheUtil.getReplacementValues(annotatedElement, toUnfold);

        List<Annotation> res = new ArrayList<>();

        for (Class<? extends Annotation> clazz : unfoldInfo.value()) {
            //Deny unfolding into special service annotationts
            if (CompileConstants.UNFOLD_UNSUPPORTED.contains(clazz)) {
                throw new IllegalArgumentException("Annotation type [%s] does not support unfolding into".formatted(clazz.getName()));
            }

            Map<String, Object> attributes = replacements.get(clazz);

            Annotation instance = SyntheticAnnotation.create(clazz, attributes);

            //Unroll repeatable containers, don't add the container itself
            if (SafeReflectionUtil.isRepeatableContainer(clazz)) {
                List<? extends Annotation> unwrapped = SafeReflectionUtil.unwrapRepeatableContainer(instance);
                res.addAll(unwrapped);
                continue;
            }

            res.add(instance);
        }

        return res;
    }

    @Override
    public void save(Annotation toSave) {
        toSave = TransformerUtil.processAnnotation(annotatedElement, toSave);
        Class<? extends Annotation> type = toSave.annotationType();
        getStorage(processingDeclared).computeIfAbsent(type, v -> new ArrayList<>()).add(toSave);
    }

    @Override
    public Annotated annotated(Annotation object) {
        return object.annotationType()::getAnnotation;
    }

    public boolean hasAnnotation(@NonNull Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass, false) != null;
    }

    public <T extends Annotation> T getAnnotation(@NotNull Class<T> annotationClass, boolean declared) {
        T[] byClass = getAnnotationsByType(annotationClass, declared);
        if (byClass.length == 0) {
            return null;
        }
        return byClass[0];
    }

    public @NotNull <T extends Annotation> T[] getAnnotationsByType(@NotNull Class<T> annotationClass, boolean declared) {
        return getStorage(declared).getOrDefault(annotationClass, List.of()).toArray(i -> SafeReflectionUtil.genericArray(annotationClass, i));
    }

    public Annotation[] getAnnotations(boolean declared) {
        return getStorage(declared).values().stream()
                .flatMap(Collection::stream)
                .toArray(Annotation[]::new);
    }
}
