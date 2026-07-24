package dev.jackraidenph.libraomni.compilation.task.cache;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.compilation.task.CompilationTask;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import dev.jackraidenph.libraomni.util.AnnotationMirrorUtil;
import dev.jackraidenph.libraomni.util.ElementUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.nio.file.Path;
import java.util.*;

public class ProcessingCache {

    private static final Path TMP_FOLDER = Path.of(System.getProperty("java.io.tmpdir"), '.' + LibraOmni.MOD_ID);

    private final Map<Integer, RoundCache> roundCaches = new HashMap<>();

    public @Nonnull RoundCache getOrCreateRoundCache(int round) {
        RoundCache roundCache = getRoundCache(round);
        if (roundCache == null) {
            roundCache = new RoundCache();
            roundCaches.put(round, roundCache);
        }
        return roundCache;
    }

    public @Nullable RoundCache getRoundCache(int round) {
        return roundCaches.get(round);
    }

    private void cacheElement(RoundCache cache, CompilationTask task, Element element) {
        for (AnnotationMirror m : element.getAnnotationMirrors()) {
            if (AnnotationMirrorUtil.taskSupportsAnnotationMirror(task, m)) {
                cache.add(task, element, m);
            }
        }
    }

    public RoundCache cacheRoundElements(ProcessingContext context, List<CompilationTask> tasksToCache) {
        RoundEnvironment roundEnvironment = context.roundEnvironment();
        RoundCache cache = getOrCreateRoundCache(context.round());

        for (Element e : ElementUtil.getAllElements(roundEnvironment)) {
            for (CompilationTask t : tasksToCache) {
                cacheElement(cache, t, e);
            }
        }

        return cache;
    }


    public static Path getTempFolder() {
        return TMP_FOLDER;
    }
}
