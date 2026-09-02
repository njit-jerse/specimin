package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a method call whose scope is a {@code NameExpr} denoting a type, rather
 * than a value, does not crash Specimin when it appears inside an anonymous class.
 *
 * <p>Such a scope does not resolve to a {@link
 * com.github.javaparser.resolution.declarations.ResolvedValueDeclaration}, so inside an anonymous
 * class Specimin retries the resolution by hoisting the name out of the class. That fallback ends
 * at {@code calculateResolvedType}, which reports the scope's <em>type</em> -- not an answer to
 * "what declaration does this name?", and not what a caller expecting a declaration can use.
 *
 * <p>The unsolved {@code Helper.normalize} call is what makes the crash reachable: without some
 * generated symbol in the slice, {@code Slicer#buildSlice} skips the post-processing pass that
 * inspects the {@code Integer.parseInt} call at all.
 */
public class StaticScopeResolvedAsTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "staticscoperesolvedastype",
        new String[] {"org/example/AtlasData.java"},
        new String[] {"org.example.AtlasData#load(Page)"});
  }
}
