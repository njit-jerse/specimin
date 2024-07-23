package com.example;

import java.util.Deque;

public final class AccessOrderDeque<E> extends AbstractLinkedDeque<E> {

    public boolean contains(Object o) {
        throw new Error();
    }

    public boolean remove(Object o) {
        throw new Error();
    }

    public E getPrevious(E e) {
        throw new Error();
    }

    public void setPrevious(E e, E prev) {
        throw new Error();
    }

    public E getNext(E e) {
        throw new Error();
    }

    public void setNext(E e, E next) {
        throw new Error();
    }
}
