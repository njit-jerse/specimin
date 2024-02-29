package org.checkerframework.specimin;

import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A simple class to keep track of the progress of UnsolvedSymbolVisitor. */
public class UnsolvedSymbolVisitorProgress {

  /**
   * Fields and methods that could be called inside the target methods. We call them potential-used
   * because the usage check is simply based on the simple names of those members.
   */
  private Set<String> potentialUsedMembers;

  /** New files that should be added to the list of target files for the next iteration. */
  private Set<String> addedTargetFiles;

  /**
   * A set containing synthetic versions of used classes that are not present in the source code.
   * These synthetic versions are created by the UnsolvedSymbolVisitor.
   */
  private Set<String> createdSyntheticClass;

  /**
   * Constructs a new instance of UnsolvedSymbolVisitorProgress.
   *
   * @param potentialUsedMembers A set of potential-used members.
   * @param addedTargetFiles A set of new files to be added as target files.
   * @param createdSyntheticClass A set of synthetic classes created.
   */
  public UnsolvedSymbolVisitorProgress(
      Set<String> potentialUsedMembers,
      Set<String> addedTargetFiles,
      Set<String> createdSyntheticClass) {
    this.potentialUsedMembers = potentialUsedMembers;
    this.addedTargetFiles = addedTargetFiles;
    this.createdSyntheticClass = createdSyntheticClass;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof UnsolvedSymbolVisitorProgress)) {
      return false;
    }
    UnsolvedSymbolVisitorProgress other = (UnsolvedSymbolVisitorProgress) obj;
    return potentialUsedMembers.equals(other.potentialUsedMembers)
        && addedTargetFiles.equals(other.addedTargetFiles)
        && createdSyntheticClass.equals(other.createdSyntheticClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(potentialUsedMembers, addedTargetFiles, createdSyntheticClass);
  }
}
