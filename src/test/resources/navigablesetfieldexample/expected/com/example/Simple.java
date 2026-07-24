package com.example;

import java.io.Serializable;

public class Simple {

  static class UnmodifiableCollection<E> {

    UnmodifiableCollection(Collection<? extends E> c) {
      throw new java.lang.Error();
    }
  }

  static class UnmodifiableSet<E> extends UnmodifiableCollection<E> {

    UnmodifiableSet(Set<? extends E> s1) {
      super(s1);
    }
  }

  static class UnmodifiableSortedSet<E> extends UnmodifiableSet<E> {

    UnmodifiableSortedSet(SortedSet<E> s) {
      super(s);
    }
  }

  static class UnmodifiableNavigableSet<E> extends UnmodifiableSortedSet<E>
      implements NavigableSet<E> {

    UnmodifiableNavigableSet(NavigableSet<E> s) {
      super(s);
    }

    private static class EmptyNavigableSet<E> extends UnmodifiableNavigableSet<E>
        implements Serializable {

      public EmptyNavigableSet() {
        super(new TreeSet<E>());
      }
    }

    @SuppressWarnings("rawtypes")
    private static final NavigableSet<?> EMPTY_NAVIGABLE_SET = new EmptyNavigableSet<>();
  }
}
