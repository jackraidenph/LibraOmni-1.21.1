package dev.jackraidenph.libraomni.annotation.runprocessing.proceesor.stereotype;

import java.lang.reflect.Field;

public interface FieldProcessor extends RuntimeAnnotationProcessor<Field> {

    void process(Field field);

}
