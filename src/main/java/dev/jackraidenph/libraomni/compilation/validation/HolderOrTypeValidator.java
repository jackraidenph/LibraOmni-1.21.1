package dev.jackraidenph.libraomni.compilation.validation;

import dev.jackraidenph.libraomni.common.ElementUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public final class HolderOrTypeValidator extends AssignabilityValidator {

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
    public boolean test(Element element, List<String> args, ProcessingEnvironment processingEnvironment) {
        if (!isDeferredHolder(element, processingEnvironment.getElementUtils(), processingEnvironment.getTypeUtils())) {
            return super.test(element, args, processingEnvironment);
        }

        return super.test(resolveDeferredHolder(element), args, processingEnvironment);
    }
}
