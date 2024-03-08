package org.example;

@FunctionalInterface
public interface Consumer3<T, T1, T2> {

    public void apply(T parameter0, T1 parameter1, T2 parameter2);
}