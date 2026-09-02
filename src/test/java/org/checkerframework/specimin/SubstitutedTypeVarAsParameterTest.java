package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CallOnSubstitutedTypeVarTest}, but the type variable appears in a parameter type, so
 * the substitution constrains an argument rather than supplying a receiver. By JLS 15.12.2 the
 * invocation {@code c.set(s.produce())} is applicable only if the type of {@code s.produce()} is
 * assignable to {@code T} under [T := Absent], i.e. to {@code Absent}. That is the only constraint
 * on the synthetic {@code Source#produce}, so its return type must be {@code Absent} -- {@code T}
 * names no type at the call site and would not compile.
 */
public class SubstitutedTypeVarAsParameterTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedtypevarasparameter",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Container<Absent>, Source)"});
  }
}
