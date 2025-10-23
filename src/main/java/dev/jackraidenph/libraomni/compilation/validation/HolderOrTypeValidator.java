package dev.jackraidenph.libraomni.compilation.validation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class HolderOrTypeValidator extends AssignabilityValidator {

    private static final String DEFERRED_HOLDER_CLASS = "net.neoforged.neoforge.registries.DeferredHolder";

    private static boolean isDeferredHolder(Element e, Elements elements, Types types) {
        TypeMirror deferredHolderType = elements.getTypeElement(DEFERRED_HOLDER_CLASS).asType();
        TypeMirror fieldType = e.asType();

        return types.isAssignable(
                types.erasure(fieldType),
                deferredHolderType
        );
    }

    private static Element tryResolveDeferredHolder(Element e) {
        if (!(e instanceof VariableElement variableElement)) {
            return null;
        }

        TypeMirror fieldType = variableElement.asType();

        DeclaredType fieldDeclaredType = (DeclaredType) fieldType;
        DeclaredType firstTypeArg = (DeclaredType) fieldDeclaredType.getTypeArguments().getFirst();

        return firstTypeArg.asElement();
    }

    @Override
    public boolean test(Element element, ProcessingEnvironment processingEnvironment) {
        if (!isDeferredHolder(element, processingEnvironment.getElementUtils(), processingEnvironment.getTypeUtils())) {
            return super.test(element, processingEnvironment);
        }

        return super.test(tryResolveDeferredHolder(element), processingEnvironment);
    }
}
