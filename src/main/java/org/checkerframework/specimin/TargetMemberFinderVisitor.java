package org.checkerframework.specimin;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * The main visitor for Specimin's first phase, which locates the target member(s) and compiles
 * information on what specifications they use.
 */
public class TargetMemberFinderVisitor extends ModifierVisitor<Void> {
  /**
   * The names of the target methods. The format is
   * class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...). All the names will have
   * spaces remove for ease of comparison.
   */
  private final Set<String> targetMethods;

  /** The names of the target fields. The format is class.fully.qualified.Name#fieldName. */
  private final Set<String> targetFields;

  /** The simple name of the class currently visited */
  protected @ClassGetSimpleName String className = "";

  /** The qualified name of the class currently being visited. */
  protected String currentClassQualifiedName = "";

  /**
   * The keys of this map are a local copy of the input list of methods. A method is removed from
   * this copy's key set when it is located. If the visitor has been run on all source files and the
   * key set isn't empty, that usually indicates an error. The values are other method signatures
   * that were found in the key's class, but which were not the key. These values are only used for
   * error reporting: it is useful to the user if they make a typo to know what the correct
   * name/signature of a method actually is.
   */
  private final Map<String, Set<String>> unfoundMethods;

  /** Same as the unfoundMethods set, but for fields */
  private final Map<String, Set<String>> unfoundFields = new HashMap<>();

  /** The slicer, providing an ability to add to the worklist or slice. */
  private final Slicer slicer;

  /**
   * Create a new target method finding visitor.
   *
   * @param targetMethods The set of methods to preserve
   * @param targetFields The set of fields to preserve
   */
  public TargetMemberFinderVisitor(
      List<String> targetMethods, List<String> targetFields, Slicer slicer) {
    this.targetMethods = new HashSet<String>();

    for (String methodSignature : targetMethods) {
      this.targetMethods.add(methodSignature.replaceAll("\\s", ""));
    }

    this.targetFields = new HashSet<>(targetFields);

    unfoundMethods = new HashMap<>(targetMethods.size());
    this.targetMethods.forEach(m -> unfoundMethods.put(m, new HashSet<>()));
    this.targetFields.forEach(f -> unfoundFields.put(f, new HashSet<>()));

    this.slicer = slicer;
  }

  /**
   * Returns the methods that so far this visitor has not located from its target list. Usually,
   * this should be checked after running the visitor to ensure that it is empty. The targets are
   * the keys in the returned maps; the values are methods in the same class that were considered
   * but were not the target (useful for issuing error messages).
   *
   * @return the methods that so far this visitor has not located from its target list, mapped to
   *     the candidate methods that were considered
   */
  public Map<String, Set<String>> getUnfoundMethods() {
    return unfoundMethods;
  }

  /**
   * Returns the fields that so far this visitor has not located from its target list. Usually, this
   * should be checked after running the visitor to ensure that it is empty. The targets are the
   * keys in the returned maps; the values are fields in the same class that were considered but
   * were not the target (useful for issuing error messages).
   *
   * @return the fields that so far this visitor has not located from its target list, mapped to the
   *     candidate fields that were considered
   */
  public Map<String, Set<String>> getUnfoundFields() {
    return unfoundFields;
  }

  /**
   * Updates unfoundMethods so that the appropriate elements have their set of considered methods
   * updated to match a method that was not a target method.
   *
   * @param methodAsString the method that wasn't a target method
   */
  private void updateUnfoundMethods(String methodAsString) {
    Set<String> targetMethodsInClass =
        targetMethods.stream()
            .filter(t -> t.startsWith(this.currentClassQualifiedName))
            .collect(Collectors.toSet());

    for (String targetMethodInClass : targetMethodsInClass) {
      // This check is necessary to avoid an NPE if the target method
      // in question has already been removed from unfoundMethods.
      if (unfoundMethods.containsKey(targetMethodInClass)) {
        unfoundMethods.get(targetMethodInClass).add(methodAsString);
      }
    }
  }

  /**
   * Updates unfoundFields so that the appropriate elements have their set of considered fields
   * updated to match a field that was not a target field.
   *
   * @param fieldAsString the field that wasn't a target field
   */
  private void updateUnfoundFields(String fieldAsString) {
    Set<String> targetFieldsInClass =
        targetFields.stream()
            .filter(t -> t.startsWith(this.currentClassQualifiedName))
            .collect(Collectors.toSet());

    for (String targetFieldInClass : targetFieldsInClass) {
      // This check is necessary to avoid an NPE if the target field
      // in question has already been removed from unfoundFields.
      if (unfoundFields.containsKey(targetFieldInClass)) {
        unfoundFields.get(targetFieldInClass).add(fieldAsString);
      }
    }
  }

  /*
   * The purpose of this method is to help find the fully qualified name, not to do any modification
   * to the worklist/slice.
   */
  @Override
  public Visitable visit(ClassOrInterfaceDeclaration decl, Void p) {
    if (decl.isNestedType()) {
      this.currentClassQualifiedName += "." + decl.getName().asString();
    } else if (!JavaParserUtil.isLocalClassDecl(decl)) {
      // the purpose of keeping track of class name is to recognize the signatures of target
      // methods. Since we don't support methods inside local classes as target methods, we don't
      // need
      // to keep track of class name in this case.
      this.currentClassQualifiedName = decl.getFullyQualifiedName().orElseThrow();
    }

    Visitable result = super.visit(decl, p);

    if (decl.isNestedType()) {
      this.currentClassQualifiedName =
          this.currentClassQualifiedName.substring(
              0, this.currentClassQualifiedName.lastIndexOf('.'));
    } else if (!JavaParserUtil.isLocalClassDecl(decl)) {
      this.currentClassQualifiedName = "";
    }

    return result;
  }

  @Override
  public Visitable visit(ConstructorDeclaration method, Void p) {
    String constructorMethodAsString = method.getDeclarationAsString(false, false, false);
    // the methodName will be something like this: "com.example.Car#Car()"
    String methodName = this.currentClassQualifiedName + "#" + constructorMethodAsString;
    // remove spaces
    methodName = methodName.replaceAll("\\s", "");
    if (this.targetMethods.contains(methodName)) {
      unfoundMethods.remove(methodName);
      addMethodAndChildrenToWorklist(method);
    } else {
      updateUnfoundMethods(methodName);
    }

    return super.visit(method, p);
  }

  @Override
  public Visitable visit(MethodDeclaration method, Void p) {
    String methodWithoutReturnAndAnnos =
        JavaParserUtil.removeMethodReturnTypeAndAnnotations(method);
    String methodName = this.currentClassQualifiedName + "#" + methodWithoutReturnAndAnnos;

    String methodWithoutAnySpace = methodName.replaceAll("\\s", "");

    if (this.targetMethods.contains(methodWithoutAnySpace)) {
      unfoundMethods.remove(methodWithoutAnySpace);
      addMethodAndChildrenToWorklist(method);
    } else {
      updateUnfoundMethods(methodWithoutAnySpace);
    }

    return super.visit(method, p);
  }

  private void addMethodAndChildrenToWorklist(CallableDeclaration<?> method) {
    // Add itself to the worklist
    slicer.addToWorklist(method);

    // Add body to the worklist

    if (method instanceof ConstructorDeclaration constructor) {
      slicer.addToWorklist(constructor.getBody());
    } else {
      Optional<BlockStmt> body = ((MethodDeclaration) method).getBody();

      if (body.isPresent()) {
        slicer.addToWorklist(body.get());
      }
    }
  }

  @Override
  public Visitable visit(VariableDeclarator node, Void arg) {
    if (node.getParentNode().isPresent()
        && node.getParentNode().get() instanceof FieldDeclaration) {
      String fieldName = this.currentClassQualifiedName + "#" + node.getNameAsString();
      if (targetFields.contains(fieldName)) {
        unfoundFields.remove(fieldName);

        slicer.addToWorklist(node);
      } else {
        updateUnfoundFields(fieldName);
      }
    }
    return super.visit(node, arg);
  }
}
