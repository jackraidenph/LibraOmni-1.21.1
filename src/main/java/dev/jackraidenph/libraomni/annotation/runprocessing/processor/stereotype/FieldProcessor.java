package dev.jackraidenph.libraomni.annotation.runprocessing.processor.stereotype;

import java.lang.reflect.Field;

public interface FieldProcessor extends RuntimeAnnotationProcessor<Field> {

    void process(Field field);

}
