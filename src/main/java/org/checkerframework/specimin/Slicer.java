package org.checkerframework.specimin;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.specimin.unsolved.AddInformationResult;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolAlternates;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolGenerator;

public class Slicer {
  /**
   * The slice of nodes.
   *
   * <p>We cannot use HashSet<Node> here; when nodes' children are modified, {@code slice.contains}
   * no longer returns true, even if {@code equals} evaluates to true, {@code hashCode} returns the
   * same value, and {@code ==} yields the same reference.
   *
   * <p>An {@code IdentityHashMap} is safe to use because we use the same compilation units
   * everywhere, so all corresponding nodes will have the same reference.
   */
  private final Set<Node> slice = Collections.newSetFromMap(new IdentityHashMap<>());

  /** The slice of generated symbols. */
  private final Set<UnsolvedSymbolAlternates<?>> generatedSymbolSlice = new HashSet<>();

  /** The Nodes that need to be processed in the main algorithm. */
  private final Deque<Node> worklist = new ArrayDeque<>();

  private final Set<Node> postProcessingWorklist = new HashSet<>();

  private final Map<String, Path> existingClassesToFilePath;
  private final String rootDirectory;
  private final Map<String, CompilationUnit> toSlice;

  private final UnsolvedSymbolGenerator unsolvedSymbolGenerator;
  private final TypeRuleDependencyMap typeRuleDependencyMap;

  public Slicer(
      Map<String, Path> existingClassesToFilePath,
      String rootDirectory,
      Map<String, CompilationUnit> toSlice,
      TypeRuleDependencyMap typeRuleDependencyMap,
      UnsolvedSymbolGenerator unsolvedSymbolGenerator) {
    this.existingClassesToFilePath = existingClassesToFilePath;
    this.rootDirectory = rootDirectory;
    this.toSlice = toSlice;
    this.typeRuleDependencyMap = typeRuleDependencyMap;
    this.unsolvedSymbolGenerator = unsolvedSymbolGenerator;
  }

  public void slice() throws IOException {
    Set<CompilationUnit> usedCompilationUnits = new HashSet<>();

    // Step 1: build the slice; see which nodes to keep
    while (!worklist.isEmpty()) {
      Node element = worklist.remove();
      handleElement(element, usedCompilationUnits);
    }

    if (!generatedSymbolSlice.isEmpty()) {
      // Step 2: Handle the post-processing worklist
      Iterator<Node> ppwIterator = postProcessingWorklist.iterator();
      while (ppwIterator.hasNext()) {
        Node element = ppwIterator.next();
        AddInformationResult result = unsolvedSymbolGenerator.addInformation(element);
        generatedSymbolSlice.addAll(result.toAdd());
        generatedSymbolSlice.removeAll(result.toRemove());
      }
    }

    // Step 3: add all used compilation units
    for (CompilationUnit used : usedCompilationUnits) {
      toSlice.put(
          qualifiedNameToFilePath(used.getPrimaryType().get().getFullyQualifiedName().get())
              .toString(),
          used);
    }

    // Step 4: remove all nodes except the ones marked for preservation OR package/import
    // declarations
    for (CompilationUnit cu : toSlice.values()) {
      sliceCompilationUnit(cu);
    }
  }

  public Set<UnsolvedSymbolAlternates<?>> getGeneratedSymbolSlice() {
    return generatedSymbolSlice;
  }

  public void addToWorklist(Node node) {
    worklist.add(node);
  }

  private void handleElement(Node node, Set<CompilationUnit> usedCompilationUnits) {
    if (slice.contains(node)) {
      return;
    }

    if (node instanceof Resolvable<?>) {
      Resolvable<?> asResolvable = (Resolvable<?>) node;

      try {
        Object resolved = asResolvable.resolve();

        if (resolved == null) {
          throw new RuntimeException("Unexpected null value in resolve() call");
        }

        List<Node> toAddToWorklist = typeRuleDependencyMap.getRelevantElements(resolved);
        addToBeginningOfWorklist(toAddToWorklist);

        // Since resolved declarations may reference another file, we need to add that compilation
        // unit to the output
        usedCompilationUnits.addAll(
            toAddToWorklist.stream().map(n -> n.findCompilationUnit().get()).toList());
      } catch (UnsolvedSymbolException ex) {
        // Generate a synthetic type
        generatedSymbolSlice.addAll(unsolvedSymbolGenerator.inferContext(node));
      }
    }

    slice.add(node);
    addToBeginningOfWorklist(typeRuleDependencyMap.getRelevantElements(node));

    if (unsolvedSymbolGenerator.needToPostProcess(node)) {
      postProcessingWorklist.add(node);
    }
  }

  /**
   * Adds all nodes to the beginning of the worklist. This ensures that synthetic types are
   * generated before they are used. Use instead of worklist.addAll.
   *
   * @param c The list of nodes.
   */
  private void addToBeginningOfWorklist(List<Node> c) {
    for (int i = c.size(); i > 0; i--) {
      worklist.addFirst(c.get(i - 1));
    }
  }

  /**
   * Starts the slice on a compilation unit. Use this instead of {@link #sliceNode(Node)} because
   * {@link CompilationUnit} has some quirks that prevents {@link CompilationUnit#getChildNodes()}
   * from accessing anything but package/import declarations.
   *
   * @param cu The compilation unit
   */
  private void sliceCompilationUnit(CompilationUnit cu) {
    List<TypeDeclaration<?>> typesCopy = new ArrayList<>(cu.getTypes());
    for (TypeDeclaration<?> typeDecl : typesCopy) {
      sliceNode(typeDecl);
    }
  }

  /**
   * Recursively removes all nodes not in the slice.
   *
   * @param node The node to slice
   */
  private void sliceNode(Node node) {
    if (slice.contains(node)) {
      List<Node> copy = new ArrayList<>(node.getChildNodes());
      for (Node child : copy) {
        sliceNode(child);
      }
    }
    // If a BlockStmt is being removed, it's a method/constructor to be trimmed
    else if (node instanceof BlockStmt blockStmt) {
      TypeDeclaration<?> enclosing = JavaParserUtil.getEnclosingClassLike(node);

      if (enclosing.isClassOrInterfaceDeclaration()
          && enclosing.asClassOrInterfaceDeclaration().isInterface()) {
        node.remove();
      } else {
        blockStmt.setStatements(
            new NodeList<>(StaticJavaParser.parseStatement("throw new java.lang.Error();")));
      }
    } else {
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
    return absoluteRootDirectory.relativize(absoluteFilePath).toString().replace('\\', '/');
  }
}
