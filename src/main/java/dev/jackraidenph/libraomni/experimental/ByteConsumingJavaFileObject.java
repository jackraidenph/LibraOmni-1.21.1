package dev.jackraidenph.libraomni.experimental;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

public class ByteConsumingJavaFileObject extends SimpleJavaFileObject {
    private final ByteArrayOutputStream outputStream;

    protected ByteConsumingJavaFileObject(String name, ByteArrayOutputStream outputStream) {
        super(URI.create("byte:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        this.outputStream = outputStream;
    }

    @Override
    public OutputStream openOutputStream() {
        return outputStream;
    }

    public byte[] read() {
        return outputStream.toByteArray();
    }
}
