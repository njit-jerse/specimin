package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a method reference whose scope is a variable of an unsolved type puts the
 * referenced method in the synthetic class for the variable's type.
 *
 * <p>JavaParser parses the {@code g} in {@code g::mref} as a type name, because the grammar cannot
 * tell a type name from an expression name in that position; JLS 15.13 says it is a variable
 * whenever one of that name is in scope. Specimin used to take JavaParser's parse at face value and
 * emit a class named {@code com.example.g} holding a static {@code mref()}, which does not compile,
 * leaving {@code org.example.Foo} empty.
 *
 * <p>Unlike {@link MethodRefInCastTest}, the reference here is the initializer of a variable
 * declarator, a context in which JavaParser can find the target functional interface. So this test
 * covers the same disambiguation without the resolution crash that motivated it.
 */
public class MethodRefUnsolvedScopeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "methodrefunsolvedscope",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
