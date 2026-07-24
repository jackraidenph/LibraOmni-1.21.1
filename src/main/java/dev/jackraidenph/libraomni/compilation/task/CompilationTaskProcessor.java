package dev.jackraidenph.libraomni.compilation.task;

import com.google.common.base.Stopwatch;
import dev.jackraidenph.libraomni.annotation.meta.ModPackage;
import dev.jackraidenph.libraomni.compilation.task.cache.ProcessingCache;
import dev.jackraidenph.libraomni.compilation.task.cache.RoundCache;
import dev.jackraidenph.libraomni.compilation.task.cache.TaskCache;
import dev.jackraidenph.libraomni.util.ObjectOriginGetter;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.*;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import dev.jackraidenph.libraomni.experimental.BlackMagicBootstrap;
import dev.jackraidenph.libraomni.experimental.BlackMagicUtil;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.io.*;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class CompilationTaskProcessor extends AbstractProcessor {

    private static ModIdGetter MOD_ID_GETTER = null;

    private final List<CompilationTask> tasks = new ArrayList<>();
    private final ModIdGetter modIdGetter = new ModIdGetter();
    private final AnnotationProcessorConfig config = new AnnotationProcessorConfig();
    private final ProcessingCache cache = new ProcessingCache();

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private final Stopwatch stopwatchFull = Stopwatch.createUnstarted();

    private int round = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        BlackMagicUtil.shutOffLog4j();
        config.init(processingEnv);
        CompilationTasksInit.init(this);
    }

    void registerTask(CompilationTask task) {
        this.tasks.add(task);
        if (processingEnv != null) {
            processingEnv.getMessager().printNote("Registered [" + task.getClass().getSimpleName() + "] for processing");
        }
    }

    private void discoverMods(RoundEnvironment roundEnvironment) {
        TypeElement modAnnotation = this.processingEnv.getElementUtils().getTypeElement(AnnotationProcessorConstants.NF_MOD_ANNOTATION_CLASS_NAME);
        TypeElement modRootAnnotation = this.processingEnv.getElementUtils().getTypeElement(ModPackage.class.getName());
        this.modIdGetter.discoverMods(modAnnotation, "value", roundEnvironment, this.processingEnv.getMessager());
        this.modIdGetter.discoverMods(modRootAnnotation, "value", roundEnvironment, this.processingEnv.getMessager());
        SafeReflectionUtil.invoke(ModIdGetter.class, modIdGetter, "setInitialized", true);
        MOD_ID_GETTER = modIdGetter;
    }

    public static ObjectOriginGetter getModIdGetter() {
        return MOD_ID_GETTER;
    }

    private long elapsedTotal = 0;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        stopwatchFull.reset();
        stopwatchFull.start();

        discoverMods(roundEnvironment);

        RoundEnvironment proxyEnvironment = ProxyFactory.makeRuntimeEnvironmentProxy(roundEnvironment, processingEnv);

        ProcessingContext context = new ProcessingContext(modIdGetter, null, config, proxyEnvironment, processingEnv, round);

        ResourceManager resourceManager = new ResourceManager(context, cache);
        context.setResourceManager(resourceManager);

        Messager messager = this.processingEnv.getMessager();

        if (round == 0) {
            stopwatch.reset();
            stopwatch.start();
            BlackMagicUtil.compileAndLoad(context);
            long elapsedCompile = stopwatch.elapsed().getNano();
            messager.printNote("Compiling took %.4f seconds".formatted(elapsedCompile / 1_000_000_000.));
        }

        boolean processignOver = roundEnvironment.processingOver();

        if (!processignOver) {
            messager.printNote("Processing round " + round);
        } else {
            messager.printNote("Finishing");
        }

        stopwatch.reset();
        stopwatch.start();
        RoundCache oldCache = RoundCache.readFromTempDir(round);
        RoundCache newCache = cache.cacheRoundElements(context, tasks);
        long elapsedCache = stopwatch.elapsed().getNano();
        messager.printNote("Reading and calculating cache took %.4f seconds".formatted(elapsedCache / 1_000_000_000.));

        for (CompilationTask compilationTask : this.tasks) {

            if (isTaskUpToDate(compilationTask, oldCache, newCache, context)) {
                continue;
            }

            dirtyTasks.add(compilationTask.className());

            String simpleTaskName = compilationTask.getClass().getSimpleName();

            String op = processignOver ? "Finishing" : "Processing";

            if (!tryEnableBlackMagic(compilationTask, context, op)) {
                continue;
            }

            stopwatch.reset();
            stopwatch.start();

            boolean executed;
            try {
                executed = compilationTask.processStage(context);
            } catch (Exception e) {
                RoundCache.removeFromTempDir(round);
                printStackTrace(e);
                throw new RuntimeException("Exception thrown while processing [%s]".formatted(compilationTask.getClass().getSimpleName()), e);
            }

            long elapsed = stopwatch.elapsed().getNano();
            if (executed) {
                messager.printNote("%s [%s] took %.4f seconds".formatted(op, simpleTaskName, elapsed / 1_000_000_000.));
            }
        }

        newCache.setBuilt(true);
        newCache.saveToTempDir(round);
        elapsedTotal += stopwatchFull.elapsed().getNano();

        if (processignOver) {
            //Restore original Log4J config
            BlackMagicUtil.restoreLog4j();
            messager.printNote("LibraOmni processor finished, took %.4f seconds".formatted(elapsedTotal / 1_000_000_000.));
        }

        this.round++;
        return false;
    }

    private boolean tryEnableBlackMagic(CompilationTask task, ProcessingContext context, String op) {
        Messager messager = context.processingEnvironment().getMessager();

        if (task.requiresBlackMagicEnabled() && !isBlackMagicAllowed(context)) {
            messager.printNote("""
                    %s [%s] denied due to the task requiring black magic, \
                    but it's disabled. If you want it to work, \
                    enable it in the Gradle plugin.
                    """.formatted(op, task.getClass().getSimpleName()));
            return false;
        }

        if (!BlackMagicBootstrap.isBlackMagicActive()) {
            stopwatch.reset();
            stopwatch.start();
            BlackMagicBootstrap.bootstrapBlackMagic(modIdGetter, context);
            long elapsedBootstrap = stopwatch.elapsed().getNano();
            messager.printNote("Bootstrapping Black Magic took %.4f seconds".formatted(elapsedBootstrap / 1_000_000_000.));
        }

        return true;
    }

    private final Set<String> dirtyTasks = new HashSet<>();

    private boolean isTaskUpToDate(CompilationTask task, RoundCache oldCache, RoundCache newCache, ProcessingContext processingContext) {
        Messager messager = processingContext.processingEnvironment().getMessager();
        ResourceManager resourceManager = processingContext.resourceManager();
        boolean processingOver = processingContext.roundEnvironment().processingOver();

        if (oldCache == null) {
            return false;
        }

        TaskCache oldTaskCache = oldCache.getTaskCache(task.className());
        if (oldTaskCache == null) {
            return false;
        }

        TaskCache newTaskCahe = newCache.getOrCreateTaskCache(task.className());
        boolean processingOverAndUpToDate = processingOver && !dirtyTasks.contains(task.className());

        //No need to compare type cache if processing over - no elements are retained at this point
        if (processingOverAndUpToDate || (!processingOver && newTaskCahe.elementsUpToDate(oldTaskCache))) {
            oldTaskCache.outputResourceCache(resourceManager);
            newTaskCahe.copyOutputs(oldTaskCache);

            String simpleTaskName = task.getClass().getSimpleName();
            messager.printNote("Task [%s] is UP-TO-DATE".formatted(simpleTaskName));
            return true;
        }

        return false;
    }

    private static boolean isBlackMagicAllowed(ProcessingContext processingContext) {
        ProcessingEnvironment environment = processingContext.processingEnvironment();
        Map<String, String> options = environment.getOptions();
        String blackMagicAllowedStr = options.get(AnnotationProcessorConstants.ENABLE_BLACK_MAGIC_OPTION);
        if (blackMagicAllowedStr == null) {
            return false;
        }

        return Boolean.parseBoolean(blackMagicAllowedStr);
    }

    private void printStackTrace(Throwable throwable) {
        Messager messager = processingEnv.getMessager();
        try (
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter)
        ) {
            throwable.printStackTrace(printWriter);
            messager.printNote(stringWriter.getBuffer());
        } catch (IOException ioException) {
            throw new IllegalStateException(ioException);
        }
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(
                AnnotationProcessorConstants.RESOURCE_LOCATIONS_OPTION,
                AnnotationProcessorConstants.SOURCES_OPTION,
                AnnotationProcessorConstants.CLASSPATH_OPTION,
                AnnotationProcessorConstants.CONFIG_OPTION,
                AnnotationProcessorConstants.ENABLE_BLACK_MAGIC_OPTION
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                AnnotationProcessorConstants.NF_MOD_ANNOTATION_CLASS_NAME,
                ModPackage.class.getName()
        );
    }
}