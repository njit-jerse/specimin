package com.example;

import org.other.Absent;

public class Simple {

    void bar() {
        Factory.<Absent>make().absentMethod();
    }
}
