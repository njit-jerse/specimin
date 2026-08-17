package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A lambda result type need not be a final class to rule out a generated placeholder type. An array
 * type has no declarable subtypes at all: by JLS 4.10.3 its only non-array supertypes are {@code
 * Object}, {@code Cloneable} and {@code Serializable}, and no class declaration can name it as a
 * superclass.
 *
 * <p>Two lambdas here demand incompatible result types from the same unsolved call, so no concrete
 * return type satisfies both and the unconstrained type variable is the only option. Without
 * recognizing {@code String[]} as non-extendable, the {@code Payload} constraint wins and the
 * output stops compiling.
 */
public class LambdaResultUnsolvedArrayTargetTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedarraytarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
