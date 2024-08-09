package com.example;

import java.util.Map;
import java.util.List;

public class Bar {

    public static void noParamVoid() {
        throw new Error();
    }

    public static String noParamNonVoid() {
        throw new Error();
    }

    public static void oneParamVoid(int x) {
        throw new Error();
    }

    public static Object oneParamNonVoid(Map<Object, String> x) {
        throw new Error();
    }

    public static void twoParamVoid(String x, boolean y) {
        throw new Error();
    }

    public static int twoParamNonVoid(Object x, Object y) {
        throw new Error();
    }

    public static void threeParamVoid(Object x, List<List<Object>> y, Object z) {
        throw new Error();
    }

    public static Object threeParamNonVoid(Object x, Object y, Object z) {
        throw new Error();
    }
}
