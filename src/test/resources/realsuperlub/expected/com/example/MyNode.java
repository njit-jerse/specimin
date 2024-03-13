package com.example;
import org.mygraphlib.Node;
import org.mygraphlib.NodeType1;

public class MyNode extends Node {
    public Node target(int x) {
        if (x < 0) {
            child = child.getChild();
            return child;
        } else if (x == 0) {
            return this.getChild();
        } else {
            child = new NodeType1();
            return child;
        }
    }
}
