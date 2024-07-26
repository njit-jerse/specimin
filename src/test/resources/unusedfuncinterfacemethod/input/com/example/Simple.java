package com.example;

public class Simple implements FuncInterfaceUnused {
    public void foo() {
        Bar.use((a, b, c, d) -> 0);
    }

    public void shouldBeRemoved() {

    }
}
