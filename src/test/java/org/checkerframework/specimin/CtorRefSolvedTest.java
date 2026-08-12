package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a constructor reference to a type with several constructors preserves all
 * of them.
 *
 * <p>JavaParser cannot resolve a constructor reference at all, so Specimin recovers by looking up
 * the candidates on the scope's type (see {@link CtorRefUnsolvedTest}). Here that lookup is
 * ambiguous: choosing between {@code Foo(String)} and {@code Foo()} needs the target functional
 * interface, which is what was unavailable in the first place. Dropping the ambiguous candidates
 * would delete both -- nothing preserves them, since resolution failed, and nothing synthesizes a
 * replacement either, since a reference with candidates is not an unsolved symbol -- and {@code
 * Foo::new} would have no constructor to bind to. This test requires that both candidate
 * constructors are kept but that {@code Foo#unused()} is still pruned.
 */
public class CtorRefSolvedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "ctorrefsolved",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
