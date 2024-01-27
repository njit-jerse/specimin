package com.example;

import java.util.IdentityHashMap;
import com.sun.source.tree.Tree;
import java.util.Set;

class Simple {

    protected IdentityHashMap<Tree, Set<Tree>> treeLookup;

    public boolean bar() {
        return treeLookup.isEmpty();
    }
}
