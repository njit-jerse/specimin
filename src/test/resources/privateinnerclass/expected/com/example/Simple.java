package com.example;

class Simple {
    void bar() {
        MyList<Example> examples = new MyList();
        examples.add(new Example());
    }

    private class Example {

    }
}
