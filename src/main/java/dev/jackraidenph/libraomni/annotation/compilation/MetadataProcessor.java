package dev.jackraidenph.libraomni.annotation.compilation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import dev.jackraidenph.libraomni.annotation.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.util.data.ElementData;
import dev.jackraidenph.libraomni.util.data.Metadata;
import dev.jackraidenph.libraomni.util.data.MetadataFileManager;
import net.neoforged.fml.common.Mod;
import org.apache.commons.io.FilenameUtils;


import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.FileObject;
import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;
import java.util.Map.Entry;

class MetadataProcessor extends AbstractCompilationProcessor {

    private final NavigableMap<String, String> packageToModId = new TreeMap<>();
    private final Set<String> modClasses = new HashSet<>();
    private final Map<String, Metadata> modMetadata = new HashMap<>();
    private final Map<Scope, SetMultimap<String, String>> modRuntimeProcessorsPerScope = new HashMap<>();
    private final Map<String, ElementData> modElementData = new HashMap<>();

    private final Set<Element> runtimeElements = new HashSet<>();

    private final SetMultimap<Scope, Element> runtimeProcessorElements = HashMultimap.create();

    private final Set<Class<? extends Annotation>> processableAnnotations = new HashSet<>();

    protected MetadataProcessor(
            ProcessingEnvironment processingEnvironment,
            Collection<Class<? extends Annotation>> processableAnnotations
    ) {
        super(processingEnvironment);
        this.processableAnnotations.addAll(processableAnnotations);
    }

    //UTILITY START

    private String getPackage(Element element) {
        return this.getProcessingEnvironment()
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
    }

    private Metadata getOrCreateMetadata(String modId) {
        return this.modMetadata.computeIfAbsent(modId, Metadata::new);
    }

    private void addMod(Element modClass) {
        Messager messager = this.getProcessingEnvironment().getMessager();
        Mod modAnnotation = modClass.getAnnotation(Mod.class);
        String pkg = this.getPackage(modClass);
        String modId = modAnnotation.value();

        if (modId == null) {
            throw new IllegalStateException("Failed to get value from @Mod annotation");
        }

        this.packageToModId.put(pkg, modId);
        this.modClasses.add(modId);

        messager.printNote("Using @Mod[" + modId + "] annotation as annotation scan root");
    }

    private String modIdByPackage(String pkg) {
        Entry<String, String> entry = this.packageToModId.floorEntry(pkg);
        return entry == null ? null : entry.getValue();
    }

    private String modIdForElement(Element element) {
        String pkg = this.getPackage(element);
        return this.modIdByPackage(pkg);
    }

    //UTILITY END
    //METADATA PIPELINE START

    @Override
    public void processRound(RoundEnvironment roundEnvironment) {
        roundEnvironment.getElementsAnnotatedWith(Mod.class).forEach(this::addMod);

        this.runtimeElements.addAll(
                roundEnvironment.getElementsAnnotatedWithAny(this.processableAnnotations)
        );

        for (Element e : roundEnvironment.getElementsAnnotatedWith(RuntimeProcessor.class)) {
            RuntimeProcessor annotation = e.getAnnotation(RuntimeProcessor.class);
            this.runtimeProcessorElements.get(annotation.value()).add(e);
        }
    }

    private void createModMetadata() {
        this.modClasses.forEach(id -> this.modMetadata.computeIfAbsent(id, Metadata::new));
    }

    private void associateElements() {
        for (Element element : this.runtimeElements) {
            String modId = this.modIdForElement(element);
            if (modId == null) {
                continue;
            }

            this.modElementData.computeIfAbsent(modId, ElementData::new).addElement(element);
        }
    }

    private void addRuntimeProcessors() {
        for (Entry<Scope, Collection<Element>> entry : this.runtimeProcessorElements.asMap().entrySet()) {
            for (Element element : entry.getValue()) {
                String name = ((TypeElement) element).getQualifiedName().toString();
                String pkg = ((PackageElement) element.getEnclosingElement()).getQualifiedName().toString();
                String modId = this.modIdByPackage(pkg);
                if (modId == null) {
                    this.messager().printWarning("Got runtime processor [" + name + "], but failed to compute the owning mod");
                    continue;
                }

                this.modRuntimeProcessorsPerScope.computeIfAbsent(entry.getKey(), k -> HashMultimap.create()).get(modId).add(name);
            }
        }
    }

    private void addProcessorsToMetadata() {
        for (String mod : modClasses) {
            Metadata metadata = this.getOrCreateMetadata(mod);
            for (Scope scope : Scope.values()) {
                SetMultimap<String, String> processors = this.modRuntimeProcessorsPerScope.get(scope);
                if (processors != null) {
                    processors.get(mod).forEach(rp -> metadata.addRuntimeProcessorClass(scope, rp));
                }
            }
        }
    }

    private void serializeElementData() {
        MetadataFileManager.Writer writer = MetadataFileManager.writer(this.filer());

        for (ElementData data : modElementData.values()) {
            if (data.isEmpty()) {
                continue;
            }

            try {
                String modId = data.getModId();
                FileObject file = writer.writeElementData(data);
                this.messager().printNote(
                        "Created elements data for [" + data.getModId() + "]: " + FilenameUtils.getBaseName(file.getName())
                );

                Metadata metadata = this.getOrCreateMetadata(modId);
                metadata.setElementDataPath(MetadataFileManager.Writer.elementDataResource(data));
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
    }

    private void serializeMetadata() {
        MetadataFileManager.Writer writer = MetadataFileManager.writer(this.filer());

        for (Metadata metadata : this.modMetadata.values()) {
            try {
                FileObject file = writer.writeMetadata(metadata);
                this.messager().printNote(
                        "Created metadata for [" + metadata.getModId() + "]: " + FilenameUtils.getBaseName(file.getName())
                );
            } catch (IOException ioException) {
                throw new RuntimeException(ioException);
            }
        }
    }

    @Override
    public void finish(RoundEnvironment roundEnvironment) {
        this.createModMetadata();
        this.associateElements();
        this.addRuntimeProcessors();
        this.addProcessorsToMetadata();
        this.serializeElementData();
        this.serializeMetadata();
    }

    //METADATA PIPELINE END

    private Messager messager() {
        return this.getProcessingEnvironment().getMessager();
    }

    private Filer filer() {
        return this.getProcessingEnvironment().getFiler();
    }
}
