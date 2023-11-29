package com.example;

public class Issue38 {

    static class ExampleClass {
    }

    public void test() {
        try {
            Class<?> exampleClass = ExampleClass.class;
            ExampleClass instance = (ExampleClass) exampleClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
