package com.example;

import java.util.IdentityHashMap;
import com.sun.source.tree.BigTree;
import java.util.Set;

class Simple {

    protected IdentityHashMap<BigTree, Set<BigTree>> treeLookup;

    public boolean bar() {
        return treeLookup.isEmpty();
    }
}
