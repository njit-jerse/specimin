package com.example;

import java.util.AbstractCollection;
import java.util.Collection;

public abstract class AbstractLinkedDeque<E> extends AbstractCollection<E> implements LinkedDeque<E> {

    public boolean isEmpty() {
        throw new java.lang.Error();
    }

    public int size() {
        throw new java.lang.Error();
    }

    public void clear() {
        throw new java.lang.Error();
    }

    public abstract boolean contains(Object o);

    public E peek() {
        throw new java.lang.Error();
    }

    public E peekFirst() {
        throw new java.lang.Error();
    }

    public E peekLast() {
        throw new java.lang.Error();
    }

    public E getFirst() {
        throw new java.lang.Error();
    }

    public E getLast() {
        throw new java.lang.Error();
    }

    public E element() {
        throw new java.lang.Error();
    }

    public boolean offer(E e) {
        throw new java.lang.Error();
    }

    public boolean offerFirst(E e) {
        throw new java.lang.Error();
    }

    public boolean offerLast(E e) {
        throw new java.lang.Error();
    }

    public boolean add(E e) {
        throw new java.lang.Error();
    }

    public void addFirst(E e) {
        throw new java.lang.Error();
    }

    public void addLast(E e) {
        throw new java.lang.Error();
    }

    public E poll() {
        throw new java.lang.Error();
    }

    public E pollFirst() {
        throw new java.lang.Error();
    }

    public E pollLast() {
        throw new java.lang.Error();
    }

    public E remove() {
        throw new java.lang.Error();
    }

    public E removeFirst() {
        throw new java.lang.Error();
    }

    public boolean removeFirstOccurrence(Object o) {
        throw new java.lang.Error();
    }

    public E removeLast() {
        throw new java.lang.Error();
    }

    public boolean removeLastOccurrence(Object o) {
        throw new java.lang.Error();
    }

    public boolean removeAll(Collection<?> c) {
        throw new java.lang.Error();
    }

    public void push(E e) {
        throw new java.lang.Error();
    }

    public E pop() {
        throw new java.lang.Error();
    }

    public PeekingIterator<E> iterator() {
        throw new java.lang.Error();
    }

    public PeekingIterator<E> descendingIterator() {
        throw new java.lang.Error();
    }
}
