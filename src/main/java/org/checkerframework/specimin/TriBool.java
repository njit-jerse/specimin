package org.checkerframework.specimin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A boolean with a third value, {@link #MAYBE}, for a question whose answer Specimin cannot always
 * determine. This is Kleene's strong three-valued logic: {@link #and} is that logic's conjunction,
 * and the constants are declared in its truth order, {@code FALSE < MAYBE < TRUE}.
 *
 * <p>In the logic, {@code MAYBE} means only that the answer is unknown, and what to do about that
 * is up to the caller. However, every use of this type inside Specimin at the time it was written
 * answered a question of the form "might this declaration be needed?", and treated {@code MAYBE}
 * like {@code TRUE} when deciding what to keep. That is because keeping a declaration that turns
 * out to be unnecessary costs only minimality, but discarding one that turns out to be necessary
 * makes the output fail to compile. {@code MAYBE} still ranks below {@code TRUE}, so that a caller
 * choosing among candidates can prefer the ones that are definitely right; see {@link
 * #selectTruest}.
 */
public enum TriBool {
  /** Definitely false. */
  FALSE,
  /** Either true or false; whatever analysis produced the value cannot tell which. */
  MAYBE,
  /** Definitely true. */
  TRUE;

  /**
   * Converts a boolean.
   *
   * @param b the boolean
   * @return {@code TRUE} if {@code b} is true, {@code FALSE} otherwise
   */
  public static TriBool of(boolean b) {
    return b ? TRUE : FALSE;
  }

  /**
   * Returns the conjunction of this value and another: whichever of the two is less true.
   *
   * @param other the other value
   * @return the conjunction of this value and {@code other}
   */
  public TriBool and(TriBool other) {
    return compareTo(other) <= 0 ? this : other;
  }

  /**
   * Some items, together with the value of a predicate that they all share.
   *
   * @param items the items
   * @param value the value of the predicate for every item in {@code items}; {@code FALSE} if and
   *     only if {@code items} is empty
   * @param <T> the type of the items
   */
  public record Selection<T>(List<T> items, TriBool value) {
    /**
     * Returns the selection that contains nothing.
     *
     * @param <T> the type of the items
     * @return an empty selection
     */
    public static <T> Selection<T> empty() {
      return new Selection<>(List.of(), FALSE);
    }
  }

  /**
   * Selects the items for which a predicate is truest: every item for which it is {@code TRUE}, or
   * if there are none, every item for which it is {@code MAYBE}. The order of {@code items} is
   * preserved.
   *
   * @param items the items to select from
   * @param predicate the predicate
   * @param <T> the type of the items
   * @return the selected items, and the value of {@code predicate} for them
   */
  public static <T> Selection<T> selectTruest(
      List<? extends T> items, Function<? super T, TriBool> predicate) {
    List<T> definitely = new ArrayList<>();
    List<T> possibly = new ArrayList<>();

    for (T item : items) {
      switch (predicate.apply(item)) {
        case TRUE -> definitely.add(item);
        case MAYBE -> possibly.add(item);
        case FALSE -> {}
      }
    }

    if (!definitely.isEmpty()) {
      return new Selection<>(definitely, TRUE);
    }
    if (!possibly.isEmpty()) {
      return new Selection<>(possibly, MAYBE);
    }
    return Selection.empty();
  }
}
