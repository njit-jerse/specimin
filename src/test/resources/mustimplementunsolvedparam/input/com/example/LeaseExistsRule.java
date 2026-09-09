package com.example;

import com.other.InstanceInfo;

public class LeaseExistsRule implements Rule {
    public String apply(InstanceInfo instanceInfo) {
        return "lease";
    }
}
