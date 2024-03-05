package com.example;

import org.testing.UnsolvedType;

public class Simple {


    public void foo(SomeClass inner) {
        throw new RuntimeException();
    }
}

// SomeClass is unsolved, and there is no SomeClass.java in the original codebase. However, no synthetic SomeClass should be created.
class SomeClass extends UnsolvedType {

 }