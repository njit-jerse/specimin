/*
 * Based on an example from java.util.Collections.
 */
package com.example;

public class Simple {

    static class UnmodifiableCollection<E> {
    }

    static class UnmodifiableSet<E> extends UnmodifiableCollection<E> {
    }

    static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E> {
    }

    static class UnmodifiableNavigableSet<E> extends UnmodifiableSortedSet<E> implements NavigableSet<E> {

        private static class EmptyNavigableSet<E> extends UnmodifiableNavigableSet<E> {

            public EmptyNavigableSet() {
                throw new Error();
            }
        }

        @SuppressWarnings("rawtypes")
        private static final NavigableSet<?> EMPTY_NAVIGABLE_SET = new EmptyNavigableSet<>();
    }
}
