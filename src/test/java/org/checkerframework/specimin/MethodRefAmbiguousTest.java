package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that when a method reference is ambiguous, Specimin both preserves every
 * candidate and still synthesizes the reference's target functional interface. Here {@code
 * Foo::mref} matches two overloads, and the interface it targets, {@code org.example.Handler}, is
 * unsolved; the output needs all three of those symbols to compile.
 *
 * <p>These two jobs look mutually exclusive but are not, and the output above is what pins that:
 * preservation happens only when the reference has candidate declarations, while synthesis of the
 * referenced method happens only when it has none, so neither can duplicate the other's work.
 * Synthesis of the target functional interface is a separate matter and has to run either way,
 * which is why {@link Slicer#handleElement} leaves its {@code generateUnsolvedSymbol} flag set
 * after preserving candidates. Clearing it there instead breaks this test, {@link
 * MethodRefInCastTest}, {@link MethodRefUnsolvedScopeTest}, {@link CtorRefUnsolvedTest}, and {@link
 * UnsolvedMethodReferenceWithKnownLHSTest}.
 */
public class MethodRefAmbiguousTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "methodrefambiguous",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
