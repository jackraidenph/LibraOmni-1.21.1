package dev.jackraidenph.libraomni.annotation.compilation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

class ElementUtils {
    protected static PackageElement packageOf(ProcessingEnvironment processingEnvironment, Element element) {
        return processingEnvironment.getElementUtils().getPackageOf(element);
    }

    protected static String qualifiedName(PackageElement packageElement) {
        return packageElement.getQualifiedName().toString();
    }

    protected static String qualifiedPackageName(ProcessingEnvironment processingEnvironment, Element element) {
        return qualifiedName(packageOf(processingEnvironment, element));
    }
}
