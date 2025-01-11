package dev.jackraidenph.libraomni.annotation.runprocessing.proceesor.stereotype;

public interface RuntimeAnnotationProcessor<T> {

    void process(T element);

}
