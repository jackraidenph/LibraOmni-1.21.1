package dev.jackraidenph.libraomni.common;

import dev.jackraidenph.libraomni.annotation.meta.Replaces;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.lang.model.element.ExecutableElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TransformerUtil {
    private static final Map<Class<? extends Function<?, ?>>, Function<?, ?>> transformers = new HashMap<>();

    public static Class<? extends Function<?, ?>> getTransformerClass(Replaces declaringAnnotation) {
        return ElementUtil.getOrUnmirrorClass(declaringAnnotation::transformer);
    }

    public static <IN, OUT> OUT tryTransform(IN oldValue, @NonNull Replaces replacer, Method executableElement) {
        //noinspection unchecked
        Class<? extends Function<IN, OUT>> transformerClass = (Class<? extends Function<IN, OUT>>) getTransformerClass(replacer);

        if (transformerClass.equals(NoOpTransformer.class)) {
            //Always IN == OUT for NoOpTransformer
            //noinspection unchecked
            return (OUT) oldValue;
        }

        //noinspection unchecked
        Class<OUT> destinationClass = (Class<OUT>) executableElement.getReturnType();

        return applyTransformation(oldValue, destinationClass, transformerClass);
    }

    public static <IN, OUT> OUT tryTransform(IN oldValue, @NonNull Replaces repplacer, ExecutableElement executableElement) {
        //noinspection unchecked
        Class<? extends Function<IN, OUT>> transformerClass = (Class<? extends Function<IN, OUT>>) getTransformerClass(repplacer);

        if (transformerClass.equals(NoOpTransformer.class)) {
            //Always IN == OUT for NoOpTransformer
            //noinspection unchecked
            return (OUT) oldValue;
        }

        //noinspection unchecked
        Class<OUT> destinationClass = (Class<OUT>) ElementUtil.fromTypeMirror(executableElement.getReturnType());

        return applyTransformation(oldValue, destinationClass, transformerClass);
    }

    public static <IN, OUT> OUT applyTransformation(IN oldValue, Class<OUT> destinationClass, Class<? extends Function<IN, OUT>> transformerClass) {
        //noinspection unchecked
        Function<IN, OUT> transformer = (Function<IN, OUT>) transformers.computeIfAbsent(transformerClass, UnsafeReflectionUtil::tryConstruct);
        OUT newObj = transformer.apply(oldValue);
        return SafeReflectionUtil.selfOrSingletonArray(destinationClass, newObj);
    }

    public static final class NoOpTransformer implements Function<Object, Object> {
        @Override
        public Object apply(Object o) {
            return o;
        }
    }
}
