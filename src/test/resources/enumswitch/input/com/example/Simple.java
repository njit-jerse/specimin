package com.example;

class Simple {
    // Target method.
    void bar(Analysis a) {
        switch(a.getDirection()) {
            case FORWARD:
                System.out.println("forward");
            case BACKWARD:
                System.out.println("backward");
            default:
                throw new RuntimeException();
        }
    }
}
