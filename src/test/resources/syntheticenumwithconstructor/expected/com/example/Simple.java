package com.example;

import org.example.Constants;
import org.example.Header;

public class Simple {

    @Header(Constants.DEFAULT)
    public void target() {
        Constants c = new Constants(5);
    }
}
