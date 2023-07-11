package com.example;
import org.factory.Car;

public class Simple {
    private boolean getWarranty (Car myCar) {

        return myCar.addYears(2).getWarranty();
    }
}
