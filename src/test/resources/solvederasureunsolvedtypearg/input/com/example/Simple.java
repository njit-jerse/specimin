package com.example;

import java.util.List;
import java.util.stream.Collectors;

public class Simple {
    public void foo() {
        List<Foo> list;

        // No extra symbols should be generated even those these are technically unresolvable.
        list.get(0);
        list.get(0).bar();

        Foo foo = list.stream()
                .filter(e -> e.bar() > 0)
                .findFirst();

        List<List<Foo>> listOfLists;

        listOfLists.get(0);
        listOfLists.get(0).get(0).bar();

        List<Foo> foos = listOfLists.stream()
                .filter(e -> e.get(0).bar() > 0)
                .findFirst();
    }
}
