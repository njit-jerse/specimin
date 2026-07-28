package com.example;

import java.lang.annotation.Annotation;

public class Simple {

    public enum Kind {

        PRECONDITION(PreconditionAnnotation.class), POSTCONDITION(PostconditionAnnotation.class);

        Kind(Class<? extends Annotation> metaAnnotation) {
            throw new java.lang.Error();
        }
    }

    public void bar() {
        Kind kind = Kind.POSTCONDITION;
        kind = Kind.PRECONDITION;
    }
}
