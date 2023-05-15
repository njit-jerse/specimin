package com.example;
import java.util.Random;

public class Simple {
    public void test() {
        Random random = new Random();
        int randomNumber = random.nextInt(100);
        System.out.println("You got: " + randomNumber);
    }
}
