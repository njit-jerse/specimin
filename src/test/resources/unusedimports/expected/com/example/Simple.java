package com.example;

import java.util.List;
import java.io.*;
import static java.util.Map.*;
import static java.lang.Math.sqrt;
import static java.lang.Math.PI;

public class Simple {

    public ManyImports shouldNotBeRemoved() {
        List<Entry<Object, Object>> x;
        sqrt(PI);
        File file;
        return null;
    }
}
