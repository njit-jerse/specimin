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
                        "    // BUG: Diagnostic contains: NoJavaParserResolve",
                        "    java.util.function.Supplier<?> f = type::resolve;",
                        "  }",
                        "}")
                .doTest();
    }

    // TypeDeclaration declares its own resolve() without implementing Resolvable; only its concrete
    // subclasses do.
    @Test
    public void flagsResolveCallOnTypeDeclaration() {
        compilationHelper
                .addSourceLines(
                        "Test.java",
                        "import com.github.javaparser.ast.body.TypeDeclaration;",
                        "class Test {",
                        "  void foo(TypeDeclaration<?> type) {",
                        "    // BUG: Diagnostic contains: NoJavaParserResolve",
                        "    type.resolve();",
                        "    // BUG: Diagnostic contains: NoJavaParserResolve",
                        "    java.util.function.Supplier<?> f = type::resolve;",
                        "  }",
                        "}")
                .doTest();
    }
}