package org.checkerframework.specimin;

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.cache.Cache;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Creates and manages the type solvers required for Specimin, in addition to some reflection-based
 * utility methods.
 */
public class SpeciminTypeSolvers {
  /** The combined type solver. */
  private final CombinedTypeSolver typeSolver;

  /** The solver that reads in files from the root. */
  private final JavaParserTypeSolver javaParserTypeSolver;

  /** Type solver for types created during Specimin's run. */
  private final MemoryTypeSolver memoryTypeSolver;

  /**
   * Creates the necessary type solvers for Specimin.
   *
   * @param root The root directory of the input.
   * @param jarPaths The paths to the jar files.
   * @throws IOException if something goes wrong
   */
  public SpeciminTypeSolvers(String root, List<String> jarPaths) throws IOException {
    this.memoryTypeSolver = new MemoryTypeSolver();
    this.javaParserTypeSolver = new JavaParserTypeSolver(new File(root));
    this.typeSolver =
        new CombinedTypeSolver(new JdkTypeSolver(), javaParserTypeSolver, memoryTypeSolver);

    for (String path : jarPaths) {
      this.typeSolver.add(new JarTypeSolver(path));
    }
  }

  /**
   * Gets the {@link CombinedTypeSolver} associated with this instance. Use this as the type solver
   * for resolution.
   *
   * @return The {@link CombinedTypeSolver}
   */
  public CombinedTypeSolver getTypeSolver() {
    return typeSolver;
  }

  /**
   * Gets the {@link MemoryTypeSolver} associated with this instance.
   *
   * @return The {@link MemoryTypeSolver}
   */
  public MemoryTypeSolver getMemoryTypeSolver() {
    return memoryTypeSolver;
  }

  /**
   * Overrides a type's cache from JavaParser's resolution cache. This method uses reflection--there
   * is no way to modify the cache otherwise. Call this method when you have modified the AST and
   * you cannot have a stale cache for that object.
   *
   * <p>We must manually set the cache, because clearing it is not enough. In JavaParserTypeSolver,
   * it simply reparses the original source file, which does not contain any AST changes made during
   * Specimin's run.
   *
   * @param updatedDecl The declaration to override in the cache
   */
  public void overrideCache(TypeDeclaration<?> updatedDecl) {
    Object resolved = Resolver.resolve((Resolvable<?>) updatedDecl);

    if (resolved instanceof ResolvedReferenceTypeDeclaration resolvedDecl) {
      overrideInCombinedTypeSolverCache(resolvedDecl);
      overrideInJavaParserTypeSolverCache(resolvedDecl);
    }
  }

  /**
   * Overrides the cache in {@link SpeciminTypeSolvers#typeSolver} for the given type.
   *
   * @param resolvedReferenceTypeDeclaration The declaration to override in the cache
   */
  private void overrideInCombinedTypeSolverCache(
      ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
    try {
      overrideInJavaParserCacheImpl(
          CombinedTypeSolver.class.getDeclaredField("typeCache"),
          typeSolver,
          resolvedReferenceTypeDeclaration);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Overrides the cache in {@link SpeciminTypeSolvers#javaParserTypeSolver} for the given type.
   *
   * @param resolvedReferenceTypeDeclaration The declaration to override in the cache
   */
  private void overrideInJavaParserTypeSolverCache(
      ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
    try {
      overrideInJavaParserCacheImpl(
          JavaParserTypeSolver.class.getDeclaredField("foundTypes"),
          javaParserTypeSolver,
          resolvedReferenceTypeDeclaration);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the cache from the given field and overrides the given type.
   *
   * @param field The field containing the cache
   * @param solver The solver containing the field
   * @param resolvedReferenceTypeDeclaration The declaration to override in the cache
   */
  private void overrideInJavaParserCacheImpl(
      Field field,
      Object solver,
      ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
    try {
      field.setAccessible(true);

      @SuppressWarnings("unchecked")
      // This is safe; both type solvers declares this type
      Cache<String, SymbolReference<ResolvedReferenceTypeDeclaration>> value =
          (Cache<String, SymbolReference<ResolvedReferenceTypeDeclaration>>) field.get(solver);

      if (value == null) {
        throw new RuntimeException("Could not access JavaParser's symbol resolution cache.");
      }

      value.put(
          resolvedReferenceTypeDeclaration.getQualifiedName(),
          SymbolReference.solved(resolvedReferenceTypeDeclaration));
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
