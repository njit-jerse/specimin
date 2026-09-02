package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CallOnSubstitutedTypeVarTest}, but the type arguments come from a supertype rather
 * than from the receiver's own type. {@code AbsentContainer} is not generic; it inherits {@code
 * get} from {@code Container<Absent>}, and by JLS 8.4.8 the inherited method's signature is the one
 * produced by the substitution [T := Absent] recorded in the {@code extends} clause. So the type of
 * {@code c.get()} is {@code Absent}, not {@code T}.
 */
public class SubstitutedTypeVarFromSupertypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedtypevarfromsupertype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(AbsentContainer)"});
  }
}
