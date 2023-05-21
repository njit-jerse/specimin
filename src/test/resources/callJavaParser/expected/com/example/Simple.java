package com.example;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

public class Simple {
    public void test() {
        CompilationUnit compilationUnit = new CompilationUnit();
        Node child = compilationUnit.getChildNodes().get(0);
    }
}
