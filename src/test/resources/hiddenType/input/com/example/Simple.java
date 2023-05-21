package com.example;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;

public class Simple {
    private boolean isVoidType (MethodDeclaration method) {
        return method.getType().isVoidType();
    }
}
