package com.example;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import org.example.Baz;

public class Simple {

    public void test(List<Baz> numbers) {
        List<String> squaredNumbers = numbers.stream().map(e -> e.printValue()).collect(Collectors.toList());
    }
}
