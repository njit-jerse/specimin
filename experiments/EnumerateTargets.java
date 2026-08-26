import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * Lists every method, constructor and field in a Java source tree, in the signature format that
 * Specimin's --targetMethod and --targetField options expect.
 *
 * <p>Usage: java -cp specimin.jar EnumerateTargets.java SOURCE_ROOT
 *
 * <p>Writes one tab-separated record per line to standard output:
 * {@code relative/path/To/File.java <TAB> method|field <TAB> fully.qualified.Type#member(Arg,Arg)}.
 * The path is relative to SOURCE_ROOT, i.e. it is what --targetFile wants.
 *
 * <p>The signatures are built the same way {@code TargetMemberFinderVisitor} builds the names it
 * matches against, including the call to {@code removeMethodReturnTypeAndAnnotations}, so that
 * every emitted target is one Specimin can actually find. Members that Specimin cannot target are
 * skipped: anything inside a local class, an anonymous class or an enum constant's class body, and
 * anything in a top-level type whose name does not match its file (targeting those requires
 * --disable-root-validation).
 */
public class EnumerateTargets {

  /** This class is a collection of static methods; it should not be instantiated. */
  private EnumerateTargets() {
    throw new AssertionError("do not instantiate");
  }

  /**
   * Entry point.
   *
   * @param args one element: the source root to enumerate
   * @throws IOException if the source root cannot be walked
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("usage: EnumerateTargets SOURCE_ROOT");
      System.exit(2);
    }
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    StaticJavaParser.setConfiguration(
        new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
    StringBuilder out = new StringBuilder();
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        try {
          emitTargets(root, file, out);
        } catch (Exception e) {
          // An unparseable file is not interesting: skip it and keep going.
          System.err.println("skipping " + file + ": " + e);
        }
      }
    }
    System.out.print(out);
  }

  /**
   * Appends a record for every targetable member of one file.
   *
   * @param root the source root
   * @param file the file to enumerate
   * @param out the buffer to append to
   * @throws IOException if the file cannot be read
   */
  private static void emitTargets(Path root, Path file, StringBuilder out) throws IOException {
    CompilationUnit cu = StaticJavaParser.parse(file);
    String relative = root.relativize(file).toString();
    String primaryType = file.getFileName().toString().replace(".java", "");
    for (CallableDeclaration<?> callable : cu.findAll(CallableDeclaration.class)) {
      if (!(callable instanceof MethodDeclaration) && !(callable instanceof ConstructorDeclaration)) {
        continue;
      }
      String owner = ownerOf(callable, primaryType);
      if (owner == null) {
        continue;
      }
      String signature =
          callable instanceof ConstructorDeclaration
              ? callable.getDeclarationAsString(false, false, false)
              : JavaParserUtil.removeMethodReturnTypeAndAnnotations(callable);
      out.append(relative)
          .append('\t')
          .append("method")
          .append('\t')
          .append((owner + "#" + signature).replaceAll("\\s", ""))
          .append('\n');
    }
    for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
      String owner = ownerOf(field, primaryType);
      if (owner == null) {
        continue;
      }
      for (VariableDeclarator variable : field.getVariables()) {
        out.append(relative)
            .append('\t')
            .append("field")
            .append('\t')
            .append(owner + "#" + variable.getNameAsString())
            .append('\n');
      }
    }
  }

  /**
   * Returns the qualified name of the type that declares a member, or null if Specimin cannot
   * target members of that type.
   *
   * @param member the member declaration
   * @param primaryType the simple name of the file's primary type
   * @return the declaring type's qualified name, or null
   */
  private static String ownerOf(Node member, String primaryType) {
    List<String> nesting = new ArrayList<>();
    Node current = member;
    while (current.getParentNode().isPresent()) {
      current = current.getParentNode().get();
      if (current instanceof ObjectCreationExpr
          || current instanceof EnumConstantDeclaration
          || current instanceof LocalClassDeclarationStmt
          || current instanceof LocalRecordDeclarationStmt) {
        return null;
      }
      if (current instanceof TypeDeclaration<?> type) {
        if (type.isNestedType()) {
          nesting.add(0, type.getNameAsString());
        } else {
          if (!type.getNameAsString().equals(primaryType)) {
            return null;
          }
          String qualified = type.getFullyQualifiedName().orElse(null);
          if (qualified == null) {
            return null;
          }
          nesting.add(0, qualified);
          return String.join(".", nesting);
        }
      }
    }
    return null;
  }
}
