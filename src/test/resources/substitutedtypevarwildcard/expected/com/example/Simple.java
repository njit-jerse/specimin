package com.example;

import org.other.Absent;

public class Simple {

    void bar(Container<? extends Absent> c) {
        c.get().absentMethod();
    }
}
