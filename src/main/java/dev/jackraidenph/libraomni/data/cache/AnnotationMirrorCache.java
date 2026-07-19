package dev.jackraidenph.libraomni.data.cache;

import dev.jackraidenph.libraomni.annotation.meta.UnfoldsInto;
import dev.jackraidenph.libraomni.common.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.common.ElementUtil;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.data.proxy.SyntheticAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.Map.Entry;

public class AnnotationMirrorCache implements AnnotatedConstruct {

    protected final Map<TypeElement, List<AnnotationMirror>> annotationMirrorsMap = new HashMap<>();
    protected final Map<Class<? extends Annotation>, List<Annotation>> annotationsMap = new HashMap<>();

    public AnnotationMirrorCache(AnnotatedConstruct construct) {
        List<? extends AnnotationMirror> mirrors = construct instanceof Element e
                ? ElementUtil.Javac.getAllAnnotationMirrors(e)
                : construct.getAnnotationMirrors();

        cache(mirrors);
    }

    private void cache(List<? extends AnnotationMirror> mirrors) {
        for (AnnotationMirror m : mirrors) {
            TypeElement type = AnnotationMirrorUtil.toTypeElement(m);
            UnfoldsInto unfold = type.getAnnotation(UnfoldsInto.class);

            if (unfold == null || unfold.retainSelf()) {
                annotationMirrorsMap.computeIfAbsent(type, v -> new ArrayList<>()).add(m);
                Annotation annotation = tryCovnertToAnnotation(m);
                if (annotation != null) {
                    annotationsMap.computeIfAbsent(annotation.annotationType(), v -> new ArrayList<>()).add(annotation);
                }
            }

            if (unfold != null) {
                List<? extends AnnotationMirror> children = unfoldAnnotations(m, unfold);
                cache(children);
            }
        }
    }

    private List<? extends AnnotationMirror> unfoldAnnotations(AnnotationMirror parent, UnfoldsInto unfold) {
        List<AnnotationMirror> res = new ArrayList<>();

        Map<String, Map<ExecutableElement, AnnotationValue>> replacements = CacheUtil.getReplacementValues(parent);

        List<? extends TypeMirror> mirrors = ElementUtil.mirrorClassArray(unfold::value);

        for (TypeMirror typeMirror : mirrors) {
            TypeElement type = ElementUtil.mirrorToElement(typeMirror);

            //Deny unfolding into special service annotationts
            if (CacheUtil.isUnfoldUnsupported(type)) {
                throw new IllegalArgumentException("Annotation type [%s] does not support unfolding into".formatted(ElementUtil.Javac.binaryName(type)));
            }

            String typeName = ElementUtil.Javac.binaryName(type);
            Map<ExecutableElement, AnnotationValue> attributes = replacements.getOrDefault(typeName, Map.of());

            AnnotationMirror instance = new SyntheticAnnotationMirror((DeclaredType) typeMirror, attributes);

            //Unroll repeatable containers, don't add the container itself
            if (AnnotationMirrorUtil.isRepeatableContainer(instance)) {
                //TODO
                break;
            }

            res.add(instance);
        }

        return res;
    }

    private Annotation tryCovnertToAnnotation(AnnotationMirror mirror) {
        TypeElement type = AnnotationMirrorUtil.toTypeElement(mirror);
        String binaryName = ElementUtil.Javac.binaryName(type);
        Class<? extends Annotation> clazz = SafeReflectionUtil.forNameSubclass(binaryName, Annotation.class);

        if (clazz == null || CacheUtil.isUnfoldUnsupported(type)) {
            return null;
        }

        Map<String, Object> reflectiveReplacements = new HashMap<>();

        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> kv : mirror.getElementValues().entrySet()) {
            String attribute = kv.getKey().getSimpleName().toString();
            Object value = kv.getValue().getValue();
            value = CacheUtil.normalizeValue(value);

            reflectiveReplacements.put(attribute, value);
        }

        return SyntheticAnnotation.create(clazz, reflectiveReplacements);
    }

    @Override
    public @NotNull List<? extends AnnotationMirror> getAnnotationMirrors() {
        return annotationMirrorsMap.values().stream().flatMap(Collection::stream).toList();
    }

    @Override
    public @Nullable <A extends Annotation> A getAnnotation(@NotNull Class<A> annotationType) {
        A[] array = getAnnotationsByType(annotationType);
        if (array.length == 0) {
            return null;
        }

        return array[0];
    }

    @Override
    public @NotNull <A extends Annotation> A[] getAnnotationsByType(@NotNull Class<A> annotationType) {
        return annotationsMap.getOrDefault(annotationType, List.of()).toArray(i -> SafeReflectionUtil.genericArray(annotationType, i));
    }
}
