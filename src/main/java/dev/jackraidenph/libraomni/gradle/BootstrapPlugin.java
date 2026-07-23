package dev.jackraidenph.libraomni.gradle;

import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import net.neoforged.moddevgradle.dsl.ModDevExtension;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BootstrapPlugin implements Plugin<Project> {

    private static final String COMPILE_JAVA = "compileJava";
    private static final String PROCESS_RESOURCES = "processResources";

    private final FileSystemOperations fs;

    @Inject
    public BootstrapPlugin(FileSystemOperations fileSystemOperations) {
        this.fs = fileSystemOperations;
    }

    @Override
    public void apply(Project project) {

        /// Gather resource directories to AP argument
        TaskContainer tasks = project.getTasks();
        ExtensionContainer extensions = project.getExtensions();

        JavaPluginExtension javaExt;
        try {
            javaExt = extensions.getByType(JavaPluginExtension.class);
        } catch (UnknownDomainObjectException e) {
            throw new IllegalStateException("Java extension is not found, probably, LibraOmni plugin is defined before Java plugin, change this");
        }


        JavaCompile javaCompile = (JavaCompile) tasks.getByName(COMPILE_JAVA);

        javaCompile.getOptions().getCompilerArgs().addAll(List.of(
                "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
        ));

        javaCompile.getOptions().setFork(true);
        Optional.ofNullable(javaCompile.getOptions().getForkOptions().getJvmArgs()).ifPresent(args -> args.addAll(List.of(
                "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED"
        )));

        String compileTaskName = javaCompile.getName();

        SourceSetContainer sourceSets = javaExt.getSourceSets();

        Set<File> resourceDirs = sourceSets
                .stream()
                .filter(s -> s.getCompileJavaTaskName().equals(compileTaskName))
                .flatMap(s -> s.getResources().getSrcDirs().stream())
                .collect(Collectors.toSet());

        String resourceDirsArg = resourceDirs.toString().replaceAll("[\\[\\]\\s]", "");
        javaCompile.getOptions().getCompilerArgs().add("-A" + AnnotationProcessorConstants.RESOURCE_LOCATIONS_OPTION + '=' + resourceDirsArg);

        /// Exclude merged resources from processResources and copy over after compileJava

        ProcessResources processResources = (ProcessResources) tasks.getByName(PROCESS_RESOURCES);
        processResources.exclude(AnnotationProcessorConstants.PROCESSED_RESOURCES);

        javaCompile.doLast("copyResources", task -> fs.copy(copy -> {
                    File destination = ((JavaCompile) task).getDestinationDirectory().get().getAsFile();
                    copy
                            .from(resourceDirs)
                            .into(destination)
                            .eachFile(file -> {
                                if (file.getRelativePath().getFile(destination).exists()) {
                                    file.exclude();
                                }
                            })
                            .filesNotMatching(AnnotationProcessorConstants.PROCESSED_RESOURCES, FileCopyDetails::exclude);
                    copy.setIncludeEmptyDirs(false);
                })
        );

        /// Add configuration extension

        extensions.create("libraOmni", LibraOmniExtension.class);

        project.afterEvaluate(proj -> {
            LibraOmniExtension extension = getExtension(proj);
            if (extension == null) {
                return;
            }

            SourceSet main = sourceSets.getByName("main");

            javaCompile.getOptions().getCompilerArgs().add("-A" + AnnotationProcessorConstants.CONFIG_OPTION + '=' + extension.annotationProcessorConfiguration);
            javaCompile.getOptions().getCompilerArgs().add("-A" + AnnotationProcessorConstants.ENABLE_BLACK_MAGIC_OPTION + '=' + extension.blackMagicEnabled);
            javaCompile.getOptions().getCompilerArgs().add("-A" + AnnotationProcessorConstants.CLASSPATH_OPTION + '=' + main.getCompileClasspath().getAsPath());
            javaCompile.getOptions().getCompilerArgs().add("-A" + AnnotationProcessorConstants.SOURCES_OPTION + '=' + main.getJava().getAsPath());
        });

        NamedDomainObjectProvider<SourceSet> libraOmniSourceSetProvider = javaExt.getSourceSets().register("libraOmniAnnotationProcessor");
        SourceSet libraOmniSourceSet = libraOmniSourceSetProvider.get();

        try {
            if (extensions.getByName("neoForge") instanceof ModDevExtension modDevExtension) {
                project.afterEvaluate(p -> modDevExtension.addModdingDependenciesTo(libraOmniSourceSet));
            }
        } catch (UnknownDomainObjectException e) {
            throw new IllegalStateException("NeoForge extension is not found, probably, LibraOmni plugin is defined before NeoForge's ModDev, change this");
        }

        ConfigurationContainer configurations = project.getConfigurations();

        Configuration apConfig = configurations.getByName("annotationProcessor");
        Configuration sourceSetCompileClassPath = configurations.getByName(libraOmniSourceSet.getCompileClasspathConfigurationName());

        apConfig.extendsFrom(sourceSetCompileClassPath);
        apConfig.exclude(Map.of("group", "net.fabricmc"));
    }

    private LibraOmniExtension getExtension(Project project) {
        return project.getExtensions().getByName("libraOmni") instanceof LibraOmniExtension libraOmniExtension ? libraOmniExtension : null;
    }

    public static class LibraOmniExtension {
        public Map<String, String> annotationProcessorConfiguration = Map.of();
        public boolean blackMagicEnabled = false;
    }
}