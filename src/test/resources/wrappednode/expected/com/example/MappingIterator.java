package com.example;

import java.io.Closeable;
import java.util.*;
import tools.jackson.core.*;

public class MappingIterator<T> implements Iterator<T>, Closeable {

  public boolean hasNext() {
    throw new java.lang.Error();
  }

  public T next() {
    throw new java.lang.Error();
  }

  public void close() {
    throw new java.lang.Error();
  }

  public List<T> readAll() throws JacksonException {
    return readAll(new ArrayList<T>());
  }

  public <L extends List<? super T>> L readAll(L resultList) throws JacksonException {
    throw new java.lang.Error();
  }
}
