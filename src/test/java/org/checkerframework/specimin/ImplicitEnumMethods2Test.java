package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link ImplicitEnumMethodsTest}, but the program also contains an unsolved symbol. That
 * matters because {@link org.checkerframework.specimin.unsolved.UnsolvedSymbolGenerator} only runs
 * when there is something to generate, so the code that handles an enum's implicitly declared
 * {@code values()} and {@code valueOf(String)} (JLS 8.9.3) is unreachable without one. Those
 * methods have no AST node of their own, and JavaParser reports the enum declaration as their AST;
 * that declaration is neither a {@code NodeWithType} nor a {@code NodeWithParameters}, so code
 * assuming it had a method's AST used to fail.
 *
 * <p>The nested enum is here because it is not redundant with the top-level one: a nested enum
 * reaches these casts too, so it crashed even where {@link ImplicitEnumMethodsTest}'s nested case
 * succeeded.
 */
public class ImplicitEnumMethods2Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "implicitenummethods2",
        new String[] {"p/UsesEnums.java"},
        new String[] {"p.UsesEnums#target()"});
  }
}
