package com.netflix.eureka.lease;

public class Lease<T> {
  private final T holder;

  public Lease(T holder) {
    this.holder = holder;
  }

  public T getHolder() {
    return holder;
  }
}
