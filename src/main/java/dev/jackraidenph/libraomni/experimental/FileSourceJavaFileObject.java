package dev.jackraidenph.libraomni.experimental;


import javax.tools.SimpleJavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.stream.Stream;

public class FileSourceJavaFileObject extends SimpleJavaFileObject {

    private final File file;

    public FileSourceJavaFileObject(String path) {
        super(URI.create("file:///" + path.replace("\\", "/")), Kind.SOURCE);
        file = new File(toUri());
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (Stream<String> s = Files.lines(file.toPath())) {
            s.forEach(l -> builder.append(l).append('\n'));
        }
        return builder.toString();
    }
}
