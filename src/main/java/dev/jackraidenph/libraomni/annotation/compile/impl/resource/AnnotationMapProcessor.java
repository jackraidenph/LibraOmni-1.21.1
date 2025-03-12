package dev.jackraidenph.libraomni.annotation.compile.impl.resource;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.compile.impl.ScanRootProcessor;
import dev.jackraidenph.libraomni.annotation.compile.util.ElementUtils;
import dev.jackraidenph.libraomni.annotation.compile.util.SerializationHelper;
import dev.jackraidenph.libraomni.annotation.impl.Registered;
import dev.jackraidenph.libraomni.annotation.impl.AnnotationScanRoot;
import dev.jackraidenph.libraomni.util.ResourceUtilities;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationMapProcessor extends ResourceGeneratingProcessor {

    public static final String ROOT = "META-INF/" + LibraOmni.MODID + "/";

    public static final String ANNOTATION_MAP_FILE_SUFFIX = "annotations";

    public static final String ANNOTATION_REGISTRY_FILE_EXT = "list";
    public static final String ANNOTATION_MAP_FILE_EXT = "json";

    public static final String ANNOTATION_REGISTRY_FILE = LibraOmni.MODID + "." + ANNOTATION_MAP_FILE_SUFFIX + "." + ANNOTATION_REGISTRY_FILE_EXT;

    private final ScanRootProcessor scanRootProcessor;

    private final Map<String, Map<String, Map<ElementKind, Set<String>>>> targetsMap = new HashMap<>();


    public AnnotationMapProcessor(
            ProcessingEnvironment processingEnvironment,
            ScanRootProcessor rootProcessor
    ) {
        super(processingEnvironment, ROOT);
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

    private boolean processAnnotation(Class<? extends Annotation> annotation, RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(annotation)) {

            String pkg = this.getAndCheckPackage(element);
            String modId = this.getAndCheckModIdFromPackage(pkg);

            Map<String, Map<ElementKind, Set<String>>> annotationMap = new HashMap<>();
            this.targetsMap.put(modId, annotationMap);

            Map<ElementKind, Set<String>> elementTypeMap = Map.of(
                    ElementKind.CLASS, new HashSet<>(),
                    ElementKind.FIELD, new HashSet<>(),
                    ElementKind.CONSTRUCTOR, new HashSet<>(),
                    ElementKind.METHOD, new HashSet<>()
            );

            annotationMap.put(annotation.getCanonicalName(), elementTypeMap);

            elementTypeMap.get(element.getKind()).add(SerializationHelper.getElementString(element));
        }

        return true;
    }

    @Override
    public boolean processRound(RoundEnvironment roundEnvironment) {
        for (Class<? extends Annotation> annotation : this.supportedAnnotations()) {

            if (!this.processAnnotation(annotation, roundEnvironment)) {
                return false;
            }

            Target targetAnnotation = annotation.getAnnotation(Target.class);
            if (targetAnnotation == null) {
                this.getProcessingEnvironment().getMessager().printNote("No target specified for " + annotation);
                return false;
            }
        }

        return true;
    }

    @Override
    public Set<TransientResource> output(RoundEnvironment roundEnvironment) {
        Set<TransientResource> createdFiles = new HashSet<>();

        StringJoiner stringJoiner = new StringJoiner("\n");

        for (String modId : this.targetsMap.keySet()) {
            Map<String, Map<ElementKind, Set<String>>> jsonFileContentsObject = this.targetsMap.get(modId);
            String fileName = modId + "." + ANNOTATION_MAP_FILE_SUFFIX;

            createdFiles.add(TransientResource.json(fileName, jsonFileContentsObject));

            stringJoiner.add(fileName + "." + ANNOTATION_MAP_FILE_EXT);
        }

        createdFiles.add(TransientResource.fullName(ANNOTATION_REGISTRY_FILE, stringJoiner.toString()));

        return createdFiles;
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        return Set.of(
                Registered.class
        );
    }

    public static String registryLocation() {
        return ROOT + ANNOTATION_REGISTRY_FILE;
    }

    public static String annotationsFileLocation(String modId) {
        return ROOT + modId + "." + ANNOTATION_MAP_FILE_SUFFIX + "." + ANNOTATION_MAP_FILE_EXT;
    }

    public static Set<String> allAnnotationMaps() {
        return ResourceUtilities.getResourcesAsStrings(registryLocation())
                .flatMap(String::lines)
                .collect(Collectors.toSet());
    }

    public static String extractModNameFromMapFile(String name) {
        String endingString = "." + ANNOTATION_MAP_FILE_SUFFIX + "." + ANNOTATION_MAP_FILE_EXT;
        if (!name.endsWith(endingString)) {
            throw new IllegalArgumentException("The name is not a proper annotation map file");
        }

        return name.substring(0, name.indexOf(endingString));
    }
}
