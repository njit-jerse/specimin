/*
 * Based on an example from java.util.Collections.
 */
package com.example;

import java.io.Serializable;
import java.util.TreeSet;

public class Simple {

    static class UnmodifiableCollection<E> {
        UnmodifiableCollection(Collection<? extends E> c) {
        }
    }

    static class UnmodifiableSet<E> extends UnmodifiableCollection<E> {
        UnmodifiableSet(Set<? extends E> s)     {super(s);}
    }

    static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E> {
        UnmodifiableSortedSet(SortedSet<E> s) {super(s); }
    }

    static class UnmodifiableNavigableSet<E> extends UnmodifiableSortedSet<E> implements NavigableSet<E> {
        UnmodifiableNavigableSet(NavigableSet<E> s)         {super(s);}

        private static class EmptyNavigableSet<E> extends UnmodifiableNavigableSet<E>
                implements Serializable {
            private static final long serialVersionUID = -6291252904449939134L;

            public EmptyNavigableSet() {
                super(new TreeSet<E>());
            }

            private Object readResolve()        { return EMPTY_NAVIGABLE_SET; }
        }

        @SuppressWarnings("rawtypes")
        private static final NavigableSet<?> EMPTY_NAVIGABLE_SET = new EmptyNavigableSet<>();
    }
}
