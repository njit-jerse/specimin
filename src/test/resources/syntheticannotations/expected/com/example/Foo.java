package com.example;

@java.lang.annotation.Target({ 
    java.lang.annotation.ElementType.TYPE, 
    java.lang.annotation.ElementType.FIELD, 
    java.lang.annotation.ElementType.METHOD, 
    java.lang.annotation.ElementType.PARAMETER, 
    java.lang.annotation.ElementType.CONSTRUCTOR, 
    java.lang.annotation.ElementType.LOCAL_VARIABLE, 
    java.lang.annotation.ElementType.ANNOTATION_TYPE,
    java.lang.annotation.ElementType.PACKAGE,
    java.lang.annotation.ElementType.TYPE_USE 
})
public @interface Foo {

    public Deprecated x();

    public Anno[] y();
}
