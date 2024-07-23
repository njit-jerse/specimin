package com.example;

import java.util.Deque;
import java.util.Iterator;

public interface LinkedDeque<E> extends Deque<E> {

    interface PeekingIterator<E> extends Iterator<E> {
    }
}
