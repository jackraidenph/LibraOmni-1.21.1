package dev.jackraidenph.libraomni.annotation.runprocessing.proceesor.stereotype;

import java.lang.reflect.Executable;

public interface ExecutableProcessor extends RuntimeAnnotationProcessor<Executable> {

    void process(Executable executable);

}
