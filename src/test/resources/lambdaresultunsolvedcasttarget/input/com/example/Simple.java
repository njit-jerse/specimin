package com.example;

import java.util.function.Supplier;
import org.example.Unsolved;

public class Simple {
    Object foo() {
        return (Supplier<Fn>) () -> Unsolved.make();
    }
}
