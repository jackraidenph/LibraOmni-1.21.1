package dev.jackraidenph.libraomni.annotation.compilation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import dev.jackraidenph.libraomni.annotation.*;
import dev.jackraidenph.libraomni.annotation.compilation.CompilationProcessorsManager.ModLocator;
import dev.jackraidenph.libraomni.annotation.runtime.RuntimeProcessor.Scope;
import dev.jackraidenph.libraomni.util.data.ElementData;
import dev.jackraidenph.libraomni.util.data.Metadata;
import dev.jackraidenph.libraomni.util.data.MetadataFileReader;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

class MetadataProcessor extends ResourceGeneratingProcessor {
    private final Map<String, Metadata> modMetadata = new HashMap<>();
    private final Map<String, SetMultimap<Scope, String>> modRuntimeProcessorsPerScope = new HashMap<>();
    private final Map<String, ElementData> modElementData = new HashMap<>();

    private final Set<Element> runtimeElements = new HashSet<>();

    private final SetMultimap<Scope, Element> runtimeProcessorElements = HashMultimap.create();

    private final Set<String> processableAnnotations = new HashSet<>();

    protected MetadataProcessor(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    //UTILITY START

    private Metadata getOrCreateMetadata(String modId) {
        return this.modMetadata.computeIfAbsent(modId, Metadata::new);
    }

    private ModLocator modLocator() {
        return CompilationProcessorsManager.runningModLocator();
    }

    private String modIdByPackage(Element e) {
        ModLocator modLocator = this.modLocator();
        if (modLocator == null) {
            return null;
        }
        return modLocator.modId(e);
    }

    //UTILITY END
    //METADATA PIPELINE START

    private static boolean isRuntimeAnnotation(Element e) {
        if (!(e instanceof TypeElement annotationType)) {
            return false;
        }

        Retention retention = annotationType.getAnnotation(Retention.class);
        if (retention == null) {
            return false;
        }

        return retention.value().equals(RetentionPolicy.RUNTIME);
    }

    private Set<String> findRuntimeAnnotations(RoundEnvironment roundEnvironment) {
        return roundEnvironment
                .getRootElements()
                .stream()
                .flatMap(e -> e.getAnnotationMirrors().stream())
                .map(am -> (TypeElement) am.getAnnotationType().asElement())
                .filter(e -> e.getAnnotation(NeedsRuntimeProcessing.class) != null)
                .filter(MetadataProcessor::isRuntimeAnnotation)
                .map(typeElement -> typeElement.getQualifiedName().toString())
                .collect(Collectors.toSet());
    }

    private TypeElement[] typesFromStrings(Set<String> names) {
        Elements elements = this.processingEnvironment().getElementUtils();
        return names.stream().map(elements::getTypeElement).toArray(TypeElement[]::new);
    }

    @Override
    public void processRound(RoundEnvironment roundEnvironment) {
        Set<String> runtimeAnnotations = this.findRuntimeAnnotations(roundEnvironment);

        if (!runtimeAnnotations.isEmpty()) {
            this.processableAnnotations.addAll(runtimeAnnotations);
            this.messager().printNote("Found runtime annotations " + this.processableAnnotations);
        }

        this.runtimeElements.addAll(
                roundEnvironment.getElementsAnnotatedWithAny(
                        this.typesFromStrings(this.processableAnnotations)
                )
        );

        for (Element e : roundEnvironment.getElementsAnnotatedWith(Processor.class)) {
            Processor annotation = e.getAnnotation(Processor.class);
            this.runtimeProcessorElements.get(annotation.value()).add(e);
        }
    }

    private void createModMetadata() {
        this.modLocator().mods().forEach(id -> this.modMetadata.computeIfAbsent(id, Metadata::new));
    }

    private void associateElements() {
        for (Element element : this.runtimeElements) {
            String modId = this.modIdByPackage(element);
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
                String modId = this.modIdByPackage(element);
                if (modId == null) {
                    this.messager().printWarning("Got runtime processor [" + name + "], but failed to compute the owning mod");
                    continue;
                }

                this.modRuntimeProcessorsPerScope.computeIfAbsent(modId, k -> HashMultimap.create()).get(scope).add(name);
            }
        }
    }

    private void addProcessorsToMetadata() {
        for (String mod : this.modLocator().mods()) {
            Metadata metadata = this.getOrCreateMetadata(mod);
            SetMultimap<Scope, String> processors = this.modRuntimeProcessorsPerScope.get(mod);
            for (Entry<Scope, Collection<String>> perScopeProcessors : processors.asMap().entrySet()) {
                Scope scope = perScopeProcessors.getKey();
                Collection<String> scopeProcessors = perScopeProcessors.getValue();
                metadata.addRuntimeProcessors(scope, scopeProcessors);
            }
        }
    }

    private Set<Resource> serializeElementData() {
        Set<Resource> dataResources = new HashSet<>();
        for (ElementData data : modElementData.values()) {
            if (data.isEmpty()) {
                continue;
            }

            String modId = data.getModId();
            Resource resource = Resource.json(MetadataFileReader.DIRECTORY, MetadataFileReader.elementDataFileRoot(modId), data);
            Metadata metadata = this.getOrCreateMetadata(modId);
            metadata.setElementDataPath(resource.path());
            dataResources.add(resource);
        }

        return dataResources;
    }

    private Set<Resource> serializeMetadata() {
        Set<Resource> dataResources = new HashSet<>();
        for (Metadata metadata : this.modMetadata.values()) {
            Resource resource = Resource.json(MetadataFileReader.DIRECTORY, MetadataFileReader.metadataFileRoot(), metadata);
            dataResources.add(resource);
        }
        return dataResources;
    }

    @Override
    public Set<Resource> output(RoundEnvironment roundEnvironment) {
        this.createModMetadata();
        this.associateElements();
        this.addRuntimeProcessors();
        this.addProcessorsToMetadata();
        return Sets.union(
                this.serializeElementData(),
                this.serializeMetadata()
        );
    }

    //METADATA PIPELINE END
}
