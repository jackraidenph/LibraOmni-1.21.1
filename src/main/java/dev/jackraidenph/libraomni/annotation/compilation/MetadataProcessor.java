package dev.jackraidenph.libraomni.annotation.compilation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.RuntimeProcessor;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.util.ResourceUtilities;
import dev.jackraidenph.libraomni.util.data.ElementData;
import dev.jackraidenph.libraomni.util.data.Metadata;
import dev.jackraidenph.libraomni.util.data.MetadataFileManager;
import net.neoforged.fml.common.Mod;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

class MetadataProcessor extends AbstractCompilationProcessor {

    private final NavigableMap<String, String> packageToModId = new TreeMap<>();
    private final Set<String> modClasses = new HashSet<>();
    private final Map<String, Metadata> modMetadata = new HashMap<>();
    private final Map<String, SetMultimap<Scope, String>> modRuntimeProcessorsPerScope = new HashMap<>();
    private final Map<String, ElementData> modElementData = new HashMap<>();

    private final Set<Element> runtimeElements = new HashSet<>();

    private final SetMultimap<Scope, Element> runtimeProcessorElements = HashMultimap.create();

    private final Set<Class<? extends Annotation>> processableAnnotations = new HashSet<>();

    protected MetadataProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
        this.registerRuntimeAnnotations(processingEnvironment.getMessager());
    }

    public static final String RUNTIME_ANNOTATIONS_CONFIG = LibraOmni.MODID + ".runtime.json";
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STRING_SET_TYPE = new TypeToken<Set<String>>() {
    }.getType();

    private Set<String> readProcessableAnnotations() {
        return ResourceUtilities.getResourcesAsStrings(RUNTIME_ANNOTATIONS_CONFIG)
                .map(
                        s -> (Set<String>) GSON.fromJson(s, STRING_SET_TYPE)
                ).flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private void registerRuntimeAnnotations(Messager messager) {
        Set<String> annotations = this.readProcessableAnnotations();
        for (String className : annotations) {
            try {
                Class<?> clazz = Class.forName(className);
                if (Annotation.class.isAssignableFrom(clazz)) {
                    this.processableAnnotations.add((Class<? extends Annotation>) clazz);
                    messager.printNote("[" + className + "] was added as a runtime annotation");
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Failed to get class for name " + className, e);
            }
        }
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
            Scope scope = entry.getKey();
            for (Element element : entry.getValue()) {
                String name = ((TypeElement) element).getQualifiedName().toString();
                String pkg = ((PackageElement) element.getEnclosingElement()).getQualifiedName().toString();
                String modId = this.modIdByPackage(pkg);
                if (modId == null) {
                    this.messager().printWarning("Got runtime processor [" + name + "], but failed to compute the owning mod");
                    continue;
                }

                this.modRuntimeProcessorsPerScope.computeIfAbsent(modId, k -> HashMultimap.create()).get(scope).add(name);
            }
        }
    }

    private void addProcessorsToMetadata() {
        for (String mod : modClasses) {
            Metadata metadata = this.getOrCreateMetadata(mod);
            SetMultimap<Scope, String> processors = this.modRuntimeProcessorsPerScope.get(mod);
            for (Entry<Scope, Collection<String>> perScopeProcessors : processors.asMap().entrySet()) {
                Scope scope = perScopeProcessors.getKey();
                Collection<String> scopeProcessors = perScopeProcessors.getValue();
                metadata.getRuntimeProcessors(scope).addAll(scopeProcessors);
            }
        }
    }

    private void serializeElementData() {
        MetadataFileManager.Writer writer = MetadataFileManager.newWriter(this.filer());

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
        MetadataFileManager.Writer writer = MetadataFileManager.newWriter(this.filer());

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
