package com.example;

public class Simple {

    static class UnmodifiableCollection<E> {
    }

    static class UnmodifiableSet<E> extends UnmodifiableCollection<E> {
    }

    static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E> {
    }

    static class UnmodifiableNavigableSet<E> extends UnmodifiableSortedSet<E> implements NavigableSet<E> {

        @SuppressWarnings("rawtypes")
        private static final NavigableSet<?> EMPTY_NAVIGABLE_SET = null;
    }

    @SuppressWarnings("unchecked")
    public static <E> NavigableSet<E> emptyNavigableSet() {
        return (NavigableSet<E>) UnmodifiableNavigableSet.EMPTY_NAVIGABLE_SET;
    }
}
