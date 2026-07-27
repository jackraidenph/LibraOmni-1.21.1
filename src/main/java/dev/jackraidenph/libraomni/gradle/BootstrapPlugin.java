package dev.jackraidenph.libraomni.gradle;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.compilation.CompileConstants;
import dev.jackraidenph.libraomni.compilation.util.ResourceManager;
import net.neoforged.moddevgradle.dsl.ModDevExtension;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class BootstrapPlugin implements Plugin<Project> {

    private static final String COMPILE_JAVA_TASK = "compileJava";
    private static final String PROCESS_RESOURCES_TASK = "processResources";
    private static final String LIBRAOMNI_SOURCESET = LibraOmni.MOD_ID;
    private static final List<String> REQUIRED_MODULES = List.of(
            "jdk.compiler/com.sun.tools.javac.code",
            "jdk.compiler/com.sun.tools.javac.util",
            "java.base/java.lang"
    );

    @Override
    public void apply(Project project) {
        ExtensionContainer extensions = project.getExtensions();

        extensions.create(LibraOmniExtension.NAME, LibraOmniExtension.class);

        JavaCompile javaCompile = taskByNameChecked(project, COMPILE_JAVA_TASK);
        javaCompile.getOptions().setFork(true);

        CompileOptions compileOptions = javaCompile.getOptions();
        addExportsToAllUnnamed(compileOptions.getCompilerArgs(), REQUIRED_MODULES);
        List<String> jvmArgs = compileOptions.getForkOptions().getJvmArgs();
        if (jvmArgs != null) {
            addExportsToAllUnnamed(jvmArgs, REQUIRED_MODULES);
        }

        JavaPluginExtension javaExt = extensionByTypeChecked(project, JavaPluginExtension.class);
        SourceSetContainer sourceSets = javaExt.getSourceSets();

        Set<File> resourceDirs = sourceSets
                .stream()
                .filter(s -> s.getCompileJavaTaskName().equals(COMPILE_JAVA_TASK))
                .flatMap(s -> s.getResources().getSrcDirs().stream())
                .collect(Collectors.toSet());
        String resourceDirsArg = resourceDirs.toString().replaceAll("[\\[\\]\\s]", "");
        addAPCompilerArg(javaCompile, CompileConstants.RESOURCE_LOCATIONS_OPTION, resourceDirsArg);

        excludeGeneratedResources(project);

        NamedDomainObjectProvider<SourceSet> libraOmniSourceSetProvider = javaExt.getSourceSets().register(LIBRAOMNI_SOURCESET);
        SourceSet libraOmniSourceSet = libraOmniSourceSetProvider.get();
        ConfigurationContainer configurations = project.getConfigurations();
        Configuration apConfig = configurations.getByName("annotationProcessor");
        Configuration sourceSetCompileClassPath = configurations.getByName(libraOmniSourceSet.getCompileClasspathConfigurationName());
        apConfig.extendsFrom(sourceSetCompileClassPath);
        apConfig.exclude(Map.of("group", "net.fabricmc"));

        project.afterEvaluate(BootstrapPlugin::doAfterEvaluate);
    }

    private static void excludeGeneratedResources(Project project) {
        JavaCompile javaCompile = taskByNameChecked(project, COMPILE_JAVA_TASK);
        ProcessResources processResources = taskByNameChecked(project, PROCESS_RESOURCES_TASK);

        processResources.dependsOn(javaCompile);

        processResources.doFirst("excludeGenerated", t -> {
            try {
                byte[] data = Files.readAllBytes(ResourceManager.GRADLE_EXCLUSION_LIST_FILE.toPath());
                String str = new String(data, StandardCharsets.UTF_8);
                String[] resources = str.split(";");
                ((ProcessResources) t).exclude(resources);
            } catch (IOException e) {
                t.getLogger().warn("Failed to read exclusion list from file [{}]", ResourceManager.GRADLE_EXCLUSION_LIST_FILE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void doAfterEvaluate(Project project) {
        JavaPluginExtension javaExt = extensionByTypeChecked(project, JavaPluginExtension.class);
        LibraOmniExtension libraOmniExt = extensionByTypeChecked(project, LibraOmniExtension.class);
        JavaCompile javaCompile = taskByNameChecked(project, COMPILE_JAVA_TASK);

        SourceSetContainer sourceSets = javaExt.getSourceSets();
        SourceSet main = sourceSets.getByName("main");

        addAPCompilerArg(javaCompile, CompileConstants.CONFIG_OPTION, libraOmniExt.getConflictPolicies().get());
        addAPCompilerArg(javaCompile, CompileConstants.ENABLE_BLACK_MAGIC_OPTION, libraOmniExt.getBlackMagicEnabled().get());
        addAPCompilerArg(javaCompile, CompileConstants.DISABLE_CACHE_OPTION, libraOmniExt.getCacheDisabled().get());
        addAPCompilerArg(javaCompile, CompileConstants.CLASSPATH_OPTION, main.getCompileClasspath().getAsPath());
        addAPCompilerArg(javaCompile, CompileConstants.SOURCES_OPTION, main.getJava().getAsPath());

        ModDevExtension modDevExt = extensionByTypeChecked(project, ModDevExtension.class);

        SourceSet libraOmniSourceSet = sourceSets.getByName(LIBRAOMNI_SOURCESET);
        modDevExt.addModdingDependenciesTo(libraOmniSourceSet);
    }

    private static <T> @Nonnull T extensionByTypeChecked(Project project, Class<T> clazz) {
        T ext = project.getExtensions().findByType(clazz);
        if (ext == null) {
            throw new IllegalStateException("[%s] is not found, probably, LibraOmni plugin is defined too early".formatted(clazz.getSimpleName()));
        }
        return ext;
    }

    private static <T> @Nonnull T taskByNameChecked(Project project, String name) {
        try {
            //noinspection unchecked
            T task = (T) project.getTasks().findByName(name);
            if (task == null) {
                throw new IllegalStateException("[%s] is not found, probably, LibraOmni plugin is defined too early".formatted(name));
            }
            return task;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Task with name [%s] has inappropriate type", e);
        }
    }

    private static void addAPCompilerArg(JavaCompile javaCompile, String key, Object value) {
        javaCompile.getOptions().getCompilerArgs().add("-A" + key + '=' + value);
    }

    private static void addExportsToAllUnnamed(List<String> addTo, Collection<String> modules) {
        List<String> options = new ArrayList<>();
        for (String m : modules) {
            options.add("--add-exports=" + m + "=ALL-UNNAMED");
        }
        addTo.addAll(options);
    }
}