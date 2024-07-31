package com.example;

@java.lang.annotation.Target({ java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE_USE })
public @interface Baz {

    public String foo();

    public Class<?> bar();
}
