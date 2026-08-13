package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A synthetic instance method whose declaring type <em>does</em> have a type parameter of the
 * leaked name, but where the receiver at this call site instantiates that type parameter to
 * something else.
 *
 * <p>This is the case that makes "the declaring type has a type parameter of this name, and the
 * member is not static" too weak a test. Here {@code src} has type {@code Src<String>}, so by JLS
 * 4.5.2 the type of {@code put} as a member of {@code Src<String>} is obtained by substituting
 * {@code String} for the class's type parameter. Leaving the caller's {@code T} in the signature
 * therefore does not mean "the argument's type" -- it means {@code String} -- and the call {@code
 * src.put(item)} passing a {@code T} does not compile.
 *
 * <p>The collision here does not come from Specimin preferring the call site's type variable name
 * for the class's type parameter: {@code Src<String>} contributes no type variable name, and the
 * class's parameter is named {@code T} simply because that is the first generated name. So the
 * clash arises from both sources independently, and a fix must key on whether the caller's type
 * variable actually occupies that position in the receiver's type, not on the name.
 *
 * <p>The call's result is discarded, so {@code put}'s return type is the invented placeholder
 * {@code PutReturnType}. That is unrelated to what this fixture pins down, but it is deliberate:
 * constraining the return instead (by assigning the call to a {@code String}) makes Specimin give
 * {@code put} an unconstrained type variable return type, which then captures the leaked parameter
 * name by coincidence and hides the defect under test.
 */
public class TypeVarFromCallerMismatchTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallermismatch",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Src<String>, T)"});
  }
}
