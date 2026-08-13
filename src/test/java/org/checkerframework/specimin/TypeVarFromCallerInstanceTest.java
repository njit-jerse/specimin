package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A type variable of the calling method leaks into a synthetic <em>instance</em> method whose
 * declaring type has no type parameter at all.
 *
 * <p>This is the same defect as {@link StaticTypeVarFromCallerTest}, but on the branch where the
 * leaked name has nothing to bind to: {@code Holder} is not generic, so the {@code T} that Specimin
 * copies out of {@code Simple#target} is simply an unresolvable name in {@code Holder}'s body
 * rather than a static-context violation. Per JLS 6.3 a type variable's scope is the declaration
 * that binds it, so the name was never meaningful here; the differing diagnostic is incidental.
 *
 * <p>Because {@code Holder} has no type parameter, JLS 4.5.2 offers no substitution channel from
 * the call site to the signature, so the relationship between the argument's element type and the
 * return's can only be expressed by a type variable that the method itself declares.
 */
public class TypeVarFromCallerInstanceTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallerinstance",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Src<T>, Holder)"});
  }
}
