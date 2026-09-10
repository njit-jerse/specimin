package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * {@code Impl} declares two same-name, same-arity overloads whose parameter types have the same
 * simple name but live in different packages, and only one of the two packages is on the source
 * path. The unresolvable overload therefore matches {@code Rule#apply} only approximately, while
 * the other matches exactly.
 *
 * <p>Specimin used to preserve whichever of the two it happened to visit first: {@link
 * com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration#getDeclaredMethods}
 * returns a set, and the resolved declaration is rebuilt on each call, so the iteration order
 * follows fresh identity hash codes and varies from one call to the next. That made the output
 * non-deterministic, and it let {@code apply(com.other.b.Foo)} -- which overrides nothing, since
 * JLS 8.4.2 compares erasures and {@code com.other.b.Foo} is not {@code com.example.a.Foo} -- be
 * preserved in place of the real implementation, dragging a synthetic {@code com.other.b.Foo} into
 * the output with it.
 *
 * <p>The expected output keeps only {@code apply(com.example.a.Foo)}, which JLS 8.1.1.1 requires a
 * non-abstract {@code Impl} to declare, and no {@code com.other.b} package at all.
 */
public class SameSimpleNameOverloadTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "samesimplenameoverload",
        new String[] {"com/example/Target.java"},
        new String[] {"com.example.Target#target(Foo)"});
  }
}
