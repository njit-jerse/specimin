package com.example;

import java.util.Iterator;

public class Foo<E> {
    public Iterator<E> iterator() {
        return new AbstractLinkedIterator() {
            @Override
            E computeNext() {
                throw new Error();
            }
        };
    }

    public abstract class AbstractLinkedIterator implements Iterator3<E> {
        abstract E computeNext();

        @Override
        public E next() {
            throw new Error();
        }

        @Override
        public boolean hasNext() {
            throw new Error();
        }
    }

    public interface Iterator3<E> extends Iterator2<E> {
    }

    public interface Iterator2<E> extends Iterator1<E> {
    }
    
    public interface Iterator1<E> extends Iterator<E> {
    }
}
