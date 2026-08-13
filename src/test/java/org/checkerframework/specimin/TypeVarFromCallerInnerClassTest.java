package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A type variable of the enclosing class, reached from a non-static inner class, which the
 * synthetic method must declare.
 *
 * <p>This is the fixture that pins down {@link
 * org.checkerframework.specimin.JavaParserUtil#getReferenceableTypeParameterNames} in the direction
 * that can produce non-compiling output. {@code Inner} is not static, so JLS 8.1.2 does not forbid
 * naming {@code Simple}'s {@code C} inside it, and {@code C} genuinely appears in {@code target}'s
 * signature. A walk that treated any nested type as a static boundary would leave {@code C} out of
 * the referenceable set, {@code Box.from} would not declare it, and the output would fail with
 * "non- static type variable C cannot be referenced from a static context".
 *
 * <p>The other two fixtures added with this one ({@link TypeVarFromCallerStaticScopeTest} and
 * {@link TypeVarFromCallerInterfaceMemberTest}) cover the static cases, where the excluded names
 * are ones no type at the call site could mention anyway, so they can only guard against dropping a
 * type variable that is still usable.
 */
public class TypeVarFromCallerInnerClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallerinnerclass",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple.Inner#target(Src<C>)"});
  }
}
