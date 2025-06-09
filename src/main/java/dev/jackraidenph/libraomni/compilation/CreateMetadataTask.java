package dev.jackraidenph.libraomni.compilation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import dev.jackraidenph.libraomni.annotation.*;
import dev.jackraidenph.libraomni.common.data.ModMetadata;
import dev.jackraidenph.libraomni.reflect.RuntimeTask.Scope;
import dev.jackraidenph.libraomni.common.data.ModAnnotatedData;
import dev.jackraidenph.libraomni.common.data.ModMetadataReader;

import javax.annotation.processing.Messager;
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

class CreateMetadataTask implements CompilationTask {
    private final Map<String, ModMetadata> modMetadata = new HashMap<>();
    private final Map<String, SetMultimap<Scope, String>> modRuntimeProcessorsPerScope = new HashMap<>();
    private final Map<String, ModAnnotatedData> modAnnotatedDataMap = new HashMap<>();

    private final Set<Element> runtimeElements = new HashSet<>();

    private final SetMultimap<Scope, Element> runtimeProcessorElements = HashMultimap.create();

    private final Set<String> processableAnnotations = new HashSet<>();

    //UTILITY START

    private ModMetadata getOrCreateMetadata(String modId) {
        return this.modMetadata.computeIfAbsent(modId, ModMetadata::new);
    }

    private String modIdByPackage(ModIdGetter modLocator, Element e) {
        if (modLocator == null) {
            return null;
        }
        return modLocator.forElement(e);
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
                .filter(CreateMetadataTask::isRuntimeAnnotation)
                .map(typeElement -> typeElement.getQualifiedName().toString())
                .collect(Collectors.toSet());
    }

    private TypeElement[] typesFromStrings(Set<String> names, Elements elements) {
        return names.stream().map(elements::getTypeElement).toArray(TypeElement[]::new);
    }

    @Override
    public Collection<Resource> processRound(ModIdGetter modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        Set<String> runtimeAnnotations = this.findRuntimeAnnotations(roundEnv);

        if (!runtimeAnnotations.isEmpty()) {
            this.processableAnnotations.addAll(runtimeAnnotations);
            processingEnv.getMessager().printNote("Found runtime annotations " + this.processableAnnotations);
        }

        this.runtimeElements.addAll(
                roundEnv.getElementsAnnotatedWithAny(
                        this.typesFromStrings(this.processableAnnotations, processingEnv.getElementUtils())
                )
        );

        for (Element e : roundEnv.getElementsAnnotatedWith(Processor.class)) {
            Processor annotation = e.getAnnotation(Processor.class);
            this.runtimeProcessorElements.get(annotation.value()).add(e);
        }

        return Set.of();
    }

    private void createModMetadata(ModIdGetter modLocator) {
        modLocator.mods().forEach(id -> this.modMetadata.computeIfAbsent(id, ModMetadata::new));
    }

    private void associateElements(ModIdGetter modLocator) {
        for (Element element : this.runtimeElements) {
            String modId = this.modIdByPackage(modLocator, element);
            if (modId == null) {
                continue;
            }

            this.modAnnotatedDataMap.computeIfAbsent(modId, ModAnnotatedData::new).addElement(element);
        }
    }

    private void addRuntimeProcessors(ModIdGetter modLocator, Messager messager) {
        for (Entry<Scope, Collection<Element>> entry : this.runtimeProcessorElements.asMap().entrySet()) {
            Scope scope = entry.getKey();
            for (Element element : entry.getValue()) {
                String name = ((TypeElement) element).getQualifiedName().toString();
                String modId = this.modIdByPackage(modLocator, element);
                if (modId == null) {
                    messager.printWarning("Got runtime processor [" + name + "], but failed to compute the owning mod");
                    continue;
                }

                this.modRuntimeProcessorsPerScope.computeIfAbsent(modId, k -> HashMultimap.create()).get(scope).add(name);
            }
        }
    }

    private void addProcessorsToMetadata(ModIdGetter modLocator) {
        for (String mod : modLocator.mods()) {
            ModMetadata modMetadata = this.getOrCreateMetadata(mod);
            SetMultimap<Scope, String> processors = this.modRuntimeProcessorsPerScope.get(mod);
            for (Entry<Scope, Collection<String>> perScopeProcessors : processors.asMap().entrySet()) {
                Scope scope = perScopeProcessors.getKey();
                Collection<String> scopeProcessors = perScopeProcessors.getValue();
                modMetadata.addRuntimeTasks(scope, scopeProcessors);
            }
        }
    }

    private Set<Resource> serialize() {
        Set<Resource> dataResources = new HashSet<>();
        for (ModAnnotatedData data : modAnnotatedDataMap.values()) {
            if (data.isEmpty()) {
                continue;
            }

            String modId = data.getModId();
            ModMetadata modMetadata = this.getOrCreateMetadata(modId);
            modMetadata.setAnnotatedData(data);
        }

        return dataResources;
    }

    private Set<Resource> serializeMetadata() {
        Set<Resource> dataResources = new HashSet<>();
        for (ModMetadata modMetadata : this.modMetadata.values()) {
            Resource resource = Resource
                    .json(modMetadata)
                    .directory(ModMetadataReader.DIRECTORY)
                    .name(ModMetadataReader.metadataFileRoot())
                    .build();
            dataResources.add(resource);
        }
        return dataResources;
    }

    @Override
    public Set<Resource> finish(ModIdGetter modLocator, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv) {
        this.createModMetadata(modLocator);
        this.associateElements(modLocator);
        this.addRuntimeProcessors(modLocator, processingEnv.getMessager());
        this.addProcessorsToMetadata(modLocator);
        return Sets.union(
                this.serialize(),
                this.serializeMetadata()
        );
    }

    //METADATA PIPELINE END
}
