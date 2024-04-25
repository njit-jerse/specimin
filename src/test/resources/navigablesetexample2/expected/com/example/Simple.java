package com.example;

public class Simple {

    static class UnmodifiableCollection<E> {
    }

    static class UnmodifiableSet<E> extends UnmodifiableCollection<E> {
    }

    static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E> {
    }

    public static <T> NavigableSet<T> unmodifiableNavigableSet(NavigableSet<T> s) {
        return new UnmodifiableNavigableSet<>(s);
    }

    static class UnmodifiableNavigableSet<E> extends UnmodifiableSortedSet<E> implements NavigableSet<E> {

        UnmodifiableNavigableSet(NavigableSet<E> s) {
            throw new Error();
        }
    }
}
