package com.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface Baz {

    public String foo();

    public Class<?> bar();
}
