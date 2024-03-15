package com.example;

public final class Simple {

    private static Object test() {
        return new Object() {

            public void anotherMethod() {

            }

            public String toString() {
                cast(new Object());
                return super.toString();
            }
        };
    }

    private static void cast(Object value) {
        throw new Error();
    }
}