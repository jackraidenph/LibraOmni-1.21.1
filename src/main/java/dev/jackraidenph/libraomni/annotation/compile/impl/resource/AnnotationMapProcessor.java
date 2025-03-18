package dev.jackraidenph.libraomni.annotation.compile.impl.resource;

import dev.jackraidenph.libraomni.annotation.compile.impl.AbstractCompilationProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.ScanRootProcessor;
import dev.jackraidenph.libraomni.annotation.compile.util.ElementData;
import dev.jackraidenph.libraomni.annotation.compile.util.ElementUtils;
import dev.jackraidenph.libraomni.annotation.compile.util.MetadataFileManager;
import dev.jackraidenph.libraomni.annotation.impl.Registered;
import dev.jackraidenph.libraomni.annotation.impl.AnnotationScanRoot;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.FileObject;
import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;
import java.util.Map.Entry;

public class AnnotationMapProcessor extends AbstractCompilationProcessor {

    private final ScanRootProcessor scanRootProcessor;

    public AnnotationMapProcessor(
            ProcessingEnvironment processingEnvironment,
            ScanRootProcessor rootProcessor
    ) {
        super(processingEnvironment);
        this.scanRootProcessor = rootProcessor;
    }

    private String getAndCheckModIdFromPackage(String pkg) {
        String modId = this.scanRootProcessor.modIdFromPackage(pkg);

        if (modId == null) {
            throw new IllegalStateException("""
                    Failed to compute mod id for package [%s].
                    Please, refer to [%s] JavaDoc.
                    """.formatted(pkg, AnnotationScanRoot.class));
        }

        return modId;
    }

    private String getAndCheckPackage(Element element) {
        String pkg = ElementUtils.packageOf(this.getProcessingEnvironment(), element)
                .getQualifiedName()
                .toString();

        if (pkg == null) {
            throw new IllegalStateException("Failed to capture element package");
        }

        return pkg;
    }

    private final Map<String, ElementData> dataMap = new HashMap<>();

    private ElementData getOrCreateElementData(String modId) {
        return dataMap.computeIfAbsent(modId, id -> new ElementData());
    }

    private void processAnnotation(Class<? extends Annotation> annotation, RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(annotation)) {
            String pkg = this.getAndCheckPackage(element);
            String modId = this.getAndCheckModIdFromPackage(pkg);
            this.getOrCreateElementData(modId).addElement(element);
        }

    }

    @Override
    public boolean processRound(RoundEnvironment roundEnvironment) {
        for (Class<? extends Annotation> annotation : this.supportedAnnotations()) {
            Retention retention = annotation.getAnnotation(Retention.class);
            if (retention == null || !retention.value().equals(RetentionPolicy.RUNTIME)) {
                continue;
            }

            this.processAnnotation(annotation, roundEnvironment);
        }

        return true;
    }

    @Override
    public boolean finish(RoundEnvironment roundEnvironment) {
        Filer filer = this.getProcessingEnvironment().getFiler();
        Messager messager = this.getProcessingEnvironment().getMessager();
        MetadataFileManager.Writer writer = MetadataFileManager.writer(filer);
        for (Entry<String, ElementData> dataEntry : this.dataMap.entrySet()) {
            String modId = dataEntry.getKey();
            ElementData elements = dataEntry.getValue();

            try {
                FileObject file = writer.writeElementData(modId, elements);
                messager.printNote("Created elements data for [" + modId + "]: " + FilenameUtils.getName(file.getName()));
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }

        return true;
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(
                Registered.class
        );
    }
}
