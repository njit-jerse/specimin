package com.example;

// These imports shouldn't be preserved, even though they are
// used by the target method in Simple.java; this whole file should be deleted.
import java.util.ArrayList;
import java.util.List;

class Other {
    void bar() {
        List l = new ArrayList();
    }
}
