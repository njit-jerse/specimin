package com.example;

import java.lang.annotation.Annotation;

public class Simple {

    public enum Kind {
        PRECONDITION(PreconditionAnnotation.class),
        POSTCONDITION(PostconditionAnnotation.class);

        public final Class<? extends Annotation> metaAnnotation;

        Kind(Class<? extends Annotation> metaAnnotation) {
            this.metaAnnotation = metaAnnotation;
        }
    }

    public void bar() {
        Kind kind = Kind.POSTCONDITION;
        kind = Kind.PRECONDITION;
    }
}
