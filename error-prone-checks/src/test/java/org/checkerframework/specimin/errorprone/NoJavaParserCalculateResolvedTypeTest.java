package org.checkerframework.specimin.errorprone;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NoJavaParserCalculateResolvedTypeTest {
    private CompilationTestHelper compilationHelper;

    @BeforeEach
    public void setup() {
        compilationHelper = CompilationTestHelper.newInstance(
                NoJavaParserCalculateResolvedType.class, getClass());
    }

    @Test
    public void flagsDirectCalculateResolvedTypeCall() {
        compilationHelper
                .addSourceLines(
                        "Test.java",
                        "import com.github.javaparser.ast.expr.Expression;",
                        "class Test {",
                        "  void foo(Expression expr) {",
                        "    // BUG: Diagnostic contains: NoJavaParserCalculateResolvedType",
                        "    expr.calculateResolvedType();",
                        "    // BUG: Diagnostic contains: NoJavaParserCalculateResolvedType",
                        "    java.util.function.Supplier<?> f = expr::calculateResolvedType;",
                        "  }",
                        "}")
                .doTest();
    }
}