package com.example;

import java.util.Map;
import java.util.List;

public class Bar {

    public static void noParamVoid() {
        throw new java.lang.Error();
    }

    public static String noParamNonVoid() {
        throw new java.lang.Error();
    }

    public static void oneParamVoid(int x) {
        throw new java.lang.Error();
    }

    public static Object oneParamNonVoid(Map<Object, String> x) {
        throw new java.lang.Error();
    }

    public static void twoParamVoid(String x, boolean y) {
        throw new java.lang.Error();
    }

    public static int twoParamNonVoid(Object x, Object y) {
        throw new java.lang.Error();
    }

    public static void threeParamVoid(Object x, List<List<Object>> y, Object z) {
        throw new java.lang.Error();
    }

    public static Object threeParamNonVoid(Object x, Object y, Object z) {
        throw new java.lang.Error();
    }
}
