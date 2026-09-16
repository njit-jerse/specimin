package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The target here is a method, {@code greet()}, not the field it dereferences: {@code greeting}
 * is a blank {@code static final} field, assigned in a static initializer block, that {@code
 * greet()} reads and immediately dereferences ({@code greeting.toUpperCase()}) in its own body.
 *
 * <p>This is the case for keeping the static-initializer-following logic ungated on {@code
 * --targetField}. If it were narrowed to fire only when the field itself is a target, this exact
 * program would regress: {@code greeting} would fall through to Slicer's ordinary repair and
 * become {@code private static final String greeting = null;}, while the target method {@code
 * greet()} -- which Specimin does promise to preserve faithfully -- would still read {@code
 * return greeting.toUpperCase();} verbatim. Running NullAway on that reduced program would then
 * report a nullness warning on that dereference, inside the target itself, that does not exist in
 * the original program, where {@code greeting} is always assigned a real value. The field is not
 * the target, but the dereference site is: it is a statement of {@code greet()}'s own body, so its
 * correctness is exactly what Specimin's fidelity guarantee for the target covers.
 */
public class NullAwayFieldReadByTargetMethodPreservedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runNullAwayTestWithoutJarPaths(
        "nullaway-field-read-by-target-method",
        new String[] {"org/example/app/Greeter.java", "org/example/app/GreetingFactory.java"},
        new String[] {"org.example.app.Greeter#greet()"});
  }
}
