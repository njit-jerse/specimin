package com.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface Foo {

    public Deprecated x();

    public Anno[] y();
}