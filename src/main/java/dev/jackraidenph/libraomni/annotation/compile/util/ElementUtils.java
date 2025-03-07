package dev.jackraidenph.libraomni.annotation.compile.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

public class ElementUtils {
    public static PackageElement packageOf(ProcessingEnvironment processingEnvironment, Element element) {
        return processingEnvironment.getElementUtils().getPackageOf(element);
    }

    public static String qualifiedName(PackageElement packageElement) {
        return packageElement.getQualifiedName().toString();
    }

    public static String qualifiedPackageName(ProcessingEnvironment processingEnvironment, Element element) {
        return qualifiedName(packageOf(processingEnvironment, element));
    }
}
