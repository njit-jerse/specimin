package com.example;

import javax.lang.model.element.AnnotationMirror;

public class AnnotationUtils {

  public static boolean areSameByName(AnnotationMirror am, String aname) {
    return aname.equals(annotationName(am));
  }

  public static final String annotationName(AnnotationMirror annotation) {
    return null;
  }
}
