package com.example;

import java.util.Map;
import java.util.List;

public class Bar {
    public static void noParamVoid() { }
    public static String noParamNonVoid() { return null; }
    public static void oneParamVoid(int x) { }
    public static Object oneParamNonVoid(Map<Object, String> x) { return null; }
    public static void twoParamVoid(String x, boolean y) { }
    public static int twoParamNonVoid(Object x, Object y) { return 0; }
    public static void threeParamVoid(Object x, List<List<Object>> y, Object z) { }
    public static Object threeParamNonVoid(Object x, Object y, Object z) { return null; }
}
