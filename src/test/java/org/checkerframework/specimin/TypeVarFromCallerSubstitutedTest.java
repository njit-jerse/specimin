package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The case in which a calling method's type variable may legitimately stay in a synthetic
 * signature, and must not be rewritten.
 *
 * <p>{@code src.wrap()} has receiver type {@code Src<T>}, and the synthetic {@code Src} acquires a
 * class-level type parameter named after that very type variable. The {@code T} in {@code wrap}'s
 * return type is therefore not a stray copy of the caller's name: it is a substitution site. By JLS
 * 4.5.2 the type of a member of {@code Src<T>} is the member's type with the class's type parameter
 * replaced by the receiver's type argument, so the call yields {@code Box<T>} precisely because the
 * class type parameter is instantiated to the caller's {@code T}.
 *
 * <p>This is the negative half of the fix for {@link StaticTypeVarFromCallerTest}: a rule that
 * rewrote every caller type variable would emit {@code <T1> Box<T1> wrap()} here, which still
 * compiles but discards a relationship that the class type parameter was already carrying exactly.
 */
public class TypeVarFromCallerSubstitutedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallersubstituted",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Src<T>)"});
  }
}
