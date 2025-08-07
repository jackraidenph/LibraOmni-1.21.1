package dev.jackraidenph.libraomni.data.proxy;

import dev.jackraidenph.libraomni.annotation.service.Delegate;
import dev.jackraidenph.libraomni.common.SafeReflectionUtil;
import dev.jackraidenph.libraomni.common.UnsafeReflectionUtil;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DelegateContainer {

    private final Map<String, DelegateEntry> methodDelegates = new HashMap<>();

    public boolean isEmpty() {
        return methodDelegates.isEmpty();
    }

    public boolean hasDelegateFor(String methodName) {
        return methodDelegates.containsKey(methodName);
    }

    public Object getDelegatedValue(String methodName) {
        DelegateEntry entry = methodDelegates.get(methodName);
        if (entry == null) {
            return null;
        }
        return entry.delegatedValue();
    }

    public Delegate getAnnotation(String methodName) {
        DelegateEntry entry = methodDelegates.get(methodName);
        if (entry == null) {
            return null;
        }
        return entry.delegateAnnotation();
    }

    private static Class<?> tryGetClass(TypeMirror typeMirror) {
        String name = typeMirror.toString();
        Class<?> clazz = SafeReflectionUtil.forName(name);
        //Might be an inner class
        if (clazz == null) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot < 0) {
                return clazz;
            }
            name = name.substring(0, lastDot) + '$' + name.substring(lastDot + 1);
            clazz = SafeReflectionUtil.forName(name);
        }
        return clazz;
    }

    public Class<? extends Function<Object, Object>> getTransformerClass(String methodName) {
        Delegate annotation = getAnnotation(methodName);
        if (annotation == null) {
            return null;
        }

        Class<? extends Function<Object, Object>> transformerType;
        try {
            transformerType = annotation.transformer();
        } catch (MirroredTypeException mirroredTypeException) {
            TypeMirror typeMirror = mirroredTypeException.getTypeMirror();
            //noinspection unchecked
            transformerType = (Class<? extends Function<Object, Object>>) tryGetClass(typeMirror);
            if (transformerType == null) {
                throw new UnsupportedOperationException("""
                        Transformer class [%s] not found.
                        Most probably, you are trying to use custom transformer implementation
                        for a compile-time annotation.
                        This won't work, your transformer is not yet compiled.
                        """.formatted(typeMirror.toString()));
            }
        }

        return transformerType;
    }

    public Function<Object, Object> getTransformerInstance(String methodName) {
        Class<? extends Function<Object, Object>> trasnformerClass = getTransformerClass(methodName);
        return UnsafeReflectionUtil.tryConstruct(trasnformerClass);
    }

    public Object getTransformedValue(String methodName) {
        Object oldVal = getDelegatedValue(methodName);
        if (oldVal == null) {
            return null;
        }
        Function<Object, Object> transformer = getTransformerInstance(methodName);
        return transformer.apply(oldVal);
    }

    public void add(String delegateTarget, Delegate delegateAnnotation, Object value) {
        this.methodDelegates.put(delegateTarget, new DelegateEntry(delegateAnnotation, value));
    }

    public record DelegateEntry(Delegate delegateAnnotation, Object delegatedValue) {
    }
}
