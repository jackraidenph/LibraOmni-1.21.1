package dev.jackraidenph.libraomni.compilation.util;

import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;

import dev.jackraidenph.libraomni.common.StringUtilities;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.util.*;
import java.util.Map.Entry;

public class ModIdGetter {
    private final NavigableMap<String, String> packageToModId = new TreeMap<>();
    private final Map<String, List<String>> modClasses = new HashMap<>();

    public List<String> getModClasses(String modId) {
        return modClasses.getOrDefault(modId, List.of());
    }

    public Map<String, List<String>> getModClassesMap() {
        return Collections.unmodifiableMap(modClasses);
    }

    public static String getElementId(Element e) {
//        Id id = e.getAnnotation(Id.class);
//        if (id != null && !id.value().isBlank()) {
//            return id.value();
//        }

        return StringUtilities.snakeCase(e.getSimpleName().toString());
    }

    private static String getModId(Element e, TypeElement annotationToSearch, String valueName) {
        AnnotationMirror foundMirror = null;

        for (AnnotationMirror annotationMirror : e.getAnnotationMirrors()) {
            TypeElement annotationElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
            if (annotationElement.equals(annotationToSearch)) {
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

    public void findMods(TypeElement modAnnotationType, String annotationValue, RoundEnvironment roundEnvironment, Messager messager) {
        roundEnvironment.getElementsAnnotatedWith(modAnnotationType)
                .forEach(e -> {
                    String modId = getModId(e, modAnnotationType, annotationValue);
                    if (modId == null) {
                        return;
                    }
                    if (modAnnotationType.getQualifiedName().contentEquals(AnnotationProcessorConstants.NF_MOD_ANNOTATION_CLASS_NAME)) {
                        String className = ((TypeElement) e).getQualifiedName().toString();
                        modClasses.computeIfAbsent(modId, i -> new ArrayList<>()).add(className);
                    }
                    String pkg = getPackageOf(e);
                    String existing = forPackage(pkg);
                    if (existing == null) {
                        messager.printNote("Found mod [" + modId + "] at [" + pkg + "]");
                        packageToModId.put(pkg, modId);
                    } else if (existing.equals(modId)) {
                        messager.printWarning("Package [" + pkg + "] already points to [" + packageToModId.get(pkg) + "], skipping. If both @Mod and @ModRoot are present on a class, remove @ModRoot, @Mod is enough.");
                    } else {
                        messager.printNote("Package [" + packageToModId.floorKey(pkg) + "] is claimed by [" + existing + "], reclaiming [" + pkg + "] for [" + modId + "]");
                        packageToModId.put(pkg, modId);
                    }
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
