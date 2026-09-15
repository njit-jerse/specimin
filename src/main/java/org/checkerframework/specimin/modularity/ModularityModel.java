package org.checkerframework.specimin.modularity;

import com.google.common.base.Ascii;

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
   * Factory for creating instances that implement this interface, given an input string. This
   * method throws if the input string isn't recognized.
   *
   * @param modularityModel an input from the user about which modularity to use
   * @return the corresponding modularity model
   */
  static ModularityModel createModularityModel(String modularityModel) {
    return switch (Ascii.toLowerCase(modularityModel)) {
      case "cf", "javac" -> new CheckerFrameworkModularityModel();
      case "nullaway" -> new NullAwayModularityModel();
      default ->
          throw new RuntimeException(
              "Unsupported modularity model. Options are: \"cf\", \"javac\", \"nullaway\"");
    };
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

  /**
   * Should a blank final field's static initializer block be followed as a dependency, instead of
   * letting the field fall through to Slicer's "empty final field" repair (which invents a default
   * value)? The javac/Checker Framework baseline does not observe a difference either way, since
   * neither one performs the kind of initialization-flow analysis that would notice a field's real
   * assigned value is missing. NullAway does perform that analysis, so a repaired default can
   * produce a spurious "field not initialized" warning that the original program never had.
   *
   * @return true if a blank final field's assigning static initializer block should be preserved;
   *     false if the field should be left to Slicer's repair
   */
  default boolean preserveStaticInitializerAssignments() {
    return false;
  }
}
