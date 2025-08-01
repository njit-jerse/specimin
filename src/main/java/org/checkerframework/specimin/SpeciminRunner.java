package org.checkerframework.specimin;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.specimin.modularity.ModularityModel;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolEnumerator;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolEnumeratorResult;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolGenerator;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

/** This class is the main runner for Specimin. Use its main() method to start Specimin. */
public class SpeciminRunner {

  /**
   * The main entry point for Specimin.
   *
   * @param args the arguments to Specimin
   * @throws IOException if there is an exception
   */
  public static void main(String... args) throws IOException {
    OptionParser optionParser = new OptionParser();
    // This option is the root of the source directory of the target files. It is used
    // for symbol resolution from source code and to organize the output directory.
    OptionSpec<String> rootOption = optionParser.accepts("root").withRequiredArg();

    var jar = optionParser.accepts("jarPath").withOptionalArg().ofType(String.class);

    // This option is the relative paths to the target file(s) - the .java file(s) containing
    // target method(s) - from the root.
    OptionSpec<String> targetFilesOption = optionParser.accepts("targetFile").withRequiredArg();

    // This option is the target methods, specified in the format
    // class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...)
    OptionSpec<String> targetMethodsOption = optionParser.accepts("targetMethod").withRequiredArg();

    // This option is the target fields, specified in the format
    // class.fully.qualified.Name#fieldName
    OptionSpec<String> targetFieldsOptions = optionParser.accepts("targetField").withRequiredArg();

    // The directory in which to output the results.
    OptionSpec<String> outputDirectoryOption =
        optionParser.accepts("outputDirectory").withRequiredArg();

    // This option determines how ambiguities are to be resolved.
    // Accepts the arguments: "best-effort", "all", "input-condition"
    OptionSpec<String> ambiguityResolutionPolicy =
        optionParser
            .accepts("ambiguityResolutionPolicy")
            .withOptionalArg()
            .defaultsTo("best-effort");

    // the model for the javac type system, which is shared by the Checker Framework.
    // Accepts the arguments: "javac", "cf", "nullaway"
    OptionSpec<String> modularityModelOption =
        optionParser.accepts("modularityModel").withOptionalArg().defaultsTo("cf");

    OptionSet options = optionParser.parse(args);

    String jarDirectory = options.valueOf(jar);
    List<String> jarFiles = new ArrayList<>();
    if (jarDirectory != null) {
      jarFiles = getJarFiles(jarDirectory);
    }

    performMinimization(
        options.valueOf(rootOption),
        options.valuesOf(targetFilesOption),
        jarFiles,
        options.valuesOf(targetMethodsOption),
        options.valuesOf(targetFieldsOptions),
        options.valueOf(outputDirectoryOption),
        options.valueOf(ambiguityResolutionPolicy),
        options.valueOf(modularityModelOption));
  }

  /**
   * This method acts as an API for users who want to incorporate Specimin as a library into their
   * projects. It offers an easy way to do the minimization job without needing to directly call
   * Specimin's main method.
   *
   * @param root The root directory of the input files.
   * @param targetFiles A list of files that contain the target methods.
   * @param jarPaths Paths to relevant JAR files.
   * @param targetMethodNames A set of target method names to be preserved.
   * @param targetFieldNames A set of target field names to be preserved.
   * @param outputDirectory The directory for the output.
   * @throws IOException if there is an exception
   */
  public static void performMinimization(
      String root,
      List<String> targetFiles,
      List<String> jarPaths,
      List<String> targetMethodNames,
      List<String> targetFieldNames,
      String outputDirectory)
      throws IOException {
    performMinimization(
        root,
        targetFiles,
        jarPaths,
        targetMethodNames,
        targetFieldNames,
        outputDirectory,
        "best-effort",
        "cf");
  }

  /**
   * This method acts as an API for users who want to incorporate Specimin as a library into their
   * projects. It offers an easy way to do the minimization job without needing to directly call
   * Specimin's main method.
   *
   * @param root The root directory of the input files.
   * @param targetFiles A list of files that contain the target methods.
   * @param jarPaths Paths to relevant JAR files.
   * @param targetMethodNames A set of target method names to be preserved.
   * @param targetFieldNames A set of target field names to be preserved.
   * @param outputDirectory The directory for the output.
   * @param ambiguityResolutionPolicy The ambiguity resolution policy to use.
   * @param modularityModelCode The modularity model to use.
   * @throws IOException if there is an exception
   */
  public static void performMinimization(
      String root,
      List<String> targetFiles,
      List<String> jarPaths,
      List<String> targetMethodNames,
      List<String> targetFieldNames,
      String outputDirectory,
      String ambiguityResolutionPolicy,
      String modularityModelCode)
      throws IOException {
    // The set of path of files that have been created by Specimin. We must be careful to delete all
    // those files in the end, because otherwise they can pollute the input directory. To do that,
    // we need to register a shutdown hook with the JVM.
    Set<Path> createdClass = new HashSet<>();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                deleteFiles(createdClass);
              }
            });

    AmbiguityResolutionPolicy policy = AmbiguityResolutionPolicy.parse(ambiguityResolutionPolicy);
    ModularityModel model = ModularityModel.createModularityModel(modularityModelCode);

    performMinimizationImpl(
        root,
        targetFiles,
        jarPaths,
        targetMethodNames,
        targetFieldNames,
        outputDirectory,
        policy,
        model,
        createdClass);
  }

  /**
   * Helper method for performMinimization. The logic of performMinimization is here;
   * performMinimization itself wraps this in a try-finally to ensure that all created files are
   * cleaned up properly in the event of a crash or interrupt.
   *
   * @param root The root directory of the input files.
   * @param targetFiles A list of files that contain the target methods.
   * @param jarPaths Paths to relevant JAR files.
   * @param targetMethodNames A set of target method names to be preserved.
   * @param targetFieldNames A set of target field names to be preserved.
   * @param outputDirectory The directory for the output.
   * @param ambiguityResolutionPolicy The ambiguity resolution policy.
   * @param modularityModel The modularity model.
   * @throws IOException if there is an exception
   */
  private static void performMinimizationImpl(
      String root,
      List<String> targetFiles,
      List<String> jarPaths,
      List<String> targetMethodNames,
      List<String> targetFieldNames,
      String outputDirectory,
      AmbiguityResolutionPolicy ambiguityResolutionPolicy,
      ModularityModel modularityModel,
      Set<Path> createdClass)
      throws IOException {
    // To facilitate string manipulation in subsequent methods, ensure that 'root' ends with a
    // trailing slash.
    if (!root.endsWith("/")) {
      root = root + "/";
    }

    ParserConfiguration config = updateStaticSolver(root, jarPaths);
    decompileJarFiles(root, jarPaths, createdClass);

    SourceRoot sourceRoot = new SourceRoot(Path.of(root));
    sourceRoot.setParserConfiguration(config);
    sourceRoot.tryToParse();

    // the set of Java classes in the original codebase mapped with their corresponding Java files.
    Map<String, Path> existingClassesToFilePath = new HashMap<>();
    Map<String, CompilationUnit> fqnToCompilationUnits = new HashMap<>();

    // Keys are paths to files, values are parsed ASTs
    Map<String, CompilationUnit> parsedTargetFiles = new HashMap<>();

    // getCompilationUnits does not seem to include all files, causing some to be deleted
    for (ParseResult<CompilationUnit> res : sourceRoot.getCache()) {
      CompilationUnit compilationUnit =
          res.getResult().orElseThrow(() -> new RuntimeException(res.getProblems().toString()));
      Path pathOfCurrentJavaFile =
          compilationUnit.getStorage().get().getPath().toAbsolutePath().normalize();

      for (String targetFile : targetFiles) {
        if (Path.of(root, targetFile).equals(pathOfCurrentJavaFile)) {
          parsedTargetFiles.put(targetFile.replace('\\', '/'), compilationUnit);
        }
      }

      for (TypeDeclaration<?> declaredClass : compilationUnit.findAll(TypeDeclaration.class)) {
        if (declaredClass.getFullyQualifiedName().isPresent()) {
          String declaredClassQualifiedName = declaredClass.getFullyQualifiedName().get();
          existingClassesToFilePath.put(declaredClassQualifiedName, pathOfCurrentJavaFile);
          fqnToCompilationUnits.put(declaredClassQualifiedName, compilationUnit);
        }
      }
    }

    createdClass.addAll(getPathsFromJarPaths(root, jarPaths));

    Deque<Node> worklist = new ArrayDeque<>();

    TargetMemberFinderVisitor finder =
        new TargetMemberFinderVisitor(
            targetMethodNames, targetFieldNames, worklist, modularityModel);

    for (CompilationUnit cu : parsedTargetFiles.values()) {
      cu.accept(finder, null);
    }

    Map<String, Set<String>> unfoundMethods = finder.getUnfoundMethods();
    if (!unfoundMethods.isEmpty()) {
      throw new RuntimeException(
          "Specimin could not locate the following target methods in the target files:\n"
              + unfoundMembersTable(unfoundMethods, true));
    }

    Map<String, Set<String>> unfoundFields = finder.getUnfoundFields();
    if (!unfoundFields.isEmpty()) {
      throw new RuntimeException(
          "Specimin could not locate the following target fields in the target files:\n"
              + unfoundMembersTable(unfoundFields, false));
    }

    UnsolvedSymbolGenerator unsolvedSymbolGenerator =
        new UnsolvedSymbolGenerator(fqnToCompilationUnits);
    Slicer.SliceResult sliceResult =
        Slicer.slice(
            new StandardTypeRuleDependencyMap(fqnToCompilationUnits),
            worklist,
            unsolvedSymbolGenerator);

    // cache to avoid called Files.createDirectories repeatedly with the same arguments
    Set<Path> createdDirectories = new HashSet<>();
    Set<String> targetFilesAbsolutePaths = new HashSet<>();

    for (String target : targetFiles) {
      File targetFile = new File(target);
      // Convert to absolute path for comparison
      targetFilesAbsolutePaths.add(targetFile.getAbsolutePath());
    }

    UnsolvedSymbolEnumerator alternateOutput =
        new UnsolvedSymbolEnumerator(sliceResult.generatedSymbolSlice());
    UnsolvedSymbolEnumeratorResult enumeratorResult =
        alternateOutput.getBestEffort(sliceResult.generatedSymbolDependentSlice());

    handleUnsolvedSymbolEnumeratorResult(
        sliceResult,
        enumeratorResult,
        existingClassesToFilePath,
        root,
        targetFilesAbsolutePaths,
        outputDirectory,
        createdDirectories);
  }

  /**
   * Handles a result from an iteration of {@link UnsolvedSymbolEnumerator}. This outputs the files
   * for both solved and unsolved symbols.
   *
   * @param sliceResult The result of the slice
   * @param enumeratorResult The iteration of the UnsolvedSymbolEnumerator
   * @param existingClassesToFilePath A map of existing classes to their files paths
   * @param root The root directory
   * @param targetFilesAbsolutePaths The target files as absolute paths
   * @param outputDirectory The output directory
   * @param createdDirectories A cache of created directories
   */
  private static void handleUnsolvedSymbolEnumeratorResult(
      Slicer.SliceResult sliceResult,
      UnsolvedSymbolEnumeratorResult enumeratorResult,
      Map<String, Path> existingClassesToFilePath,
      String root,
      Set<String> targetFilesAbsolutePaths,
      String outputDirectory,
      Set<Path> createdDirectories)
      throws IOException {
    for (CompilationUnit original : sliceResult.solvedSlice()) {
      if (isEmptyCompilationUnit(original)) {
        continue;
      }

      // Generally, this set will be small, so we'll check if we need to clone at all
      // to prevent the cloning process from happening when it's not necessary
      boolean shouldClone = false;
      for (Node node : enumeratorResult.unusedDependentNodes()) {
        if (node.findCompilationUnit().get().equals(original)) {
          shouldClone = true;
          break;
        }
      }

      CompilationUnit cu = original;
      if (shouldClone) {
        cu = original.clone();

        IdentityHashMap<Node, Node> map = new IdentityHashMap<>();
        mapNodes(original, cu, map);

        for (Node toRemove : enumeratorResult.unusedDependentNodes()) {
          Node clone = map.get(toRemove);

          if (clone == null) {
            continue;
          }
          clone.remove();
        }
      }

      String path =
          qualifiedNameToFilePath(
              cu.getPrimaryType().get().getFullyQualifiedName().get(),
              existingClassesToFilePath,
              root);

      // ignore classes from the Java package, unless we are targeting a JDK file.
      // However, all related java/ files should not be included (as in used, but not targeted)
      String absolutePath = new File(path).getAbsolutePath();
      if (!targetFilesAbsolutePaths.contains(absolutePath)
          && (path.startsWith("java/") || path.startsWith("java\\"))) {
        continue;
      }
      Path targetOutputPath = Path.of(outputDirectory, path);
      // Create any parts of the directory structure that don't already exist.
      Path dirContainingOutputFile = targetOutputPath.getParent();
      // This null test is very defensive and might not be required? I think getParent can
      // only return null if its input was a single element path, which targetOutputPath
      // should not be unless the user made an error.
      if (dirContainingOutputFile != null
          && !createdDirectories.contains(dirContainingOutputFile)) {
        Files.createDirectories(dirContainingOutputFile);
        createdDirectories.add(dirContainingOutputFile);
      }
      // Write the string representation of CompilationUnit to the file
      try {
        PrintWriter writer = new PrintWriter(targetOutputPath.toFile(), StandardCharsets.UTF_8);
        writer.print(getCompilationUnitWithCommentsTrimmed(cu));
        writer.close();
      } catch (IOException e) {
        System.out.println("failed to write output file " + targetOutputPath);
        System.out.println("with error: " + e);
      }
    }

    for (Entry<String, String> alternate : enumeratorResult.classNamesToFileContent().entrySet()) {
      Path targetOutputPath =
          Path.of(outputDirectory, alternate.getKey().replace('.', '/') + ".java");
      // Create any parts of the directory structure that don't already exist.
      Path dirContainingOutputFile = targetOutputPath.getParent();
      // This null test is very defensive and might not be required? I think getParent can
      // only return null if its input was a single element path, which targetOutputPath
      // should not be unless the user made an error.
      if (dirContainingOutputFile != null
          && !createdDirectories.contains(dirContainingOutputFile)) {
        Files.createDirectories(dirContainingOutputFile);
        createdDirectories.add(dirContainingOutputFile);
      }
      // Write the string representation of CompilationUnit to the file
      try {
        PrintWriter writer = new PrintWriter(targetOutputPath.toFile(), StandardCharsets.UTF_8);
        writer.print(alternate.getValue());
        writer.close();
      } catch (IOException e) {
        System.out.println("failed to write output file " + targetOutputPath);
        System.out.println("with error: " + e);
      }
    }
  }

  /**
   * Creates a map of original nodes to cloned nodes.
   *
   * @param original The original node
   * @param clone The cloned node
   * @param map The final mapping
   */
  private static void mapNodes(Node original, Node clone, IdentityHashMap<Node, Node> map) {
    map.put(original, clone);

    List<Node> originalChildNodes = original.getChildNodes();
    List<Node> cloneChildNodes = clone.getChildNodes();

    for (int i = 0; i < originalChildNodes.size(); i++) {
      mapNodes(originalChildNodes.get(i), cloneChildNodes.get(i), map);
    }
  }

  private static void decompileJarFiles(
      String root, List<String> jarPaths, Set<Path> createdClass) {
    if (!jarPaths.isEmpty()) {
      List<String> argsToDecompile = new ArrayList<>();
      argsToDecompile.add("--silent");
      argsToDecompile.addAll(jarPaths);
      argsToDecompile.add(root);
      ConsoleDecompiler.main(argsToDecompile.toArray(new String[0]));
      // delete unneccessary legal files
      try {
        FileUtils.deleteDirectory(new File(root + "META-INF"));
      } catch (IOException ex) {
        // Following decompilation, Windows raises an IOException because the files are still
        // being used (by what?), so we should defer deletion until the end

        for (File legalFile :
            FileUtils.listFiles(new File(root + "META-INF"), new String[] {}, true)) {
          createdClass.add(legalFile.toPath());
        }
      }
    }
  }

  /**
   * Helper method to create a human-readable table of the unfound members and each member in the
   * same class that was considered.
   *
   * @param unfoundMembers the unfound members and the members that were considered
   * @param isMethod true if methods, false if fields
   * @return a human-readable string representation
   */
  private static String unfoundMembersTable(
      Map<String, Set<String>> unfoundMembers, boolean isMethod) {
    StringBuilder sb = new StringBuilder();
    for (String unfoundMember : unfoundMembers.keySet()) {
      sb.append("* ")
          .append(unfoundMember)
          .append("\n  Considered these ")
          .append(isMethod ? "methods" : "fields")
          .append(" from the same class:\n");
      for (String consideredMember : unfoundMembers.get(unfoundMember)) {
        sb.append("    * ").append(consideredMember).append("\n");
      }
    }
    return sb.toString();
  }

  /**
   * Update the static solver for JavaParser.
   *
   * @param root the root directory of the files to parse.
   * @param jarPaths the list of jar files to be used as input.
   * @throws IOException if something went wrong.
   */
  private static ParserConfiguration updateStaticSolver(String root, List<String> jarPaths)
      throws IOException {
    // Set up the parser's symbol solver, so that we can resolve definitions.
    CombinedTypeSolver typeSolver =
        new CombinedTypeSolver(new JdkTypeSolver(), new JavaParserTypeSolver(new File(root)));

    for (String path : jarPaths) {
      typeSolver.add(new JarTypeSolver(path));
    }

    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);

    ParserConfiguration config =
        new ParserConfiguration()
            .setSymbolResolver(symbolSolver)
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

    StaticJavaParser.setConfiguration(config);

    return config;
  }

  /**
   * Converts a path to a Java file into the fully-qualified name of the public class in that file,
   * relying on the file's relative path being the same as the package name.
   *
   * @param javaFilePath the path to a .java file, in this form: "path/of/package/ClassName.java".
   *     Note that this path must be rooted at the same directory in which javac could be invoked to
   *     compile the file
   * @return the fully-qualified name of the given class
   */
  @SuppressWarnings("signature") // string manipulation
  private static @FullyQualifiedName String getFullyQualifiedClassName(final String javaFilePath) {
    String result = javaFilePath.replace("/", ".");
    if (!result.endsWith(".java")) {
      throw new RuntimeException("A Java file path does not end with .java: " + result);
    }
    return result.substring(0, result.length() - 5);
  }

  /**
   * Checks whether the given compilation unit contains nothing. Should conservatively return false
   * by default if unsure.
   *
   * @param cu any compilation unit
   * @return true iff this compilation unit is totally empty and can therefore be safely removed
   *     entirely
   */
  private static boolean isEmptyCompilationUnit(CompilationUnit cu) {
    for (Node child : cu.getChildNodes()) {
      if (child instanceof PackageDeclaration
          || child instanceof ImportDeclaration
          || child instanceof Comment) {
        // Package declarations, imports, and comments don't count for the purposes of
        // deciding whether to entirely remove a compilation unit.
      } else {
        return false;
      }
    }
    return true;
  }

  /**
   * Retrieves the paths of Java files that should be created from the list of JAR files.
   *
   * @param outputDirectory The directory where the Java files will be created.
   * @param jarPaths The set of paths to JAR files.
   * @return A set containing the paths of the Java files to be created.
   * @throws IOException If an I/O error occurs.
   */
  private static Set<Path> getPathsFromJarPaths(String outputDirectory, List<String> jarPaths)
      throws IOException {
    Set<Path> pathsOfFile = new HashSet<>();
    for (String path : jarPaths) {
      JarTypeSolver jarSolver = new JarTypeSolver(path);
      for (String qualifedClassName : jarSolver.getKnownClasses()) {
        String relativePath = qualifedClassName.replace(".", "/") + ".java";
        String absolutePath = outputDirectory + relativePath;
        Path filePath = Paths.get(absolutePath);
        if (Files.exists(filePath)) {
          pathsOfFile.add(filePath);
        }
      }
    }
    return pathsOfFile;
  }

  /**
   * This method delete all files from a set of Paths. If a file is the only file in its parent
   * directory, this method will recursively delete the parent directories until it meets a
   * non-empty directory.
   *
   * @param fileList the set of Paths of files to be deleted
   */
  private static void deleteFiles(Set<Path> fileList) {
    for (Path filePath : fileList) {
      try {
        Files.delete(filePath);
        File classFile = new File(filePath.toString().replace(".java", ".class"));
        // since javac might leave some .class files
        if (classFile.exists()) {
          Files.delete(classFile.toPath());
        }
        File parentDir = filePath.toFile().getParentFile();
        if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
          String[] fileContained = parentDir.list();
          if (fileContained != null && fileContained.length == 0) {
            // Recursive call to delete the parent directory
            deleteFileFamily(parentDir);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("Unresolved file path: " + filePath, e);
      }
    }
  }

  /**
   * Returns a comment-free version of a compilation unit.
   *
   * @param cu A compilation unit possibly containing comments.
   * @return A comment-free version of the compilation unit.
   */
  private static CompilationUnit getCompilationUnitWithCommentsTrimmed(CompilationUnit cu) {
    CompilationUnit cuWithNoComments = cu;
    for (Comment child : cuWithNoComments.getAllComments()) {
      child.remove();
    }
    return cuWithNoComments;
  }

  /**
   * Given a directory, this method will delete that directory and recursively delete the parent
   * directories until it meets a non-empty directory. Be careful when making any changes to this
   * method, as an incorrect conditional statement could result in the deletion of non-empty
   * directories. This method is used to delete the temporary directory created by
   * UnsolvedSymbolVisitor.
   *
   * @param fileDir the directory of the file to be deleted
   */
  private static void deleteFileFamily(File fileDir) {
    fileDir.delete();
    File parentDir = fileDir.getParentFile();
    if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
      String[] fileContained = parentDir.list();
      // Be cautious when making any changes to this line, you might actually delete important
      // directories in the project.
      if (fileContained != null && fileContained.length == 0) {
        deleteFileFamily(parentDir);
      }
    }
  }

  /**
   * Given a directory, this method will return all the .jar files stored in the directory.
   *
   * @param directoryPath the directory of the jar files
   */
  private static List<String> getJarFiles(String directoryPath) throws IOException {
    Path jarPath = Path.of(directoryPath);
    try (Stream<Path> stream = Files.walk(jarPath)) {
      return stream
          .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".jar"))
          .map(path -> path.toString())
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the path of the file containing the definition for the class represented by a qualified
   * name. Throws an exception if this class is not in the original directory.
   *
   * @param qualifiedName The qualified name of the type
   * @param existingClassesToFilePath The map of existing classes to file paths
   * @param rootDirectory The root directory
   * @return The relative path of the file containing the definition of the class
   */
  private static String qualifiedNameToFilePath(
      String qualifiedName, Map<String, Path> existingClassesToFilePath, String rootDirectory) {
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
