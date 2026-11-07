package com.example;

public interface LeaseManager<T> {
  void register(T r, int leaseDuration);
}
