package org.example.app;

class Greeter {

    private static final String greeting;

    static {
        greeting = GreetingFactory.buildGreeting();
    }

    String greet() {
        return greeting.toUpperCase();
    }
}
