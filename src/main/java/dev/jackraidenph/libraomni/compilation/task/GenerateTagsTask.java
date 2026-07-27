package dev.jackraidenph.libraomni.compilation.task;

import dev.jackraidenph.libraomni.annotation.datagen.InTags;
import dev.jackraidenph.libraomni.compilation.util.InMemoryResource;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.compilation.util.ResourceIdentifier;
import dev.jackraidenph.libraomni.compilation.util.ResourceManager;
import dev.jackraidenph.libraomni.util.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.util.StringUtil;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GenerateTagsTask extends SequentialCompilationTask {

    private final Map<String, List<String>> tagToEntries = new HashMap<>();

    @Override
    void processElement(String modId, String elementId, Element element, ProcessingContext processingContext) {
        InTags inTags = element.getAnnotation(InTags.class);
        if(inTags == null) {
            throw new IllegalStateException();
        }

        for (String tag : inTags.value()) {
            tagToEntries.computeIfAbsent(tag, k -> new ArrayList<>()).add(StringUtil.makeNamespacedId(modId, elementId));
        }
    }

    @Override
    public void finish(ProcessingContext processingContext) {
        ResourceManager resourceManager = processingContext.resourceManager();
        for (Entry<String, List<String>> entry : tagToEntries.entrySet()) {
            String tag = entry.getKey();
            List<String> tagEntries = entry.getValue();
            resourceManager.saveAndCache(makeFile(tag, tagEntries), this.className());
        }
    }

    private static InMemoryResource makeFile(String tag, List<String> entries) {
        String namespace = "minecraft";
        String tagName = tag;
        if (tag.indexOf(':') > 0) {
            String[] parts = tag.split(":");
            namespace = parts[0];
            tagName = parts[1];
        }

        String dir = "tags";
        int dirSeparatorIndex = tagName.lastIndexOf('/');
        if (dirSeparatorIndex > 0) {
            dir += "/" + tagName.substring(0, dirSeparatorIndex);
            tagName = tagName.substring(dirSeparatorIndex + 1);
        }

        return new InMemoryResource(
                ResourceIdentifier.data(namespace, dir, tagName),
                Map.of("values", entries)
        );
    }

    @Override
    public boolean isMirrorSupported(AnnotationMirror mirror) {
        return AnnotationMirrorUtil.compareWithClass(mirror, InTags.class);
    }
}
