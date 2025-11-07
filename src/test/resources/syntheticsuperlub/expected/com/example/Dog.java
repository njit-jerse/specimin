package com.example;

import org.wild.Mammal;
import org.wild.RegularPaws;
import org.wild.WebbedPaws;

public class Dog extends Mammal {
    public void setup(String breed) {
        if (breed.contains("Water Dog")) {
            this.paws = new WebbedPaws();
        } else {
            this.paws = new RegularPaws();
        }
        paws.setNumber(4);
    }
}
