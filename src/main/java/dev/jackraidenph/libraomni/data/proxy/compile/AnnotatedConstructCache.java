package dev.jackraidenph.libraomni.data.proxy.compile;

import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;
import dev.jackraidenph.libraomni.common.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.common.ElementUtil;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.CacheUtil;
import dev.jackraidenph.libraomni.data.proxy.UnfoldingCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;

public class AnnotatedConstructCache implements UnfoldingCache<AnnotationMirror> {

    protected final Map<TypeElement, List<AnnotationMirror>> annotationMirrorsMap = new HashMap<>();
    protected final Map<Class<? extends Annotation>, List<Annotation>> annotationsMap = new HashMap<>();

    public AnnotatedConstructCache(AnnotatedConstruct construct) {
        List<? extends AnnotationMirror> mirrors = construct instanceof Element e
                ? ElementUtil.Javac.getAllAnnotationMirrors(e)
                : construct.getAnnotationMirrors();

        cache(mirrors);
    }

    @Override
    public List<? extends AnnotationMirror> unfold(AnnotationMirror toUnfold, UnfoldsInto unfoldInfo) {
        List<AnnotationMirror> res = new ArrayList<>();

        Map<String, Map<ExecutableElement, AnnotationValue>> replacements = CacheUtil.getReplacementValues(toUnfold);

        List<? extends TypeMirror> mirrors = ElementUtil.mirrorClassArray(unfoldInfo::value);

        for (TypeMirror typeMirror : mirrors) {
            TypeElement type = ElementUtil.mirrorToElement(typeMirror);

            //Deny unfolding into special service annotationts
            if (ElementUtil.isUnfoldUnsupported(type)) {
                throw new IllegalArgumentException("Annotation type [%s] does not support unfolding into".formatted(ElementUtil.Javac.binaryName(type)));
            }

            String typeName = ElementUtil.Javac.binaryName(type);
            Map<ExecutableElement, AnnotationValue> attributes = replacements.getOrDefault(typeName, Map.of());

            AnnotationMirror instance = new SyntheticAnnotationMirror((DeclaredType) typeMirror, attributes);

            //Unroll repeatable containers, don't add the container itself
            if (AnnotationMirrorUtil.isRepeatableContainer(instance)) {
                List<? extends AnnotationMirror> unwrapped = AnnotationMirrorUtil.unwrapRepeatableContainer(instance);
                res.addAll(unwrapped);
                continue;
            }

            res.add(instance);
        }

        return res;
    }

    @Override
    public void save(AnnotationMirror toSave) {
        TypeElement type = AnnotationMirrorUtil.toTypeElement(toSave);

        annotationMirrorsMap.computeIfAbsent(type, v -> new ArrayList<>()).add(toSave);
        Annotation annotation = AnnotationMirrorUtil.tryCovnertToAnnotation(toSave);
        if (annotation != null) {
            annotationsMap.computeIfAbsent(annotation.annotationType(), v -> new ArrayList<>()).add(annotation);
        }
    }

    @Override
    public Annotated annotated(AnnotationMirror object) {
        return AnnotationMirrorUtil.toTypeElement(object)::getAnnotation;
    }

    public @NotNull List<? extends AnnotationMirror> getAnnotationMirrors() {
        return annotationMirrorsMap.values().stream().flatMap(Collection::stream).toList();
    }

    public @Nullable <A extends Annotation> A getAnnotation(@NotNull Class<A> annotationType) {
        A[] array = getAnnotationsByType(annotationType);
        if (array.length == 0) {
            return null;
        }

        return array[0];
    }

    public @NotNull <A extends Annotation> A[] getAnnotationsByType(@NotNull Class<A> annotationType) {
        return annotationsMap.getOrDefault(annotationType, List.of()).toArray(i -> SafeReflectionUtil.genericArray(annotationType, i));
    }
}
