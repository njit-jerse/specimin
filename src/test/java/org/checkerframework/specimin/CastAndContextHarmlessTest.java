package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that casting the result of an unsolved method does not degrade the return type inferred
 * for that method at its other use sites. Here the cast target is {@code Object}, which constrains
 * nothing at all, so the assignment to a variable of type {@code Bar} must still force {@code
 * get()}'s return type to be a subtype of {@code Bar}. Before this was fixed, the cast contributed
 * {@code Object} as a candidate return type, which was then a defensible least-upper-bound of
 * {@code Object} and {@code Bar}; {@code get()} was widened to return {@code Object} and the
 * assignment no longer compiled.
 *
 * <p>The expected output routes the return type through the synthetic {@code GetReturnType extends
 * Bar} rather than using {@code Bar} directly. That extra class is not caused by the cast: any
 * unconstraining use site processed before the assignment produces it (replacing the cast with
 * {@code System.out.println(f.get())} yields the identical output), because the slicer's worklist
 * is a stack and so may create the method from the unconstraining site first. Collapsing such a
 * placeholder into its supertype is a separate minimality issue.
 */
public class CastAndContextHarmlessTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "castandcontextharmless",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
