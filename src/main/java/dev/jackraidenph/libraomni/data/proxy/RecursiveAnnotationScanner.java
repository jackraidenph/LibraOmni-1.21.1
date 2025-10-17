package dev.jackraidenph.libraomni.data.proxy;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementScanner14;
import java.util.HashSet;
import java.util.Set;

class RecursiveAnnotationScanner extends ElementScanner14<Set<Element>, TypeElement> {

    private final Set<Element> elements = new HashSet<>();
    private final Set<String> elementIdentities = new HashSet<>(); //Somehow, Set<Element> allows duplicates?

    public Set<Element> getElements() {
        return elements;
    }

    @Override
    public Set<Element> scan(Element e, TypeElement annotation) {
        //Make a proxy of the element that supports composed annotations
        Element toCheck = (Element) ProxyFactory.proxifyAnnotatedConstructIfNotProxy(e);
        //Check if requested annotation's TypeElement is present
        checkViaTypeElement(toCheck, annotation);
        //Accept further down
        toCheck.accept(this, annotation);
        return elements;
    }

    public void checkViaTypeElement(Element e, TypeElement a) {
        String identity = e.toString();
        //If element was already present - skip
        if (!elementIdentities.add(identity)) {
            return;
        }
        //For all annotation mirrors
        for (AnnotationMirror mirror : e.getAnnotationMirrors()) {
            DeclaredType type = mirror.getAnnotationType();
            //Get its TypeElement and check if it's the requested one
            if (type.asElement() instanceof TypeElement typeElement && typeElement.equals(a)) {
                elements.add(e);
            }
        }
    }
}
