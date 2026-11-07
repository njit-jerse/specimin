package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test is the companion to {@link UnsolvedFieldInitializerTest}: there, the unresolvable type
 * in a field initializer appears nowhere else, so no symbol is ever generated for it; here, the
 * field's declared type names the same unresolvable type, so a symbol for it does exist by the time
 * the initializer is post-processed.
 *
 * <p>Both cases discard the initializer, because no typing judgment about the target method reads
 * one (JLS 8.3 gives the expression {@code defaults} the field's <em>declared</em> type), and the
 * final field then takes the default initializer JLS 16.8 definite assignment requires. This case
 * pins that discarding it does not otherwise change the output: {@code ArrayMap} must still be
 * declared with two type parameters, because JLS 4.5 only admits {@code ArrayMap<String, String>}
 * as a type if {@code ArrayMap} is generic with that arity. Those parameters stay unconstrained,
 * since the only use of the field binds {@code println(Object)} (JLS 15.12.2), which constrains
 * nothing. In particular {@code ArrayMap} gets no supertype here -- contrast {@link
 * UnsolvedFieldInitializerTest}, where a {@code java.util.Map} declared type would impose one.
 */
public class UnsolvedFieldInitializerDeclaredTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedfieldinitializerdeclaredtype",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#build(Unsolved)"});
  }
}
