package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that when extending a JDK class with only constructors that take more than zero
 * arguments, at least one constructor gets preserved so that the result compiles.
 *
 * <p>I tried to write an equivalent of the {@link NoZeroArgCtorIntTest} using a JDK class, but I
 * couldn't find a suitable JDK class to extend that only has constructors that take primitives. I
 * considered the following unsuitable JDK classes: Number (has a zero arg ctor), Integer (final),
 * Date (zero arg ctor again), BigInteger (has a ctor that takes a String). Then, I gave up and
 * decided to only deal with that situation if we encounter it in practice.
 */
public class NoZeroArgCtorJDKTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nozeroargctorjdk",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
