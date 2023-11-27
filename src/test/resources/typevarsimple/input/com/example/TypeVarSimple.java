package com.example;

import org.example.ClassWithTypeParam;

public class TypeVarSimple {
    public static <T> T methodWithTypeParameter(ClassWithTypeParam<T> param) {
        T result = param.getT();
        return result;
    }
}
