package com.example;

import java.util.List;
import java.util.Map;
import org.plumelib.util.CollectionsPlume;

class Simple {

    Map<MethodSignature, MethodSignature> replacementMap;

    void bar() {
        List<String> signatureList =
            CollectionsPlume.mapList(MethodSignature::toString, replacementMap.keySet());
    }
}
