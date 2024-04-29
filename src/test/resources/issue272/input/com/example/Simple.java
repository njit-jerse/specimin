package com.example;

import com.sun.source.tree.Tree;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;

import javax.lang.model.element.AnnotationMirror;

public abstract class Simple {

    public final AnnotationMirror annotation;

    // Target method. Note that StringToJavaExpression is an interface in the same package as
    // the target method's class.
    public AnnotationMirror viewpointAdaptDependentTypeAnnotation(
            GenericAnnotatedTypeFactory<?, ?, ?, ?> factory,
            StringToJavaExpression stringToJavaExpr,
            @Nullable Tree errorTree) {
        DependentTypesHelper dependentTypesHelper = factory.getDependentTypesHelper();
        AnnotationMirror standardized =
                dependentTypesHelper.convertAnnotationMirror(stringToJavaExpr, annotation);
        if (standardized == null) {
            return annotation;
        }
        if (errorTree != null) {
            dependentTypesHelper.checkAnnotationForErrorExpressions(standardized, errorTree);
        }
        return standardized;
    }
}
