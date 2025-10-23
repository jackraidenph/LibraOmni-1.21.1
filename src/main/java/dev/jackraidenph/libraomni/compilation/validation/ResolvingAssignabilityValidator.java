package dev.jackraidenph.libraomni.compilation.validation;

/// To be decided if needed at all

//import dev.jackraidenph.libraomni.common.ElementUtil;
//import dev.jackraidenph.libraomni.common.ValidationUtils;
//
//import javax.annotation.processing.ProcessingEnvironment;
//import javax.lang.model.element.Element;
//import javax.lang.model.type.DeclaredType;
//import javax.lang.model.type.TypeMirror;
//
//public abstract class ResolvingAssignabilityValidator extends AssignabilityValidator {
//
//    @Override
//    public boolean test(Element element, ProcessingEnvironment processingEnvironment) {
//        String toExtendOrImplement = classNameToValidateAgainst();
//
//        TypeMirror resolved = ElementUtil.resolveFunctionalReturnType(element, processingEnvironment.getTypeUtils());
//        if (resolved instanceof DeclaredType declaredType && ValidationUtils.elementImplementsOrExtendsAny(declaredType.asElement(), toExtendOrImplement)) {
//            return true;
//        }
//
//        return super.test(element, processingEnvironment);
//    }
//
//}
