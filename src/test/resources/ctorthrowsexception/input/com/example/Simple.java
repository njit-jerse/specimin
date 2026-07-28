package com.example;

public class Simple {
    // The constructor's throws clause is the only evidence about CustomException.
    // Specimin should notice this and generate CustomException as a (checked)
    // Throwable subtype, rather than a plain class. Since nothing constructs
    // Simple here, a checked exception (extends java.lang.Exception) is valid.
    public Simple() throws CustomException {
    }
}
