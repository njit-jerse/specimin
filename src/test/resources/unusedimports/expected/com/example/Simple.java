package com.example;

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;
import static java.util.Map.*;

import java.io.*;
import java.util.List;

public class Simple {

    public ManyImports shouldNotBeRemoved() {
        List<Entry<Object, Object>> x;
        sqrt(PI);
        File file;
        return null;
    }
}
