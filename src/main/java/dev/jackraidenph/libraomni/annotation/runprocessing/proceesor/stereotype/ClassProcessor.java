package dev.jackraidenph.libraomni.annotation.runprocessing.proceesor.stereotype;

public interface ClassProcessor<T> extends RuntimeAnnotationProcessor<Class<T>> {

    void process(Class<T> type);

}
