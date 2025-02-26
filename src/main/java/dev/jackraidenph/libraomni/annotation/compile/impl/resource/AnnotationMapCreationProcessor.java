package dev.jackraidenph.libraomni.annotation.compile.impl.resource;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.compile.api.CompileTimeProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.AnnotationScanRootProcessor;
import dev.jackraidenph.libraomni.annotation.compile.util.SerializationHelper;
import dev.jackraidenph.libraomni.annotation.impl.Registered;
import dev.jackraidenph.libraomni.annotation.impl.AnnotationScanRoot;
import dev.jackraidenph.libraomni.util.ResourceUtilities;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationMapCreationProcessor extends AbstractResourceGeneratingProcessor {

    public static final String ROOT = "META-INF/" + LibraOmni.MODID + "/";

    public static final String ANNOTATION_MAP_FILE_SUFFIX = "annotations";

    public static final String ANNOTATION_REGISTRY_FILE_EXT = "list";
    public static final String ANNOTATION_MAP_FILE_EXT = "json";

    public static final String ANNOTATION_REGISTRY_FILE = LibraOmni.MODID + "." + ANNOTATION_MAP_FILE_SUFFIX + "." + ANNOTATION_REGISTRY_FILE_EXT;

    private final AnnotationScanRootProcessor annotationScanRootProcessor;

    private final Map<String, Map<String, Map<ElementKind, Set<String>>>> targetsMap = new HashMap<>();


    public AnnotationMapCreationProcessor(
            ProcessingEnvironment processingEnvironment,
            AnnotationScanRootProcessor rootProcessor
    ) {
        super(processingEnvironment, ROOT);
        this.annotationScanRootProcessor = rootProcessor;
    }

    @Override
    public boolean processRound(RoundEnvironment roundEnvironment) {
        for (Class<? extends Annotation> annotation : this.getSupportedAnnotationClasses()) {
            for (Element element : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                String pkg = CompileTimeProcessor.packageOf(this.getProcessingEnvironment(), element)
                        .getQualifiedName()
                        .toString();

                if (pkg == null) {
                    throw new IllegalStateException("Failed to capture element package");
                }

                String modId = this.annotationScanRootProcessor.getModId(pkg);

                if (modId == null) {
                    throw new IllegalStateException("""
                            Failed to compute mod id for package [%s].
                            Please, refer to [%s] JavaDoc.
                            """.formatted(pkg, AnnotationScanRoot.class));
                }

                Map<String, Map<ElementKind, Set<String>>> annotationMap = new HashMap<>();
                this.targetsMap.put(modId, annotationMap);

                Set<String> classes = new HashSet<>();
                Set<String> fields = new HashSet<>();
                Set<String> constructors = new HashSet<>();
                Set<String> methods = new HashSet<>();

                Map<ElementKind, Set<String>> elementTypeMap = Map.of(
                        ElementKind.CLASS, classes,
                        ElementKind.FIELD, fields,
                        ElementKind.CONSTRUCTOR, constructors,
                        ElementKind.METHOD, methods
                );

                annotationMap.put(annotation.getCanonicalName(), elementTypeMap);

                final SerializationHelper serializationHelper = SerializationHelper.INSTANCE;

                switch (element.getKind()) {
                    case CLASS -> classes.add(
                            serializationHelper.toClassString((TypeElement) element)
                    );
                    case FIELD -> fields.add(
                            serializationHelper.toFieldString((VariableElement) element)
                    );
                    case CONSTRUCTOR -> constructors.add(
                            serializationHelper.toConstructorString((ExecutableElement) element)
                    );
                    case METHOD -> methods.add(
                            serializationHelper.toMethodString((ExecutableElement) element)
                    );
                    default -> throw new IllegalStateException();
                }
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

            createdFiles.add(
                    TransientResource.json(
                            fileName,
                            jsonFileContentsObject
                    )
            );

            stringJoiner.add(fileName + "." + ANNOTATION_MAP_FILE_EXT);
        }
        createdFiles.add(
                TransientResource.fullName(
                        ANNOTATION_REGISTRY_FILE,
                        stringJoiner.toString()
                )
        );

        return createdFiles;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotationClasses() {
        return Set.of(
                Registered.class
        );
    }

    public static String registryLocation() {
        return ROOT + ANNOTATION_REGISTRY_FILE;
    }

    public static String annotationsForModId(String modId) {
        return ROOT + modId + "." + ANNOTATION_MAP_FILE_SUFFIX + "." + ANNOTATION_MAP_FILE_EXT;
    }

    public static Set<String> allAnnotationMaps() {
        return ResourceUtilities.getResources(registryLocation()).flatMap(url -> {
            try (InputStream inputStream = url.openStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).lines();
            } catch (IOException e) {
                LibraOmni.LOGGER.info("Failed to gather maps for {}", url.getPath());
                return Stream.empty();
            }
        }).collect(Collectors.toSet());
    }

    public static String extractModNameFromMapFile(String name) {
        String endingString = "." + ANNOTATION_MAP_FILE_SUFFIX + "." + ANNOTATION_MAP_FILE_EXT;
        if (!name.endsWith(endingString)) {
            throw new IllegalArgumentException("The name is not a proper annotation map file");
        }

        return name.substring(0, name.indexOf(endingString));
    }
}
