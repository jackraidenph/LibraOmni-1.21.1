package dev.jackraidenph.libraomni.processor;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ModIdGetter {
    private final NavigableMap<String, String> packageToModId = new TreeMap<>();

    private static String getModId(Element e, String annotationName, String valueName) {
        AnnotationMirror foundMirror = null;

        for (AnnotationMirror annotationMirror : e.getAnnotationMirrors()) {
            TypeElement annotationElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            if (annotationElement.getQualifiedName().contentEquals(annotationName)) {
                foundMirror = annotationMirror;
            }
        }

        if (foundMirror == null) {
            return null;
        }

        ExecutableElement executableElement = foundMirror.getElementValues()
                .keySet()
                .stream()
                .filter(executable -> executable.getSimpleName().contentEquals(valueName))
                .findFirst()
                .orElse(null);

        return String.valueOf(foundMirror.getElementValues().get(executableElement).getValue());
    }

    public void findMods(TypeElement modAnnotationType, RoundEnvironment roundEnvironment, Messager messager) {
        roundEnvironment.getElementsAnnotatedWith(modAnnotationType)
                .forEach(e -> {
                    String modId = getModId(e, CompilationTaskProcessor.NF_MOD_ANNOTATION_CLASS_NAME, "value");
                    if (modId == null) {
                        return;
                    }
                    String pkg = getPackageOf(e);
                    messager.printNote("Found mod [" + modId + "] at [" + pkg + "]");
                    packageToModId.put(pkg, modId);
                });
    }

    private String getPackageOf(Element e) {
        Element enclosing = e;
        while (enclosing.getKind() != ElementKind.PACKAGE) {
            enclosing = enclosing.getEnclosingElement();
            if (enclosing == null) {
                return null;
            }
        }

        return ((PackageElement) enclosing).getQualifiedName().toString();
    }

    public String forPackage(String pkg) {
        Entry<String, String> entry = packageToModId.floorEntry(pkg);
        return entry == null ? null : entry.getValue();
    }

    public String forElement(Element element) {
        return forPackage(getPackageOf(element));
    }

    public Collection<String> mods() {
        return this.packageToModId.values();
    }
}
