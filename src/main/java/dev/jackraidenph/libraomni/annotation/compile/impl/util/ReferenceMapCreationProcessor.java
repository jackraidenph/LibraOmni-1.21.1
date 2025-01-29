package dev.jackraidenph.libraomni.annotation.compile.impl.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.annotation.compile.api.CompileTimeProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.AbstractCompileTimeProcessor;
import dev.jackraidenph.libraomni.annotation.compile.impl.ScanRootProcessor;
import dev.jackraidenph.libraomni.annotation.compile.util.SerializationHelper;
import dev.jackraidenph.libraomni.annotation.impl.Register;
import dev.jackraidenph.libraomni.annotation.impl.ScanRoot;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.util.*;

public class ReferenceMapCreationProcessor extends AbstractCompileTimeProcessor {

    public static final String REGISTRY_LOCATION = LibraOmni.MODID + ".marked.registry";

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final SerializationHelper serializationHelper;
    private final ScanRootProcessor scanRootProcessor;

    private final Map<String, Map<String, Map<String, Set<String>>>> targetsMap = new HashMap<>();

    public ReferenceMapCreationProcessor(
            ProcessingEnvironment processingEnvironment,
            SerializationHelper serializationHelper,
            ScanRootProcessor rootProcessor
    ) {
        super(processingEnvironment);
        this.serializationHelper = serializationHelper;
        this.scanRootProcessor = rootProcessor;
    }

    @Override
    public boolean onRound(RoundEnvironment roundEnvironment) {
        for (Class<? extends Annotation> annotation : this.getSupportedAnnotationClasses()) {
            for (Element element : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                String pkg = CompileTimeProcessor.packageOf(this.getProcessingEnvironment(), element)
                        .getQualifiedName()
                        .toString();

                if (pkg == null) {
                    throw new IllegalStateException("Failed to capture element package");
                }

                String modId = this.scanRootProcessor.getModId(pkg);

                if (modId == null) {
                    throw new IllegalStateException("""
                            Failed to compute mod id for package [%s].
                            Please, refer to [%s] JavaDoc.
                            """.formatted(pkg, ScanRoot.class));
                }

                Map<String, Set<String>> targets = this.targetsMap
                        .computeIfAbsent(modId, k -> new HashMap<>())
                        .computeIfAbsent(annotation.getCanonicalName(), k -> new HashMap<>());

                String kind = element.getKind().toString();
                switch (element.getKind()) {
                    case CLASS -> targets.computeIfAbsent(
                            kind,
                            k -> new HashSet<>()
                    ).add(this.serializationHelper.toClassString((TypeElement) element));

                    case FIELD -> targets.computeIfAbsent(
                            kind,
                            k -> new HashSet<>()
                    ).add(this.serializationHelper.toFieldString((VariableElement) element));

                    case CONSTRUCTOR -> targets.computeIfAbsent(
                            kind,
                            k -> new HashSet<>()
                    ).add(this.serializationHelper.toConstructorString((ExecutableElement) element));

                    case METHOD -> targets.computeIfAbsent(
                            kind,
                            k -> new HashSet<>()
                    ).add(this.serializationHelper.toMethodString((ExecutableElement) element));

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
    public boolean onFinish(RoundEnvironment roundEnvironment) {
        StringJoiner stringJoiner = new StringJoiner("\n");
        Filer filer = this.getProcessingEnvironment().getFiler();

        for (String modId : this.targetsMap.keySet()) {
            String toWrite = this.GSON.toJson(this.targetsMap.get(modId));
            String location = modId + ".marked.json";

            this.write(location, filer, toWrite);

            stringJoiner.add(location);
        }
        this.write(REGISTRY_LOCATION, filer, stringJoiner.toString());

        return true;
    }

    @Override
    public Set<Class<? extends Annotation>> getSupportedAnnotationClasses() {
        return Set.of(
                Register.class
        );
    }

    private void write(String location, Filer filer, String toWrite) {
        try {
            FileObject fileObject = filer.createResource(
                    StandardLocation.CLASS_OUTPUT, LibraOmni.MODID, location
            );
            Writer writer = fileObject.openWriter();
            writer.write(toWrite);
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
