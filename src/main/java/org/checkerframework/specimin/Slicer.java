package org.checkerframework.specimin;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolAlternates;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolGenerator;

public class Slicer {
  /** The slice of nodes. */
  private final Set<Node> slice = new HashSet<>();

  /**
   * The slice of unsolved symbol definitions. These values need not be unique; the map is provided
   * for simple lookups when adding new symbols.
   */
  private final Map<String, UnsolvedSymbolAlternates<?>> unsolvedSlice = new HashMap<>();

  /** The Nodes that need to be processed in the main algorithm. */
  private final Queue<Node> worklist = new ArrayDeque<>();

  private final Map<String, Path> existingClassesToFilePath;
  private final String rootDirectory;
  private final Map<String, CompilationUnit> toSlice;

  public Slicer(
      Map<String, Path> existingClassesToFilePath,
      String rootDirectory,
      Map<String, CompilationUnit> toSlice) {
    this.existingClassesToFilePath = existingClassesToFilePath;
    this.rootDirectory = rootDirectory;
    this.toSlice = toSlice;
  }

  public void slice() throws IOException {
    Set<String> usedTypes = new HashSet<>();

    // Step 1: build the slice; see which nodes to keep
    while (!worklist.isEmpty()) {
      Node element = worklist.remove();
      handleElement(element, usedTypes);
    }

    // Step 2: add all used types
    for (String used : usedTypes) {
      toSlice.put(
          used, StaticJavaParser.parse(Path.of(rootDirectory, qualifiedNameToFilePath(used))));
    }

    // Step 3: remove all nodes except the ones marked for keeping OR package/import declarations
    for (CompilationUnit cu : toSlice.values()) {
      sliceNode(cu);
    }
  }

  public void addToWorklist(Node node) {
    worklist.add(node);
  }

  private void handleElement(Node node, Set<String> usedTypes) {
    if (slice.contains(node)) {
      return;
    }

    if (node instanceof Resolvable<?>) {
      Resolvable<?> asResolvable = (Resolvable<?>) node;

      try {
        Object resolved = asResolvable.resolve();

        worklist.addAll(TypeRuleDependencyMap.getRelevantElements(resolved));
        if (resolved instanceof ResolvedReferenceTypeDeclaration) {
          String qualifiedName = ((ResolvedReferenceTypeDeclaration) resolved).getQualifiedName();
          usedTypes.add(qualifiedName);
        }

        slice.add(node);
      } catch (UnsolvedSymbolException ex) {
        // Generate a synthetic type
      }
    } else {
      // If it's not a resolvable node, it's going to be:
      // 1) a non-resolvable expression (like a try {} block)
      // 2) a type parameter

      // It's guaranteed that these are relevant to the slice due to the algorithm.

      // We need to add this to the slice (or we potentially face missing
      // declarations or broken code). We also need to ask the type rule
      // dependency map if we can break this node down further.

      slice.add(node);
    }

    worklist.addAll(TypeRuleDependencyMap.getRelevantElements(node));
  }

  /**
   * Recursively removes all nodes not in the slice.
   *
   * @param node The node to slice
   */
  private void sliceNode(Node node) {
    if (slice.contains(node)) {
      for (Node child : node.getChildNodes()) {
        sliceNode(child);
      }
    } else if (!(node instanceof PackageDeclaration || node instanceof ImportDeclaration)) {
      node.remove();
    }
  }

  private String qualifiedNameToFilePath(String qualifiedName) {
    if (!existingClassesToFilePath.containsKey(qualifiedName)) {
      throw new RuntimeException(
          "qualifiedNameToFilePath only works for classes in the original directory");
    }
    Path absoluteFilePath = existingClassesToFilePath.get(qualifiedName);
    // theoretically rootDirectory should already be absolute as stated in README.
    Path absoluteRootDirectory = Paths.get(rootDirectory).toAbsolutePath();
    return absoluteRootDirectory.relativize(absoluteFilePath).toString();
  }
}
