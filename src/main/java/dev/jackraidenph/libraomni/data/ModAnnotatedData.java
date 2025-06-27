package dev.jackraidenph.libraomni.data;

import javax.lang.model.element.*;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ModAnnotatedData {

    private final Set<AnnotatedReflectionData<?>> annotatedReflectionData = new HashSet<>();

    public void addElement(Element element) {
        AnnotatedReflectionData<?> dataObject =
                switch (element) {
                    case TypeElement clazz -> new ClassData(clazz);
                    case VariableElement field -> new FieldData(field);
                    case ExecutableElement executable -> switch (executable.getKind()) {
                        case METHOD -> new MethodData(executable);
                        case CONSTRUCTOR -> new ConstructorData(executable);
                        default -> throw new UnsupportedOperationException();
                    };
                    default -> throw new UnsupportedOperationException();
                };

        this.annotatedReflectionData.add(dataObject);
    }

    public Set<AnnotatedElement> getElements() {
        return annotatedReflectionData.stream().map(AnnotatedReflectionData::construct).collect(Collectors.toSet());
    }
}
