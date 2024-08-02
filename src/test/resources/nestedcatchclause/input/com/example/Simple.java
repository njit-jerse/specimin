package com.example;

public class Simple {

    public void test() {
        try {
            throw new CustomException("dummy");
        } catch (CustomException e) {
            System.out.println("Caught custom exception: " + e.getMessage());
            if (e.getMessage().contains("occurred")) {
                System.out.println("Exception message indicates an occurrence.");
            }
            handleException(e);
        }
    }
    private void handleException(CustomException e) {
        System.out.println("Handling the exception: " + e.getMessage());
    }
}
