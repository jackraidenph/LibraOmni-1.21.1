package dev.jackraidenph.libraomni.compilation;

import net.neoforged.fml.common.Mod;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.*;
import java.util.Map.Entry;

class ModIdGetter {
    private final NavigableMap<String, String> packageToModId = new TreeMap<>();

    public void findMods(RoundEnvironment roundEnvironment, Messager messager) {
        roundEnvironment.getElementsAnnotatedWith(Mod.class)
                .forEach(e -> {
                    TypeElement modClass = (TypeElement) e;
                    Mod modAnnotation = modClass.getAnnotation(Mod.class);
                    String modId = modAnnotation.value();
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
