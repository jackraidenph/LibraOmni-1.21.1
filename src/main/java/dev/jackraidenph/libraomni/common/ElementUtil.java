package dev.jackraidenph.libraomni.common;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Pair;
import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import dev.jackraidenph.libraomni.data.proxy.runtime.SyntheticAnnotation;

import javax.annotation.Nonnull;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Supplier;

import static com.sun.tools.javac.code.TypeTag.CLASS;

/**
 * A utility class with hellper methods to work with the contents of javax.lang.model
 */
public final class ElementUtil {

    private static final String OBJECT_STR = Object.class.getName();

    public static boolean isUnfoldUnsupported(TypeElement type) {
        return AnnotationProcessorConstants.UNFOLD_UNSUPPORTED.stream()
                .anyMatch(c -> ElementUtil.Javac.binaryName(type).equals(c.getName()));
    }

    public static Object tryConvertInternalRepresentation(TypeMirror type, Object internal) {
        if (internal instanceof com.sun.tools.javac.util.List<?> sunList) {
            return sunListToArray(type, sunList);
        }

        if (internal instanceof Symbol.VarSymbol varSymbol) {
            if (varSymbol.getKind().equals(ElementKind.ENUM_CONSTANT)) {
                return varSymbolToEnum(varSymbol);
            } else {
                throw new UnsupportedOperationException("Ecountered VarSymbol of kind [%s]".formatted(varSymbol.getKind()));
            }
        }

        return internal;
    }

    public static Object varSymbolToEnum(Symbol.VarSymbol varSymbol) {
        String binary = varSymbol.owner.flatName().toString();
        var clazz = SafeReflectionUtil.forNameSubclass(binary, Enum.class);
        if (clazz == null) {
            throw new IllegalStateException("Failed to instantiate enum class for name [%s]".formatted(binary));
        }
        //noinspection unchecked
        return Enum.valueOf(clazz, varSymbol.name.toString());
    }

    public static Object sunListToArray(TypeMirror type, com.sun.tools.javac.util.List<?> sunList) {
        Class<?> clazz = ElementUtil.fromTypeMirror(type);

        if (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }

        Object arr = Array.newInstance(clazz, sunList.size());
        for (int i = 0; i < sunList.size(); i++) {
            Array.set(arr, i, attributeToObject((Attribute) sunList.get(i)));
        }
        return arr;
    }

    public static Object attributeToObject(Attribute attribute) {
        return switch (attribute) {
            case Attribute.Constant constant -> constant.getValue();
            case Attribute.Class clazz -> fromTypeMirror(clazz.getValue());
            case Attribute.Compound compound -> compoundToAnnotation(compound);
            case Attribute.Enum enoom -> varSymbolToEnum(enoom.value);
            case Attribute.Array arr -> attributeArrayToArray(arr);
            case Attribute.UnresolvedClass unresolved ->
                    UnsafeReflectionUtil.tryConstruct(fromTypeMirror(unresolved.type), fromTypeMirror(unresolved.classType));
            case Attribute.Error error -> UnsafeReflectionUtil.tryConstruct(fromTypeMirror(error.type));
            default -> throw new UnsupportedOperationException();
        };
    }

    public static Object attributeArrayToArray(Attribute.Array array) {
        Class<?> clazz = fromTypeMirror(array.type);
        Attribute[] vals = array.values;
        Object arr = Array.newInstance(clazz, vals.length);
        for (int i = 0; i < vals.length; i++) {
            Array.set(arr, i, attributeToObject(vals[i]));
        }
        return arr;
    }

    public static Annotation compoundToAnnotation(Attribute.Compound compound) {
        Map<String, Object> values = new HashMap<>();
        //noinspection unchecked
        Class<? extends Annotation> type = (Class<? extends Annotation>) fromTypeMirror(compound.type);
        for (Pair<MethodSymbol, Attribute> pair : compound.values) {
            String attributeName = pair.fst.name.toString();
            values.put(attributeName, attributeToObject(pair.snd));
        }

        return SyntheticAnnotation.create(type, values);
    }

    public static ExecutableElement getExecutableElementByName(String name, TypeElement typeElement) {
        return ElementFilter.methodsIn(typeElement.getEnclosedElements())
                .stream()
                .filter(ex -> ex.getSimpleName().contentEquals(name))
                .findFirst()
                .orElse(null);
    }

    @Nonnull
    public static List<? extends TypeMirror> mirrorClassArray(Supplier<Class<?>[]> supplier) {
        try {
            supplier.get();
            throw new IllegalStateException("Method called in inappropriate context");
        } catch (MirroredTypesException typeException) {
            return typeException.getTypeMirrors();
        }
    }

    @Nonnull
    public static TypeMirror mirrorClass(Supplier<Class<?>> supplier) {
        try {
            supplier.get();
            throw new IllegalStateException("Method called in inappropriate context");
        } catch (MirroredTypeException typeException) {
            return typeException.getTypeMirror();
        }
    }

    public static Class<?> fromTypeMirror(TypeMirror typeMirror) {
        String binaryName = ElementUtil.Javac.binaryName(typeMirror);
        Class<?> clazz = SafeReflectionUtil.forName(binaryName);
        if (clazz == null) {
            throw new IllegalArgumentException("""
                    Failed to find class [%s] by the corresponding TypeMirror, \
                    most probably, the class is not loaded
                    """.formatted(binaryName)
            );
        }
        return clazz;
    }

    public static <T> Class<T> getOrUnmirrorClass(Supplier<Class<T>> fromAnnotationGetter) {
        try {
            return fromAnnotationGetter.get();
        } catch (MirroredTypeException e) {
            TypeMirror typeMirror = e.getTypeMirror();
            //noinspection unchecked
            return (Class<T>) fromTypeMirror(typeMirror);
        }
    }

    public static <T> Class<T>[] getOrUnmirrorClassArray(Supplier<Class<T>[]> fromAnnotationGetter) {
        Class<T>[] classes;
        try {
            classes = fromAnnotationGetter.get();
            return classes;
        } catch (MirroredTypesException e) {
            List<? extends TypeMirror> typeMirrors = e.getTypeMirrors();
            //noinspection unchecked
            classes = new Class[typeMirrors.size()];

            int i = 0;
            for (TypeMirror typeMirror : typeMirrors) {
                String binaryName = ElementUtil.Javac.binaryName(typeMirror);
                //noinspection unchecked
                Class<T> clazz = (Class<T>) SafeReflectionUtil.forName(binaryName);
                if (clazz == null) {
                    throw new IllegalArgumentException("""
                            Failed to find class [%s] by the corresponding TypeMirror, \
                            most probably, the class is not loaded
                            """.formatted(binaryName)
                    );
                }
                classes[i++] = clazz;
            }
            return classes;
        }
    }

    public static TypeElement mirrorToElement(TypeMirror typeMirror) {
        return (TypeElement) ((DeclaredType) typeMirror).asElement();
    }

    /**
     * @param e Element
     * @return Self if TypeElement, return type if ExecutableElement, or variable type if VariableElement
     * @throws UnsupportedOperationException if supplied Element is not a TypeElement, an ExecutableElement, or a VariableElement
     */
    public static TypeElement getReturnTypeElement(Element e) {
        return (TypeElement) getReturnType(e).asElement();
    }

    public static DeclaredType getReturnType(Element e) {
        return ((DeclaredType) switch (e) {
            case TypeElement typeElement -> typeElement.asType();
            case ExecutableElement executableElement -> executableElement.getReturnType();
            case VariableElement variableElement -> variableElement.asType();
            case null, default -> throw new UnsupportedOperationException();
        });
    }

    /**
     * @param typeElement TypeElement to retrieve directly enclosed methods
     * @return List of ExecutableElements, which is a list of directly enclosed methods
     */
    public static List<ExecutableElement> getMethodsInElement(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind().isExecutable())
                .map(e -> (ExecutableElement) e)
                .toList();
    }

    /**
     * @param typeElement TypeElement to retrieve all enclosed methods
     * @return List of ExecutableElements, which is a list of all enclosed methods
     */
    public static List<ExecutableElement> getDeclaredMethodsInElement(TypeElement typeElement, Elements elements) {
        return elements.getAllMembers(typeElement).stream()
                .filter(e -> e.getKind().isExecutable())
                .map(e -> (ExecutableElement) e)
                .toList();
    }

    /**
     * @param typeElement TypeElement to retrieve a list of abstract methods from
     * @return List of directly present abstract methods
     */
    public static List<ExecutableElement> getAbstractMethods(TypeElement typeElement) {
        return getMethodsInElement(typeElement).stream()
                .filter(m -> m.getModifiers().contains(Modifier.ABSTRACT))
                .toList();
    }

    /**
     * @param typeElement TypeElement to retrieve a list of abstract methods from
     * @return List of directly present abstract methods
     */
    public static List<ExecutableElement> getDeclaredAbstractMethods(TypeElement typeElement, Elements elements) {
        return getDeclaredMethodsInElement(typeElement, elements).stream()
                .filter(m -> m.getModifiers().contains(Modifier.ABSTRACT))
                .toList();
    }

    /**
     * @param typeElement TypeElement to check
     * @return Whether the TypeElement is a functional interface. Meaning, whether it's an interface with exactly 1 abstract method
     */
    public static boolean isFunctionalInterface(TypeElement typeElement) {
        return typeElement.getKind().isInterface() && getAbstractMethods(typeElement).size() == 1;
    }

    /**
     * @param typeElement Functional interface to retrieve the function from
     * @return The sole abstract method (function) of the functional interface, in the form of ExecutableElement
     */
    public static ExecutableElement getFunction(TypeElement typeElement) {
        if (!isFunctionalInterface(typeElement)) {
            throw new IllegalArgumentException("Not a functional interface");
        }

        return getAbstractMethods(typeElement).getFirst();
    }

    /**
     * Resolves a functional interface's return type
     *
     * @param declaredType A DeclaredType of a functional interface
     * @param types        Java's set of utilities operating on Types
     * @return A TypeMirror of functional interface's function return type
     */
    private static TypeMirror getFunctionalInterfaceReturnType(DeclaredType declaredType, Types types) {
        TypeMirror memberType = types.asMemberOf(
                declaredType,
                ElementUtil.getFunction((TypeElement) declaredType.asElement())
        );
        return ((ExecutableType) memberType).getReturnType();
    }

    /**
     * @param typeElement TypeElement which's assignability to check
     * @param classNames  Class names to check assignability to
     * @return Whether the TypeElement is assignable to any of classes provided by className parameter
     */
    public static boolean isAssignableToAny(TypeElement typeElement, Elements elements, Types types, String... classNames) {
        if (typeElement.toString().equals(OBJECT_STR)) {
            return true;
        }
        for (String name : classNames) {
            TypeElement typeElementToCheck = elements.getTypeElement(name);
            if (typeElementToCheck == null) {
                throw new IllegalArgumentException("Couldn't find TypeElement for name [%s]".formatted(name));
            }
            if (types.isAssignable(typeElement.asType(), typeElementToCheck.asType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param constructor    ExecutableElement, a constructor, to check
     * @param typeParameters A sequence of canonical class names that must match constructor's type parameters
     * @return Whether the constructor matches a sequence of supplied type parameter canonical names
     */
    public static boolean constructorMatches(ExecutableElement constructor, Elements elements, Types types, String... typeParameters) {
        if (!constructor.getKind().equals(ElementKind.CONSTRUCTOR)) {
            return false;
        }
        List<? extends VariableElement> params = constructor.getParameters();
        for (int i = 0; i < params.size(); i++) {
            TypeMirror constructorParam = params.get(i).asType();
            String str = typeParameters[i];
            TypeMirror toCheckParam = elements.getTypeElement(str).asType();
            if (!types.isAssignable(toCheckParam, constructorParam)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param e              Element to check
     * @param typeParameters A sequence of canonical class names that must match constructor's type parameters
     * @return Whether the element contains a matching constructor
     */
    public static boolean hasConstructorWithParameters(Element e, Elements elements, Types types, String... typeParameters) {
        return e.getEnclosedElements().stream()
                .filter(enc -> enc.getKind().equals(ElementKind.CONSTRUCTOR))
                .map(enc -> ((ExecutableElement) enc))
                .anyMatch((constructor) -> constructorMatches(constructor, elements, types, typeParameters));
    }

    public static String descriptor(String primitiveName) {
        return switch (primitiveName) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            case "char" -> "C";
            default -> throw new UnsupportedOperationException();
        };
    }

    /**
     * Contains static reimplementations of JavacElements methods, unreliant on built symbol tables
     */
    public static final class Javac {

        //STATIC REIMPL of JavacElements#getBinaryName
        public static String binaryName(TypeElement element) {
            return ((TypeSymbol) element).flatName().toString();
        }

        public static String binaryName(TypeMirror typeMirror) {
            if (typeMirror instanceof PrimitiveType primitiveType) {
                return primitiveType.toString();
            }

            if (typeMirror instanceof ArrayType arrayType) {
                TypeMirror component = arrayType.getComponentType();
                if (component instanceof PrimitiveType primitiveType) {
                    return "[" + descriptor(primitiveType.toString());
                } else {
                    return "[L" + binaryName(component) + ";";
                }
            }

            if (typeMirror instanceof DeclaredType declaredType) {
                return binaryName(mirrorToElement(declaredType));
            }

            throw new UnsupportedOperationException("Can't get [%s]'s binary name".formatted(typeMirror));
        }

        //STATIC REIMPL of JavacElements#getAllAnnotationMirrors
        public static List<Compound> getAllAnnotationMirrors(Element e) {
            Symbol sym = (Symbol) e;
            List<Compound> annos = new LinkedList<>(sym.getAnnotationMirrors());
            while (sym.getKind() == ElementKind.CLASS) {
                Type sup = ((ClassSymbol) sym).getSuperclass();

                if (!sup.hasTag(CLASS) || sup.isErroneous() || sup.tsym.flatName().contentEquals(OBJECT_STR)) {
                    break;
                }
                sym = sup.tsym;

                for (Attribute.Compound anno : sym.getAnnotationMirrors()) {
                    if (isInherited(anno.type) && !containsAnnoOfType(annos, anno.type)) {
                        annos.addFirst(anno);
                    }
                }
            }
            return annos;
        }

        //STATIC REIMPL of JavacElements#getAllAnnotationMirrors
        public static boolean isInherited(Type annotype) {
            return annotype.tsym.getRawAttributes().stream()
                    .anyMatch(attr -> attr.type.tsym.flatName().contentEquals(Inherited.class.getName()));
        }

        //STATIC REIMPL of JavacElements#getAllAnnotationMirrors
        public static boolean containsAnnoOfType(List<Compound> annos, Type type) {
            for (Attribute.Compound anno : annos) {
                if (anno.type.tsym == type.tsym)
                    return true;
            }
            return false;
        }
    }
}
