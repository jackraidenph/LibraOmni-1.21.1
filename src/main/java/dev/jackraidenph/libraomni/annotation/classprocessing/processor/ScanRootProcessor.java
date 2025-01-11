package dev.jackraidenph.libraomni.annotation.classprocessing.processor;

import dev.jackraidenph.libraomni.annotation.classprocessing.processor.base.AbstractCompileTimeProcessor;
import dev.jackraidenph.libraomni.annotation.classprocessing.processor.base.CompileTimeProcessor;
import dev.jackraidenph.libraomni.annotation.instance.ScanRoot;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ScanRootProcessor extends AbstractCompileTimeProcessor {

    private final Map<String, String> packageToModId;

    public ScanRootProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
        this.packageToModId = new HashMap<>();
    }

    @Override
    public boolean onRound(RoundEnvironment roundEnvironment) {
        for (Element typeElement : roundEnvironment.getElementsAnnotatedWith(ScanRoot.class)) {
            ScanRoot annotation = typeElement.getAnnotation(ScanRoot.class);

            String pkg = CompileTimeProcessor.packageOf(this.getProcessingEnvironment(), typeElement)
                    .getQualifiedName()
                    .toString();

            String modId = this.getModId(pkg);

            if (modId != null && !annotation.value().equals(modId)) {
                throw new IllegalStateException("""
                        Found overlapping scan roots.
                        [%s] at [%s] overlaps [%s] at [%s]
                        """.formatted(modId, this.getPackageOfModId(modId), annotation.value(), pkg));
            }

            this.packageToModId.put(pkg, annotation.value());
        }
        return true;
    }

    private String getPackageOfModId(String modId) {
        for (Entry<String, String> entry : this.packageToModId.entrySet()) {
            if (entry.getValue().equals(modId)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public String getModId(String packageString) {
        String modId = this.packageToModId.get(packageString);
        if (modId != null) {
            return modId;
        }

        for (Entry<String, String> rootToModId : this.packageToModId.entrySet()) {
            if (packageString.startsWith(rootToModId.getKey())) {
                return this.packageToModId.get(rootToModId.getKey());
            }
        }

        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotationClasses() {
        return Set.of(
                ScanRoot.class
        );
    }
}
