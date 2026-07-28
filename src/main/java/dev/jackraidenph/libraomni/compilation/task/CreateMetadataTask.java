package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.meta.NeedsRuntimeProcessing;
import dev.jackraidenph.libraomni.compilation.util.JsonMergeHelper.ConflictPolicy;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.data.ProjectMetadata;
import dev.jackraidenph.libraomni.util.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.util.ElementUtil;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;

final class CreateMetadataTask implements CompilationTask {

    private final ProjectMetadata projectMetadata = new ProjectMetadata();

    @Override
    public void processRound(ProcessingContext processingContext) {
        RoundEnvironment roundEnvironment = processingContext.roundEnvironment();

        Messager messager = processingContext.processingEnvironment().getMessager();
        Set<TypeElement> runtimeAnnotations = new LinkedHashSet<>();
        Set<Element> annotated = new LinkedHashSet<>();

        for (Element e : ElementUtil.getAllElements(roundEnvironment)) {
            boolean needsRuntimeProcessing = false;
            for (AnnotationMirror m : ElementUtil.Javac.getAllAnnotationMirrors(e)) {
                if (isMirrorSupported(m)) {
                    TypeElement type = AnnotationMirrorUtil.toTypeElement(m);
                    runtimeAnnotations.add(type);
                    needsRuntimeProcessing = true;
                }
            }
            if (needsRuntimeProcessing) {
                annotated.add(e);
            }
        }

        if (!runtimeAnnotations.isEmpty()) {
            messager.printNote("Found runtime annotations " + runtimeAnnotations);
        }

        for (Element e : annotated) {
            String modId = processingContext.modIdGetter().modIdByElement(e);
            if (modId == null) {
                continue;
            }

            projectMetadata.getOrCreateModMetadata(modId).getAnnotatedData().addElement(
                    e,
                    processingContext.processingEnvironment().getElementUtils()
            );
        }

        saveMetadataFile(processingContext);
    }

    @Override
    public boolean isMirrorSupported(AnnotationMirror mirror) {
        return AnnotationMirrorUtil.toTypeElement(mirror).getAnnotation(NeedsRuntimeProcessing.class) != null;
    }

    private void saveMetadataFile(ProcessingContext processingContext) {
        processingContext.resourceManager().saveAndCache(
                ResourceIdentifier.builder()
                        .setDirectory(ProjectMetadata.DIRECTORY)
                        .setNameRoot(ProjectMetadata.FILE_ROOT)
                        .setJsonExtension()
                        .build(),
                projectMetadata,
                ConflictPolicy.MERGE_KEYS_PREFER_NEW,
                this.className()
        );
    }
}
