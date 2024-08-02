package com.example;

import org.example.Frame;
import org.example.InstConstraintVisitor;

public class Simple extends InstConstraintVisitor {

    // Target method. The name of this test comes from the fact
    // that this method triggered SpecSlice to accidentally create
    // two copies of this method in InstConstraintVisitor: one
    // with the fully-qualified parameter name, and one without.
    // This is the test for https://github.com/kelloggm/specSlice/issues/185.
    @Override
    public void setFrame(Frame f) {
    }
}