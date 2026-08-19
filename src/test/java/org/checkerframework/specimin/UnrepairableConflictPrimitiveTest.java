package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A conflict over a synthetic method's return type, seen from the site that cannot repair it. The
 * return type settles on {@code Payload}, so the {@code int} assignment is the one it does not
 * satisfy -- but that assignment's target is a primitive, and {@link
 * NonExtendableTargetPrimitiveTest} covers the case where it acts. Here the order is reversed, and
 * the {@code Payload} assignment is examined while the return type already is {@code Payload}: it
 * sees a target it satisfies and has nothing to complain about.
 *
 * <p>What breaks the tie is the {@code int} assignment noticing that the conflict it does see
 * cannot be fixed by making {@code Payload} a subtype of {@code int}. Nothing Specimin generated is
 * involved on that side, so there is no supertype to add and the fallback is the only move left.
 *
 * <p>This is the fixture that lets {@code UnsolvedSymbolGenerator#isNonExtendableType} treat every
 * non-extendable kind alike. A final class used to be answered for unconditionally, which papered
 * over this shape for {@code String} while leaving it broken for a primitive; with the repair
 * reachable from both directions, the exemption is unnecessary.
 */
public class UnrepairableConflictPrimitiveTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unrepairableconflictprimitive",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
