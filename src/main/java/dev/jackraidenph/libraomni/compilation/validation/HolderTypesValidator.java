package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.util.ElementUtil;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.exception.AnnotationValidationException;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public final class HolderTypesValidator extends TypesValidator {

    private static final String DEFERRED_HOLDER_CLASS = "net.neoforged.neoforge.registries.DeferredHolder";

    private static boolean isDeferredHolder(Element e, Elements elements, Types types) {
        TypeMirror deferredHolderType = elements.getTypeElement(DEFERRED_HOLDER_CLASS).asType();
        TypeMirror fieldType = e.asType();

        return types.isAssignable(
                types.erasure(fieldType),
                deferredHolderType
        );
    }

    private static Element resolveDeferredHolder(Element e) {
        DeclaredType fieldDeclaredType = ElementUtil.getReturnType(e);
        DeclaredType firstTypeArg = (DeclaredType) fieldDeclaredType.getTypeArguments().getFirst();

        return firstTypeArg.asElement();
    }

    @Override
    public void test(Element validatedElement, TypeElement validatedAnnotation, List<String> args, ProcessingContext processingContext) {
        ProcessingEnvironment processingEnvironment = processingContext.processingEnvironment();
        if (!isDeferredHolder(validatedElement, processingEnvironment.getElementUtils(), processingEnvironment.getTypeUtils())) {
            throw new AnnotationValidationException(
                    "[%s] must be a DeferredHolder<A, ? extends A>, where A is assignable to any of %s".formatted(validatedElement, args)
            );
        }

        super.test(resolveDeferredHolder(validatedElement), validatedAnnotation, args, processingContext);
    }

    @Override
    public String toString(@Nullable List<String> args, Elements elements) {
        return "Element must be a DeferredHolder holding either of types " + args;
    }
}
