package dev.jackraidenph.libraomni.experimental;

import dev.jackraidenph.libraomni.LibraOmni;
import dev.jackraidenph.libraomni.util.SafeReflectionUtil;
import dev.jackraidenph.libraomni.compilation.CompileConstants;
import dev.jackraidenph.libraomni.compilation.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class BlackMagicUtil {

    private static final String LOG4J_CONFIG_PROPERTY = "log4j2.configurationFile";

    private static String oldLog4JConfig = null;
    private static boolean compileHappened = false;

    public static boolean didCompileHappen() {
        return compileHappened;
    }

    public static void shutOffLog4j() {
        String oldLocation = System.getProperty(LOG4J_CONFIG_PROPERTY);

        URL log4jConfig = LibraOmni.class.getClassLoader().getResource("META-INF/libraomni-log4j2.xml");
        if (log4jConfig != null) {
            System.setProperty(LOG4J_CONFIG_PROPERTY, log4jConfig.toString());
        } else {
            System.err.println("LibraOmni failed to fetch log4j NO-OP confing. Log4j errors can be safely ignored, please report this");
        }

        oldLog4JConfig = oldLocation;
    }

    public static void restoreLog4j() {
        if (oldLog4JConfig != null) {
            System.setProperty(LOG4J_CONFIG_PROPERTY, oldLog4JConfig);
        } else {
            System.getProperties().remove(LOG4J_CONFIG_PROPERTY);
        }
    }

    public static Map<String, Class<?>> compileAndLoad(ProcessingContext context) {
        ProcessingEnvironment processingEnvironment = context.processingEnvironment();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        List<JavaFileObject> files = new ArrayList<>();

        StandardJavaFileManager std = compiler.getStandardFileManager(null, null, null);
        try {
            String classPathOption = processingEnvironment.getOptions().get(CompileConstants.CLASSPATH_OPTION);
            if (classPathOption == null) {
                throw new IllegalStateException();
            }
            String[] classpath = classPathOption.split(File.pathSeparator);

            std.setLocation(StandardLocation.CLASS_PATH, Arrays.stream(classpath).map(File::new).collect(Collectors.toList()));

            String sourcesOption = processingEnvironment.getOptions().get(CompileConstants.SOURCES_OPTION);
            if (sourcesOption == null) {
                throw new IllegalStateException();
            }
            String[] sources = sourcesOption.split(File.pathSeparator);

            //I have no idea if this is actually needed?
            std.setLocation(StandardLocation.SOURCE_PATH, Arrays.stream(sources).map(File::new).collect(Collectors.toList()));

            Arrays.stream(sources).map(FileSourceJavaFileObject::new).forEach(files::add);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DiagnosticCollector<JavaFileObject> diagnosticListener = new DiagnosticCollector<>();
        InMemoryFileManager fileManager = new InMemoryFileManager(std);

        List<String> options = List.of("-proc:none");
        boolean compiled = compiler.getTask(NoOpWriter.INSTANCE, fileManager, diagnosticListener, options, null, files).call();
        if (!compiled) {
            throw new ClassCompilationException(
                    "Failed to compile type elements %s".formatted(files),
                    diagnosticListener.getDiagnostics()
            );
        }

        Map<String, Class<?>> classes = new HashMap<>();
        for (Entry<String, byte[]> e : fileManager.getAll().entrySet()) {
            String qualifiedName = e.getKey();
            byte[] compiledBytes = e.getValue();
            if (compiledBytes == null) {
                throw new IllegalStateException("Couldn't fetch bytes from the file manage after compilation");
            }
            Class<?> clazz = defineClass(LibraOmni.class.getClassLoader(), qualifiedName, compiledBytes);
            classes.put(qualifiedName, clazz);
        }

        compileHappened = true;
        return classes;
    }

    public static Class<?> defineClass(ClassLoader loader, String name, byte[] bytes) {
        Class<?> clazz;
        //Already defined
        if ((clazz = SafeReflectionUtil.forName(name, false, loader)) != null) {
            return clazz;
        }

        try {
            Method define = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            define.setAccessible(true);
            return (Class<?>) define.invoke(loader, name, bytes, 0, bytes.length);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No 'defineClass' method");
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class ClassCompilationException extends RuntimeException {
        private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

        public ClassCompilationException(String message, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
            super(message + "\n" + diagnostics.toString().replaceAll("[\\[\\]]", "").replace(", ", ",\n\n"));
            this.diagnostics = diagnostics;
        }


        public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
            return diagnostics;
        }
    }

    public static final class NoOpWriter extends Writer {
        public static final NoOpWriter INSTANCE = new NoOpWriter();

        private NoOpWriter() {
        }

        @Override
        public void write(@NotNull char[] cbuf, int off, int len) {
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
