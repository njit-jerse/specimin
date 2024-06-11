package com.example;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

class Simple {

  public boolean isSideEffectFree(ExecutableElement methodElement) {
    List<? extends AnnotationMirror> annotationMirrors = methodElement.getAnnotationMirrors();
    for (AnnotationMirror am : annotationMirrors) {
      boolean found = AnnotationUtils.areSameByName(am, "org.checkerframework.dataflow.qual.SideEffectFree");
      if (found) {
        return true;
      }
    }
    return false;
  }
}
