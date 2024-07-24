package com.example;

import java.util.AbstractCollection;

public abstract class AbstractLinkedDeque<E> extends AbstractCollection<E> implements LinkedDeque<E> {

    public int size() {
        throw new Error();
    }

    public E peek() {
        throw new Error();
    }

    public E peekFirst() {
        throw new Error();
    }

    public E peekLast() {
        throw new Error();
    }

    public E getFirst() {
        throw new Error();
    }

    public E getLast() {
        throw new Error();
    }

    public E element() {
        throw new Error();
    }

    public boolean offer(E e) {
        throw new Error();
    }

    public boolean offerFirst(E e) {
        throw new Error();
    }

    public boolean offerLast(E e) {
        throw new Error();
    }

    public void addFirst(E e) {
        throw new Error();
    }

    public void addLast(E e) {
        throw new Error();
    }

    public E poll() {
        throw new Error();
    }

    public E pollFirst() {
        throw new Error();
    }

    public E pollLast() {
        throw new Error();
    }

    public E remove() {
        throw new Error();
    }

    public E removeFirst() {
        throw new Error();
    }

    public boolean removeFirstOccurrence(Object o) {
        throw new Error();
    }

    public E removeLast() {
        throw new Error();
    }

    public boolean removeLastOccurrence(Object o) {
        throw new Error();
    }

    public void push(E e) {
        throw new Error();
    }

    public E pop() {
        throw new Error();
    }

    public PeekingIterator<E> iterator() {
        throw new Error();
    }

    public PeekingIterator<E> descendingIterator() {
        throw new Error();
    }
}
