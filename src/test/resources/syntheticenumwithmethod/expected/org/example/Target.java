package org.example;

import org.example.dependency.ExternalEnum;
import org.example.dependency.Header;

public class Target {

    @Header(ExternalEnum.CONSTANT)
    public void target() {
        ExternalEnum.method();
    }
}
