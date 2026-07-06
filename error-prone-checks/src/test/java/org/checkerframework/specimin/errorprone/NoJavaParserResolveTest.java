package org.checkerframework.specimin.errorprone;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NoJavaParserResolveTest {
    private CompilationTestHelper compilationHelper;

    @BeforeEach
    public void setup() {
        compilationHelper = CompilationTestHelper.newInstance(
                NoJavaParserResolve.class, getClass());
    }

    @Test
    public void flagsDirectResolveCall() {
        compilationHelper
                .addSourceLines(
                        "Test.java",
                        "import com.github.javaparser.ast.type.ClassOrInterfaceType;",
                        "class Test {",
                        "  void foo(ClassOrInterfaceType type) {",
                        "    // BUG: Diagnostic contains: NoJavaParserResolve",
                        "    type.resolve();",
                        "  }",
                        "}")
                .doTest();
    }
}