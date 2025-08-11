package dev.jackraidenph.libraomni.gradle;

import dev.jackraidenph.libraomni.compilation.AnnotationProcessorConstants;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;
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
        JavaCompile javaCompile = (JavaCompile) tasks.getByName(COMPILE_JAVA);

        ExtensionContainer extensions = project.getExtensions();
        JavaPluginExtension javaExt = extensions.getByType(JavaPluginExtension.class);

        String compileTaskName = javaCompile.getName();
        Set<File> resourceDirs = javaExt.getSourceSets()
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
            javaCompile.getOptions().getCompilerArgs().add("-A" + AnnotationProcessorConstants.CONFIG_OPTION + '=' + extension.annotationProcessorConfiguration);
        });
    }

    private LibraOmniExtension getExtension(Project project) {
        return project.getExtensions().getByName("libraOmni") instanceof LibraOmniExtension libraOmniExtension ? libraOmniExtension : null;
    }

    public static class LibraOmniExtension {
        public Map<String, String> annotationProcessorConfiguration = Map.of();

        @Override
        public String toString() {
            return annotationProcessorConfiguration.toString();
        }
    }
}