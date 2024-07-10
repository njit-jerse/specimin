package com.example;

@java.lang.annotation.Target({ java.lang.annotation.ElementType.TYPE_USE, java.lang.annotation.ElementType.METHOD })
public @interface Baz {

    public String foo();

    public Class<?> bar();
}
