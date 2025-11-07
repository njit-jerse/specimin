package com.example;

import java.util.IdentityHashMap;
import java.util.Set;
import org.testing.BigTree;

class Simple {

    protected IdentityHashMap<BigTree, Set<BigTree>> treeLookup;

    public boolean bar() {
        return treeLookup.isEmpty();
    }
}
