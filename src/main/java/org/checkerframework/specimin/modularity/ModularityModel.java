package org.checkerframework.specimin.modularity;

/**
 * This interface represents the differences between modularity models. A single instance of a class
 * that implements this one represents a particular modularity model for an analysis.
 *
 * <p>The current implementation of this interface is that the "baseline" modularity model is the
 * javac typechecker or the Checker Framework, and this class only represents the **differences**
 * between that model and the model of other analyses. In the future, this class may become absolute
 * rather than relative.
 */
public interface ModularityModel {

  /**
   * Factory for creating instances that implement this inferface, given an input string. This
   * method throws if the input string isn't recognized.
   *
   * @param modularityModel an input from the user about which modularity to use
   * @return the corresponding modularity model
   */
  public static ModularityModel createModularityModel(String modularityModel) {
    switch (modularityModel) {
      case "cf":
      case "javac":
        return new CheckerFrameworkModularityModel();
      case "nullaway":
        return new NullAwayModularityModel();
      default:
        throw new RuntimeException(
            "Unsupported modularity model. Options are: \"cf\", \"javac\", \"nullaway\"");
    }
  }

  /**
   * Should the modularity model include all fields of a class, if that class' constructor is the
   * target? NullAway can warn about the _lack_ of an assignment to a field in a constructor, so its
   * modularity model implicitly includes all fields when analyzing a constructor.
   *
   * @return true if all fields should be preserved; false if only used fields should be preserved
   */
  default boolean preserveAllFieldsIfTargetIsConstructor() {
    return false;
  }
}
