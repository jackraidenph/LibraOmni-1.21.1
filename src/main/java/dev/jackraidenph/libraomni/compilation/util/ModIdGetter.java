package dev.jackraidenph.libraomni.compilation.util;

import dev.jackraidenph.libraomni.annotation.datagen.WithName;
import dev.jackraidenph.libraomni.compilation.CompileConstants;
import dev.jackraidenph.libraomni.exception.AlreadyInitializedException;
import dev.jackraidenph.libraomni.util.ObjectOriginGetter;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.util.StringUtil;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.util.*;
import java.util.Map.Entry;

public class ModIdGetter implements ObjectOriginGetter {
    private final NavigableMap<String, String> packageToModId = new TreeMap<>();
    private final Map<String, List<String>> modClasses = new HashMap<>();

    public List<String> getModClasses(String modId) {
        return modClasses.getOrDefault(modId, List.of());
    }

    public Map<String, List<String>> getModClassesMap() {
        return Collections.unmodifiableMap(modClasses);
    }

    @Override
    public @NonNull String getObjectName(Object object) {
        if (!(object instanceof Element element)) {
            return SafeReflectionUtil.simpleObjectName(object);
        }

        WithName nameInfo = element.getAnnotation(WithName.class);
        if (nameInfo != null) {
            return nameInfo.value();
        }

        return StringUtil.snakeCase(element.getSimpleName().toString());
    }

    @Override
    public @Nullable String getOriginModId(Object object) {
        if (!(object instanceof Element element)) {
            return modIdByPackage(object.getClass().getPackageName());
        }

        return modIdByElement(element);
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

    private boolean init = false;

    public void setInitialized(boolean value) {
        init = value;
    }

    public void discoverMods(TypeElement modAnnotationType, String annotationValue, RoundEnvironment roundEnvironment, Messager messager) {
        if (init) {
            throw new AlreadyInitializedException();
        }

        roundEnvironment.getElementsAnnotatedWith(modAnnotationType)
                .forEach(e -> {
                    String modId = getModId(e, modAnnotationType, annotationValue);
                    if (modId == null) {
                        return;
                    }
                    if (modAnnotationType.getQualifiedName().contentEquals(CompileConstants.NF_MOD_ANNOTATION_CLASS_NAME)) {
                        String className = ((TypeElement) e).getQualifiedName().toString();
                        modClasses.computeIfAbsent(modId, i -> new ArrayList<>()).add(className);
                    }
                    String pkg = getPackageOf(e);
                    String existing = modIdByPackage(pkg);
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

    public String modIdByPackage(String pkg) {
        Entry<String, String> entry = packageToModId.floorEntry(pkg);
        return entry == null ? null : entry.getValue();
    }

    public String modIdByElement(Element element) {
        return modIdByPackage(getPackageOf(element));
    }

    public Collection<String> discoveredMods() {
        return this.packageToModId.values();
    }
}
