package com.example;

class Simple extends AbstractSimple {
    // Target method, no content
    void bar() {
    }

    // This method is abstract in AbstractSimple.
    @Override
    Object baz(Object obj) {
        return obj.toString();
    }
}
