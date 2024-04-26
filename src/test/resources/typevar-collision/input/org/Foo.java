package org;

// This import isn't used. But JavaParser is imprecise, hence com/T.java will still be included in the final output.
import com.T;

public class Foo<T> {
    T useT(T t) {
        return t;
    }
}
