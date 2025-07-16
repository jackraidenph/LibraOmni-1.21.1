package dev.jackraidenph.libraomni.processor.validation;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class BlockItemGenerationValidator extends AssignabilityValidator {

    @Override
    public boolean test(Element element, ProcessingEnvironment processingEnvironment) {

        String toExtendOrImplement = classNameToValidateAgainst();
        TypeMirror resolved = ValidationUtils.resolveFunctionalReturnType(element, processingEnvironment.getTypeUtils());
        if (resolved instanceof DeclaredType declaredType && ValidationUtils.elementImplementsOrExtendsAny(declaredType.asElement(), toExtendOrImplement)) {
            return true;
        }

        return super.test(element, processingEnvironment);
    }

    @Override
    protected @NotNull String classNameToValidateAgainst() {
        return "net.minecraft.world.level.block.Block";
    }
}
