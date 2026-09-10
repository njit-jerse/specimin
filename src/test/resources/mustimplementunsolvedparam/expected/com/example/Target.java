package com.example;

import com.other.InstanceInfo;

public class Target {

    String target(InstanceInfo info) {
        Rule rule = new LeaseExistsRule();
        return rule.apply(info);
    }
}
