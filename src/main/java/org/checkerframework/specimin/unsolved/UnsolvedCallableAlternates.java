package org.checkerframework.specimin.unsolved;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.specimin.JavaParserUtil;
import org.checkerframework.specimin.QualifiedTypeName;

/**
 * Alternates of one synthetic method or constructor: everything that does not depend on which of
 * the two it is. See {@link UnsolvedMethodAlternates} for the members that only a method has (a
 * name and a return type) and {@link UnsolvedConstructorAlternates} for the constructor case.
 *
 * <p>Given a set of parameters and a set of potential encapsulating classes, this class allows for
 * alternates of the same callable to be generated in different locations. If a class were:
 *
 * <pre><code>
 * class A extends B implements C {
 *    void x() {
 *      int y = a();
 *    }
 * }
 * </code></pre>
 *
 * where B and C are both unresolvable, method a() could be in either one.
 *
 * <p>For type parameters, you may always assume the following convention: T, T1, T2, ...
 *
 * @param <T> the kind of alternate this holds
 */
public abstract class UnsolvedCallableAlternates<T extends UnsolvedCallable>
    extends UnsolvedSymbolAlternates<T> implements UnsolvedCallableCommon {
  /**
   * Base constructor for setting alternate declaring types.
   *
   * @param alternateDeclaringTypes A list of potential declaring types for this callable.
   */
  protected UnsolvedCallableAlternates(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes) {
    super(alternateDeclaringTypes);
  }

  /**
   * Returns a copy of the given alternate with a different parameter list and everything else the
   * same. Alternates are copied rather than mutated wherever a signature might take more than one
   * shape, and only a subclass knows how to build one of its own kind.
   *
   * @param alternate the alternate to copy
   * @param parameterList the parameter list the copy should have
   * @return the copy
   */
  protected abstract T copyWithParameters(T alternate, List<MemberType> parameterList);

  @Override
  public Set<String> getFullyQualifiedNames() {
    Set<String> fqns = new LinkedHashSet<>();

    for (T alternate : getAlternates()) {
      // The declared name is prepended below, once per declaring type: a constructor takes its
      // name from the type that declares it, so it is not the same for every key built here.
      StringBuilder signature = new StringBuilder("(");

      List<MemberType> parameterList = alternate.getParameterList();
      for (int i = 0; i < parameterList.size(); i++) {
        MemberType param = parameterList.get(i);

        // This is safe because all simple names are the same for unsolved types
        // and there is only one FQN for solved types
        signature.append(
            JavaParserUtil.getSimpleNameFromQualifiedName(JavaParserUtil.erase(param.toString())));

        if (i + 1 < parameterList.size()) {
          signature.append(", ");
        }
      }

      signature.append(')');

      for (UnsolvedClassOrInterfaceAlternates declaringType : getAlternateDeclaringTypes()) {
        for (String fqn : declaringType.getFullyQualifiedNames()) {
          fqns.add(
              fqn
                  + "#"
                  + alternate.declaredName(QualifiedTypeName.parse(fqn).simpleName())
                  + signature);
        }
      }
    }

    return fqns;
  }

  /**
   * Gets the number of type variables.
   *
   * @return The number of type variables
   */
  @Override
  public int getNumberOfTypeVariables() {
    return getAlternates().get(0).getNumberOfTypeVariables();
  }

  /**
   * Sets the number of type variables.
   *
   * @param number The number of type variables
   */
  @Override
  public void setNumberOfTypeVariables(int number) {
    applyToAllAlternates(UnsolvedCallable::setNumberOfTypeVariables, number);
  }

  @Override
  public String getTypeVariableName(int index) {
    return getAlternates().get(0).getTypeVariableName(index);
  }

  @Override
  public void declareTypeVariables(List<String> names) {
    applyToAllAlternates(UnsolvedCallable::declareTypeVariables, names);
  }

  /**
   * Returns whether the given type is one of the type variables bound by this callable's own
   * declaration (i.e. a name introduced by this callable, such as the {@code T} in {@code <T> T
   * get()}).
   *
   * <p>Such a name is only meaningful inside the declaration that binds it. Callers that are about
   * to use a type outside of that declaration -- for example, to describe the type of an expression
   * at a call site -- must not use the name, because it is not in scope there.
   *
   * @param type The type to check
   * @return true if the type is a type variable bound by this callable
   */
  public boolean isOwnTypeVariable(MemberType type) {
    Set<String> fqns = type.getFullyQualifiedNames();
    if (fqns.size() != 1 || !type.getTypeArguments().isEmpty()) {
      return false;
    }

    String name = fqns.iterator().next();
    for (T alternate : getAlternates()) {
      for (int i = 0; i < alternate.getNumberOfTypeVariables(); i++) {
        if (alternate.getTypeVariableName(i).equals(name)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Gets the parameter list, where each index contains a set of all possible parameter types at
   * that index.
   *
   * @return The parameter list
   */
  public List<Set<MemberType>> getParameterList() {
    List<Set<MemberType>> parameterList = new ArrayList<>();

    for (T alternate : getAlternates()) {
      List<MemberType> parameters = alternate.getParameterList();

      for (int i = 0; i < parameters.size(); i++) {
        MemberType param = parameters.get(i);

        if (parameterList.size() <= i) {
          parameterList.add(new LinkedHashSet<>());
        }

        parameterList.get(i).add(param);
      }
    }

    return parameterList;
  }

  @Override
  public List<MemberType> getThrownExceptions() {
    return getAlternates().get(0).getThrownExceptions();
  }

  @Override
  public void addThrownException(MemberType exception) {
    applyToAllAlternates(UnsolvedCallable::addThrownException, exception);
  }

  /**
   * Records that this callable might declare the given exception in its throws clause, by adding a
   * copy of each existing alternate with the exception added. Use this instead of {@link
   * #addThrownException} when it is not certain that this callable is the one that throws the
   * exception.
   *
   * <p>Note that {@link UnsolvedMethod#equals} ignores the throws clause, so a later call to {@link
   * #removeDuplicateAlternates()} collapses the alternates added here back into whichever of the
   * two possibilities comes first.
   *
   * @param exception the exception that this callable might throw
   * @param preferred whether the alternates that declare the exception should be preferred; if
   *     true, they are placed before the alternates that do not declare it
   */
  public void addAlternatesWithThrownException(MemberType exception, boolean preferred) {
    List<T> throwing = new ArrayList<>();

    for (T alternate : getAlternates()) {
      if (alternate.getThrownExceptions().contains(exception)) {
        // This callable is already known to throw the exception; there is no alternative to record.
        return;
      }

      T copy = copyWithParameters(alternate, alternate.getParameterList());
      copy.addThrownException(exception);
      throwing.add(copy);
    }

    if (preferred) {
      getAlternates().addAll(0, throwing);
    } else {
      getAlternates().addAll(throwing);
    }
  }

  /**
   * Replaces a parameter type with new parameter types in all alternates.
   *
   * @param oldType The parameter type to replace
   * @param newTypes The parameter types to replace with
   */
  public void replaceParameterType(MemberType oldType, Set<MemberType> newTypes) {
    int originalSize = getAlternates().size();
    for (int i = 0; i < originalSize; i++) {
      T alternate = getAlternates().get(i);

      List<MemberType> parameterList = alternate.getParameterList();
      boolean hasOldType = parameterList.contains(oldType);

      if (hasOldType) {
        boolean isFirst = true;
        for (MemberType newType : newTypes) {
          if (isFirst) {
            alternate.replaceParameterType(oldType, newType);
            isFirst = false;
            continue;
          }

          List<MemberType> newParameterList =
              parameterList.stream().map(param -> param.equals(oldType) ? newType : param).toList();

          addAlternate(copyWithParameters(alternate, newParameterList));
        }
      }
    }
  }

  /**
   * Use with caution: this method sets all alternates' return types to the same type.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public void setReturnType(MemberType memberType) {
    applyToAllAlternates(UnsolvedCallable::setReturnType, memberType);
    removeDuplicateAlternates();
  }

  @Override
  public String getAccessModifier() {
    return getAlternates().get(0).getAccessModifier();
  }

  @Override
  public void setAccessModifier(String accessModifier) {
    applyToAllAlternates(UnsolvedCallable::setAccessModifier, accessModifier);
  }

  @Override
  public void setContent(String content) {
    applyToAllAlternates(UnsolvedCallable::setContent, content);
  }
}
