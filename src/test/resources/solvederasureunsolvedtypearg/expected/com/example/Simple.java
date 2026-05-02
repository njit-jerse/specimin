package com.example;

import java.util.List;

public class Simple {

  public void foo() {
    List<Foo> list;
    list.get(0);
    list.get(0).bar();
    Foo foo = list.stream().filter(e -> e.bar() > 0).findFirst();
    List<List<Foo>> listOfLists;
    listOfLists.get(0);
    listOfLists.get(0).get(0).bar();
    List<Foo> foos = listOfLists.stream().filter(e -> e.get(0).bar() > 0).findFirst();
  }
}
