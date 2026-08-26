package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a {@code ::new} reference to an already-generated synthetic constructor
 * does not acquire a leading receiver parameter. An unbound reference to an <em>instance
 * method</em> takes the receiver as an extra first parameter, but a constructor reference does not:
 * its scope names the type being instantiated rather than a receiver, so the functional interface's
 * parameters are the constructor's one for one (JLS 15.13.1). Applying the instance-method
 * adjustment to a constructor invents a functional interface of the wrong arity, and Specimin then
 * cannot find the method it just generated for {@code register}.
 *
 * <p>Specimin's choice of a no-argument {@code Supplier} here, rather than reusing the {@code
 * Widget(String)} constructor established on the line above, is imprecise but compilable: nothing
 * in the input says what shape {@code Registry.register} expects.
 */
public class ConstructorRefAlreadyGeneratedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "constructorrefalreadygenerated",
        new String[] {"example/Target.java"},
        new String[] {"example.Target#target()"});
  }
}
