package dev.jackraidenph.libraomni.compilation.task;

import com.google.common.base.Stopwatch;
import dev.jackraidenph.libraomni.annotation.meta.ModPackage;
import dev.jackraidenph.libraomni.compilation.util.*;
import dev.jackraidenph.libraomni.data.proxy.ProxyFactory;
import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import dev.jackraidenph.libraomni.experimental.BlackMagicUtil;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.io.*;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class CompilationTaskProcessor extends AbstractProcessor {

    private final List<CompilationTask> tasks = new ArrayList<>();
    private final ModIdGetter modIdGetter = new ModIdGetter();
    private final AnnotationProcessorConfig config = new AnnotationProcessorConfig();
    private ResourceManager resourceManager;

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();

    private int round = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        BlackMagicUtil.shutOffLog4j();
        config.init(processingEnv);
        this.resourceManager = new ResourceManager(config, processingEnv);
        CompilationTasksInit.init(this);
    }

    void registerTask(CompilationTask task) {
        this.tasks.add(task);
        if (processingEnv != null) {
            processingEnv.getMessager().printNote("Registered [" + task.getClass().getSimpleName() + "] for processing");
        }
    }

    private void findMods(RoundEnvironment roundEnvironment) {
        TypeElement modAnnotation = this.processingEnv.getElementUtils().getTypeElement(AnnotationProcessorConstants.NF_MOD_ANNOTATION_CLASS_NAME);
        TypeElement modRootAnnotation = this.processingEnv.getElementUtils().getTypeElement(ModPackage.class.getName());
        this.modIdGetter.findMods(modAnnotation, "value", roundEnvironment, this.processingEnv.getMessager());
        this.modIdGetter.findMods(modRootAnnotation, "value", roundEnvironment, this.processingEnv.getMessager());
    }

    private long elapsedTotal = 0;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        findMods(roundEnvironment);

        RoundEnvironment proxyEnvironment = ProxyFactory.proxifyRuntimeEnvironment(roundEnvironment, processingEnv, modIdGetter);
        ProcessingContext context = new ProcessingContext(modIdGetter, resourceManager, config, proxyEnvironment, processingEnv);

        Messager messager = this.processingEnv.getMessager();
        boolean finishing = roundEnvironment.processingOver();

        if (!finishing) {
            messager.printNote("Processing round " + round);
        }

        for (CompilationTask compilationTask : this.tasks) {
            stopwatch.start();

            String taskName = compilationTask.getClass().getSimpleName();

            boolean wasOverriden;
            try {
                wasOverriden = compilationTask.processStage(context);
            } catch (Exception e) {
                printStackTrace(e);
                throw new RuntimeException("Exception thrown while processing [%s]".formatted(compilationTask.getClass().getSimpleName()), e);
            }

            long elapsed = stopwatch.elapsed().getNano();
            elapsedTotal += elapsed;
            stopwatch.reset();

            if (wasOverriden) {
                String op = finishing ? "Finishing" : "Processing";
                messager.printNote(op + "[%s] took %.4f seconds".formatted(taskName, elapsed / 1_000_000_000.));
            }
        }

        if (finishing) {
            //Restore original Log4J config
            BlackMagicUtil.restoreLog4j();
            messager.printNote("LibraOmni processor finished, took %.4f seconds".formatted(elapsedTotal / 1_000_000_000.));
        }

        this.round++;
        return false;
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
                AnnotationProcessorConstants.CONFIG_OPTION
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