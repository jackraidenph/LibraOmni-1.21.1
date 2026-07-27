package dev.jackraidenph.libraomni.data;

import dev.jackraidenph.libraomni.data.reflect.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ModAnnotatedData {

    private final Set<AnnotatedReflectionData<?>> annotatedReflectionData = new HashSet<>();

    private transient final Set<AnnotatedElement> dataCache = new HashSet<>();
    private transient boolean cacheDirty = true;

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
        cacheDirty = true;
    }

    public Set<AnnotatedElement> getElements() {
        if (cacheDirty) {
            dataCache.clear();
            annotatedReflectionData.stream().map(AnnotatedReflectionData::construct).forEach(dataCache::add);
            cacheDirty = false;
        }

        return Collections.unmodifiableSet(dataCache);
    }

    public boolean contains(Object object) {
        if (!(object instanceof AnnotatedElement annotatedElement)) {
            return false;
        }

        return getElements().contains(annotatedElement);
    }
}
