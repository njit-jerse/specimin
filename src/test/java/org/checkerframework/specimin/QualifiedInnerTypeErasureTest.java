package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * {@code Impl#apply} has one parameter whose type is not on the source path, so Specimin cannot
 * compute its signature and compares it against {@code Rule#apply} by {@link
 * JavaParserUtil#areMethodsLikelyEqual} instead. That comparison erases the type arguments of the
 * other parameter, whose type {@code Outer<String>.Inner<Integer>} carries two type argument lists:
 * one for the enclosing type and one for the member type (JLS 4.5).
 *
 * <p>Erasing everything between the first {@code <} and the last {@code >} deletes {@code Inner}
 * along with the arguments, so the two sides of the comparison disagree and {@code Rule#apply} is
 * not recognized as a method that {@code Impl#apply} overrides. Specimin then drops it, which
 * leaves {@code Rule} with no abstract method and the lambda in the target with nothing to
 * implement: "Rule is not a functional interface" (JLS 9.8).
 */
public class QualifiedInnerTypeErasureTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "qualifiedinnertypeerasure",
        new String[] {"com/example/Target.java"},
        new String[] {"com.example.Target#target(Outer<String>.Inner<Integer>, Missing)"});
  }
}
