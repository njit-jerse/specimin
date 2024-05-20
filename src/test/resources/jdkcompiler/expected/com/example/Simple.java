package com.example;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

class Simple {
    void bar(Tree tree) {
        Kind kind = tree.getKind();
    }
}
