package com.example;

import java.util.Set;
import java.util.HashSet;

public class Simple {
    public static boolean hasDuplicates(boolean[] a) { return false; }

    public static boolean hasNoDuplicates(boolean[] a) {
        return !hasDuplicates(a);
    }

    public static boolean hasDuplicates(byte[] a) { return false; }

    public static boolean hasNoDuplicates(byte[] a) {
        return !hasDuplicates(a);
    }

    public static boolean hasDuplicates(char[] a) { return false; }

    public static boolean noDuplicates(char[] a) {
        return !hasDuplicates(a);
    }

    public static boolean hasNoDuplicates(char[] a) {
        return !hasDuplicates(a);
    }

    public static boolean hasDuplicates(float[] a) { return false; }

    public static boolean noDuplicates(float[] a) {
        return !hasDuplicates(a);
    }

    public static boolean hasNoDuplicates(float[] a) {
        return !hasDuplicates(a);
    }

    public static boolean hasDuplicates(int[] a) {
        Set<Integer> hs = new HashSet<>();
        for (int i = 0; i < a.length; i++) {
            if (!hs.add(a[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean noDuplicates(int[] a) {
        return !hasDuplicates(a);
    }

    public static boolean hasNoDuplicates(int[] a) {
        return !hasDuplicates(a);
    }
}