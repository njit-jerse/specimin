package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A call to an enum's implicitly declared {@code values()} or {@code valueOf(String)} (JLS 8.9.3)
 * as an argument of an unresolvable call.
 *
 * <p>That nesting is what makes this different from {@link ImplicitEnumMethods2Test}: {@code
 * UnsolvedSymbolGenerator#inferContextImpl} recurses into sub-expressions, so the inner call is
 * handled even though it resolves perfectly well, and the code that handles it assumed a resolved
 * method has a method AST. These two have none -- JavaParser reports the enum declaration as their
 * AST -- so reading it as a method's used to throw {@code ClassCastException}.
 */
public class ImplicitEnumMethods3Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "implicitenummethods3",
        new String[] {"p/UsesEnums.java"},
        new String[] {"p.UsesEnums#target(Unsolved)"});
  }
}
