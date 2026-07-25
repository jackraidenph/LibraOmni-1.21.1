package dev.jackraidenph.libraomni.compilation.task;

import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import dev.jackraidenph.libraomni.annotation.meta.ModPackage;
import dev.jackraidenph.libraomni.compilation.task.cache.ProcessingCache;
import dev.jackraidenph.libraomni.compilation.task.cache.RoundCache;
import dev.jackraidenph.libraomni.compilation.task.cache.TaskCache;
import dev.jackraidenph.libraomni.util.ObjectOriginGetter;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.util.*;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import dev.jackraidenph.libraomni.compilation.CompileConstants;
import dev.jackraidenph.libraomni.experimental.BlackMagicBootstrap;
import dev.jackraidenph.libraomni.experimental.BlackMagicUtil;

import javax.annotation.Nullable;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class CompilationTaskProcessor extends AbstractProcessor {

    private static ModIdGetter MOD_ID_GETTER = null;

    private final List<CompilationTask> tasks = new ArrayList<>();
    private final ModIdGetter modIdGetter = new ModIdGetter();
    private final ResourceConfig config = new ResourceConfig();
    private final ProcessingCache cache = new ProcessingCache();
    private final Set<String> dirtyTasks = new HashSet<>();

    private final Stopwatch processingStopwatch = Stopwatch.createUnstarted();

    private int round = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        BlackMagicUtil.shutOffLog4j();
        config.init(processingEnv);
        CompilationTasksInit.init(this);
        ResourceManager.clearGradleExclusionListFile();
    }

    void registerTask(CompilationTask task) {
        this.tasks.add(task);
        if (processingEnv != null) {
            processingEnv.getMessager().printNote("Registered [" + task.getClass().getSimpleName() + "] for processing");
        }
    }

    private void discoverMods(RoundEnvironment roundEnvironment) {
        TypeElement modAnnotation = this.processingEnv.getElementUtils().getTypeElement(CompileConstants.NF_MOD_ANNOTATION_CLASS_NAME);
        TypeElement modRootAnnotation = this.processingEnv.getElementUtils().getTypeElement(ModPackage.class.getName());
        this.modIdGetter.discoverMods(modAnnotation, "value", roundEnvironment, this.processingEnv.getMessager());
        this.modIdGetter.discoverMods(modRootAnnotation, "value", roundEnvironment, this.processingEnv.getMessager());
        SafeReflectionUtil.invoke(ModIdGetter.class, modIdGetter, "setInitialized", true);
        MOD_ID_GETTER = modIdGetter;
    }

    public static ObjectOriginGetter getModIdGetter() {
        return MOD_ID_GETTER;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (!processingStopwatch.isRunning()) {
            processingStopwatch.start();
        }

        discoverMods(roundEnvironment);

        RoundEnvironment proxyEnvironment = ProxyFactory.makeRuntimeEnvironmentProxy(roundEnvironment, processingEnv);

        ProcessingContext context = new ProcessingContext(modIdGetter, null, config, proxyEnvironment, processingEnv, round);

        ResourceManager resourceManager = new ResourceManager(context, cache);
        context.setResourceManager(resourceManager);

        Messager messager = this.processingEnv.getMessager();

        boolean processignOver = roundEnvironment.processingOver();

        if (!processignOver) {
            messager.printNote("Processing round " + round);
        } else {
            messager.printNote("Finishing");
        }

        RoundCache oldCache = RoundCache.readFromTempDir(round);
        RoundCache newCache = cache.cacheRoundElements(context, tasks);

        for (CompilationTask compilationTask : this.tasks) {

            if (isTaskUpToDate(compilationTask, oldCache, newCache, context)) {
                continue;
            }

            dirtyTasks.add(compilationTask.className());

            String simpleTaskName = compilationTask.getClass().getSimpleName();
            String op = processignOver ? "Finishing" : "Processing";

            timed(() -> tryExecuteTask(compilationTask, context), "%s [%s]".formatted(op, simpleTaskName), messager);
        }

        newCache.setBuilt(true);
        newCache.saveToTempDir(round);

        if (processignOver) {
            BlackMagicUtil.restoreLog4j();
            double elapsedSeconds = processingStopwatch.elapsed().getNano() / 1_000_000_000.;
            messager.printNote("LibraOmni processor finished, took %.4f seconds".formatted(elapsedSeconds));
            processingStopwatch.stop();
        }

        this.round++;
        return false;
    }

    private boolean tryExecuteTask(CompilationTask task, ProcessingContext context) {
        try {
            if (!task.shouldExecute(context)) {
                return false;
            }

            if (!tryEnableBlackMagic(task, context) || !tryCompileClasspath(task, context)) {
                return false;
            }

            task.processStage(context);
            return true;
        } catch (Exception e) {
            RoundCache.removeFromTempDir(round);
            printStackTrace(e);
            throw new RuntimeException("Exception thrown while processing [%s]".formatted(task.getClass().getSimpleName()), e);
        }
    }

    private boolean tryCompileClasspath(CompilationTask task, ProcessingContext context) {
        Messager messager = context.processingEnvironment().getMessager();

        if (task.requiresCompiledClasspath() && !isBlackMagicAllowed(context)) {
            messager.printNote("""
                    Execution of [%s] denied due to the task requiring compiled classpath, \
                    but black magic is disabled. If you want it to work, \
                    enable it in the Gradle plugin.
                    """.formatted(task.getClass().getSimpleName()));
            return false;
        }

        if (task.requiresCompiledClasspath() && !BlackMagicUtil.didCompileHappen()) {
            timed(() -> BlackMagicUtil.compileAndLoad(context), "Compiling Classpath", messager);
        }

        return true;
    }

    private boolean tryEnableBlackMagic(CompilationTask task, ProcessingContext context) {
        Messager messager = context.processingEnvironment().getMessager();

        if (task.requiresBlackMagicEnabled() && !isBlackMagicAllowed(context)) {
            messager.printNote("""
                    Execution of [%s] denied due to the task requiring black magic, \
                    but it's disabled. If you want it to work, \
                    enable it in the Gradle plugin.
                    """.formatted(task.getClass().getSimpleName()));
            return false;
        }

        if (task.requiresBlackMagicEnabled() && !BlackMagicBootstrap.isBlackMagicActive()) {
            timed(() -> BlackMagicBootstrap.bootstrapBlackMagic(modIdGetter, context), "Bootstrapping Black Magic", messager);
        }

        return true;
    }

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
        String blackMagicAllowedStr = options.get(CompileConstants.ENABLE_BLACK_MAGIC_OPTION);
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

    private static void timed(Runnable action, String actionComment, @Nullable Messager messager) {
        timed(() -> {
            action.run();
            return true;
        }, actionComment, messager);
    }

    private static void timed(Supplier<Boolean> action, String actionComment, @Nullable Messager messager) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean result = action.get();
        long nanos = stopwatch.elapsed(TimeUnit.NANOSECONDS);
        double seconds = nanos / 1_000_000_000.;
        if (result && messager != null) {
            messager.printNote("%s | %.4f seconds".formatted(actionComment, seconds));
        }
        stopwatch.stop();
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(
                CompileConstants.RESOURCE_LOCATIONS_OPTION,
                CompileConstants.SOURCES_OPTION,
                CompileConstants.CLASSPATH_OPTION,
                CompileConstants.CONFIG_OPTION,
                CompileConstants.ENABLE_BLACK_MAGIC_OPTION
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                CompileConstants.NF_MOD_ANNOTATION_CLASS_NAME,
                ModPackage.class.getName()
        );
    }
}