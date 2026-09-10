package com.example;

import com.other.Missing;

public interface Rule {

  String apply(Outer<String>.Inner<Integer> x, Missing y);
}
