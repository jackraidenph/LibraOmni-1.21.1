package dev.jackraidenph.libraomni.experimental;

import javax.annotation.Nullable;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    private final Map<String, ByteConsumingJavaFileObject> javaCompilationOutputBytes = new HashMap<>();

    protected InMemoryFileManager(StandardJavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteConsumingJavaFileObject fileObject = new ByteConsumingJavaFileObject(className, out);
        javaCompilationOutputBytes.put(className, fileObject);
        return fileObject;
    }

    @Nullable
    public byte[] getBytes(String className) {
        ByteConsumingJavaFileObject file = javaCompilationOutputBytes.get(className);
        if (file != null) {
            return file.read();
        }

        return null;
    }

    public Map<String, byte[]> getAll() {
        Map<String, byte[]> res = new HashMap<>();
        for (Entry<String, ByteConsumingJavaFileObject> e : javaCompilationOutputBytes.entrySet()) {
            res.put(e.getKey(), e.getValue().read());
        }

        return res;
    }

}
