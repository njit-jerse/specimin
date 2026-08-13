package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * When a synthetic <em>static</em> method's signature is derived from an argument whose type
 * mentions a type variable of the <em>calling</em> method, that type variable is not in scope in
 * the synthetic class, so the synthetic method must declare its own.
 *
 * <p>This is the other cause of the non-compiling output reported in <a
 * href="https://github.com/njit-jerse/specimin/issues/442">issue #442</a>. Here {@code
 * Simple#target} has a type variable {@code T} and passes a {@code Src<T>} to the static {@code
 * Box.from}. Specimin copies {@code T} verbatim into the generated signature; because {@code Box}
 * separately acquires a class-level type parameter also named {@code T} (from the {@code Box<T>}
 * return type at the use site), the {@code T} in {@code from}'s signature silently binds to the
 * class's type parameter, and javac rejects the output with "non-static type variable T cannot be
 * referenced from a static context".
 *
 * <p>In the issue's own input this shows up as {@code reactor.core.publisher.Flux} getting {@code
 * public static ReactorCorePublisherFluxFromReturnType from(Publisher<T> parameter0)}.
 *
 * <p>The expected output declares the type variable on the method. Any compilable signature that is
 * as precise would do — for instance naming it {@code T1} instead of shadowing the class's {@code
 * T}. What the expected output rules out is a signature that either does not compile or erases the
 * relationship between the argument's element type and the return's, since {@code Box<?>} would not
 * be assignable to the target's declared {@code Box<T>}.
 */
public class StaticTypeVarFromCallerTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "statictypevarfromcaller",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Src<T>)"});
  }
}
