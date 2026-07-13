package org.checkerframework.specimin.unsolved;

/**
 * The "sealedness" of a synthetic type; i.e., the keyword that goes in front of the class
 * declaration.
 *
 * <p>For example, {@link Sealedness#NON_SEALED} corresponds with {@code non-sealed class Foo}.
 */
public enum Sealedness {
  /** No sealedness --> {@code class Foo} */
  NONE,
  /** Non-sealed --> {@code non-sealed class Foo} */
  NON_SEALED,
  /** Sealed --> {@code sealed class Foo} */
  SEALED,
  /** Final --> {@code final class Foo} */
  FINAL
}
