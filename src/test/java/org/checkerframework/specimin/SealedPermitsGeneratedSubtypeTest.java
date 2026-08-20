package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The one case in which a sealed class does take a synthetic subtype: its own {@code permits}
 * clause names a type Specimin has to generate. {@code Baz} must then be declared {@code extends
 * SealedBase}, because a permitted class is required to be a direct subclass, and it must be
 * declared {@code non-sealed} so that it can be extended in turn.
 *
 * <p>Treating sealed types as unextendable therefore must not be read as "no synthetic type may
 * name this as a supertype". It is the {@code permits} clause that establishes this relationship,
 * where it is processed; {@link NonExtendableCastOperandSealedTest} is what asks for it from the
 * outside, and is refused. This fixture guards the boundary between those two.
 *
 * <p>The cast that appears here is precisely such an outside request, and its being refused is
 * visible in the output: {@code get}'s return type falls back rather than {@code Baz} acquiring
 * {@code SealedBase} a second time. When the cast did impose it, the two routes disagreed about
 * what kind of supertype it was and the output said {@code implements} for a class.
 */
public class SealedPermitsGeneratedSubtypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "sealedpermitsgeneratedsubtype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
