package com.example;

import org.example.MyEnum;

// The Java scoping rules for switches over enum constants
// are surprisingly complicated and poorly documented by the JLS.
// Specifically, Java permits an unqualified enum constant that
// is a member of the type used as the selector expression in
// a switch statement to be used either as a rule or as a case,
// without an explicit import. Specimin needs to correctly
// identify those cases as enum constants, because otherwise
// there usually is not _anything_ in scope that matches, which
// causes us to produce non-sensical output. However, I don't
// _think_ that the unqualified enum constant names can be used
// in the bodies of the expressions in the rest of the switch,
// and have implemented Specimin's logic assuming that is so.
// This test case checks that we get this right; this variant
// differs from the other because it checks the superclass case.
class Simple extends Foo {
    // Target method.
    void bar(MyEnum m) {
        switch(m) {
            case CONSTANT1:
                int x = CONSTANT5; // CONSTANT5 should go into the super class, not into the enum
                break;
            case CONSTANT2:
                int y = 3 * 2;
                break;
        }
        int z = switch (m) {
            case CONSTANT3 -> CONSTANT6; // CONSTANT6 should go into the super class, not into the enum
            case CONSTANT4 -> 6;
            default -> 0;
        };
    }
}
