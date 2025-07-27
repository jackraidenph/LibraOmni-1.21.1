package dev.jackraidenph.libraomni.data;

import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ModAnnotatedData {

    private final Set<AnnotatedReflectionData<?>> annotatedReflectionData = new HashSet<>();

    public void addElement(Element element, Elements elementUtils) {
        AnnotatedReflectionData<?> dataObject =
                switch (element) {
                    case TypeElement clazz -> new ClassData(clazz, elementUtils);
                    case VariableElement field -> new FieldData(field, elementUtils);
                    case ExecutableElement executable -> switch (executable.getKind()) {
                        case METHOD -> new MethodData(executable, elementUtils);
                        case CONSTRUCTOR -> new ConstructorData(executable, elementUtils);
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
