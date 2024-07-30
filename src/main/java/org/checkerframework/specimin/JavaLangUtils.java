package org.checkerframework.specimin;

import java.util.HashSet;
import java.util.Set;

/** Utility class for questions related to the java.lang package. */
public final class JavaLangUtils {

  /**
   * Checks if the given simple name is a member of the java.lang package. This method returns false
   * for primitive names (e.g., "int").
   *
   * @param simpleName a simple name
   * @return true if this name is defined by java.lang
   */
  public static boolean isJavaLangName(String simpleName) {
    return javaLangClassesAndInterfaces.contains(simpleName);
  }

  /**
   * Checks if the given simple name is a member of the java.lang package. This method also returns
   * true for primitive types (like "int").
   *
   * @param simpleName a simple name
   * @return true if the name is defined by java.lang or is a primitive
   */
  public static boolean isJavaLangOrPrimitiveName(String simpleName) {
    return primitives.contains(simpleName) || javaLangClassesAndInterfaces.contains(simpleName);
  }

  /** Don't call this. */
  private JavaLangUtils() {
    throw new Error("cannot be instantiated");
  }

  /**
   * Internal set for the java lang types. The list comes from downloading the java.lang summary
   * Javadoc page and then applying some transformations to it. TODO: figure out what those were and
   * script it.
   */
  private static final Set<String> javaLangClassesAndInterfaces = new HashSet<>();

  /** Internal set for the names of the primitive types. */
  private static final Set<String> primitives = new HashSet<>(8);

  /**
   * This is an (incomplete) list of classes that we know are final in the JDK. The idea is that
   * Specimin should never try to extend a class in this list.
   */
  private static final Set<String> knownFinalJdkTypes = new HashSet<>();

  static {
    primitives.add("int");
    primitives.add("short");
    primitives.add("byte");
    primitives.add("long");
    primitives.add("boolean");
    primitives.add("float");
    primitives.add("double");
    primitives.add("char");

    javaLangClassesAndInterfaces.add("AbstractMethodError");
    javaLangClassesAndInterfaces.add("Appendable");
    javaLangClassesAndInterfaces.add("ArithmeticException");
    javaLangClassesAndInterfaces.add("ArrayIndexOutOfBoundsException");
    javaLangClassesAndInterfaces.add("ArrayStoreException");
    javaLangClassesAndInterfaces.add("AssertionError");
    javaLangClassesAndInterfaces.add("AutoCloseable");
    javaLangClassesAndInterfaces.add("Boolean");
    javaLangClassesAndInterfaces.add("BootstrapMethodError");
    javaLangClassesAndInterfaces.add("Byte");
    javaLangClassesAndInterfaces.add("Character");
    javaLangClassesAndInterfaces.add("Character.Subset");
    javaLangClassesAndInterfaces.add("Character.UnicodeBlock");
    javaLangClassesAndInterfaces.add("Character.UnicodeScript");
    javaLangClassesAndInterfaces.add("CharSequence");
    javaLangClassesAndInterfaces.add("Class");
    javaLangClassesAndInterfaces.add("ClassCastException");
    javaLangClassesAndInterfaces.add("ClassCircularityError");
    javaLangClassesAndInterfaces.add("ClassFormatError");
    javaLangClassesAndInterfaces.add("ClassLoader");
    javaLangClassesAndInterfaces.add("ClassNotFoundException");
    javaLangClassesAndInterfaces.add("ClassValue");
    javaLangClassesAndInterfaces.add("Cloneable");
    javaLangClassesAndInterfaces.add("CloneNotSupportedException");
    javaLangClassesAndInterfaces.add("Comparable");
    javaLangClassesAndInterfaces.add("Deprecated");
    javaLangClassesAndInterfaces.add("Double");
    javaLangClassesAndInterfaces.add("Enum");
    javaLangClassesAndInterfaces.add("Enum.EnumDesc");
    javaLangClassesAndInterfaces.add("EnumConstantNotPresentException");
    javaLangClassesAndInterfaces.add("Error");
    javaLangClassesAndInterfaces.add("Exception");
    javaLangClassesAndInterfaces.add("ExceptionInInitializerError");
    javaLangClassesAndInterfaces.add("Float");
    javaLangClassesAndInterfaces.add("FunctionalInterface");
    javaLangClassesAndInterfaces.add("IdentityException");
    javaLangClassesAndInterfaces.add("IllegalAccessError");
    javaLangClassesAndInterfaces.add("IllegalAccessException");
    javaLangClassesAndInterfaces.add("IllegalArgumentException");
    javaLangClassesAndInterfaces.add("IllegalCallerException");
    javaLangClassesAndInterfaces.add("IllegalMonitorStateException");
    javaLangClassesAndInterfaces.add("IllegalStateException");
    javaLangClassesAndInterfaces.add("IllegalThreadStateException");
    javaLangClassesAndInterfaces.add("IncompatibleClassChangeError");
    javaLangClassesAndInterfaces.add("IndexOutOfBoundsException");
    javaLangClassesAndInterfaces.add("InheritableThreadLocal");
    javaLangClassesAndInterfaces.add("InstantiationError");
    javaLangClassesAndInterfaces.add("InstantiationException");
    javaLangClassesAndInterfaces.add("Integer");
    javaLangClassesAndInterfaces.add("InternalError");
    javaLangClassesAndInterfaces.add("InterruptedException");
    javaLangClassesAndInterfaces.add("Iterable");
    javaLangClassesAndInterfaces.add("LayerInstantiationException");
    javaLangClassesAndInterfaces.add("LinkageError");
    javaLangClassesAndInterfaces.add("Long");
    javaLangClassesAndInterfaces.add("MatchException");
    javaLangClassesAndInterfaces.add("Math");
    javaLangClassesAndInterfaces.add("Module");
    javaLangClassesAndInterfaces.add("ModuleLayer");
    javaLangClassesAndInterfaces.add("ModuleLayer.Controller");
    javaLangClassesAndInterfaces.add("NegativeArraySizeException");
    javaLangClassesAndInterfaces.add("NoClassDefFoundError");
    javaLangClassesAndInterfaces.add("NoSuchFieldError");
    javaLangClassesAndInterfaces.add("NoSuchFieldException");
    javaLangClassesAndInterfaces.add("NoSuchMethodError");
    javaLangClassesAndInterfaces.add("NoSuchMethodException");
    javaLangClassesAndInterfaces.add("NullPointerException");
    javaLangClassesAndInterfaces.add("Number");
    javaLangClassesAndInterfaces.add("NumberFormatException");
    javaLangClassesAndInterfaces.add("Object");
    javaLangClassesAndInterfaces.add("OutOfMemoryError");
    javaLangClassesAndInterfaces.add("Override");
    javaLangClassesAndInterfaces.add("Package");
    javaLangClassesAndInterfaces.add("Process");
    javaLangClassesAndInterfaces.add("ProcessBuilder");
    javaLangClassesAndInterfaces.add("ProcessBuilder.Redirect");
    javaLangClassesAndInterfaces.add("ProcessBuilder.Redirect.Type");
    javaLangClassesAndInterfaces.add("ProcessHandle");
    javaLangClassesAndInterfaces.add("ProcessHandle.Info");
    javaLangClassesAndInterfaces.add("Readable");
    javaLangClassesAndInterfaces.add("Record");
    javaLangClassesAndInterfaces.add("ReflectiveOperationException");
    javaLangClassesAndInterfaces.add("Runnable");
    javaLangClassesAndInterfaces.add("Runtime");
    javaLangClassesAndInterfaces.add("Runtime.Version");
    javaLangClassesAndInterfaces.add("RuntimeException");
    javaLangClassesAndInterfaces.add("RuntimePermission");
    javaLangClassesAndInterfaces.add("SafeVarargs");
    javaLangClassesAndInterfaces.add("SecurityException");
    javaLangClassesAndInterfaces.add("SecurityManager");
    javaLangClassesAndInterfaces.add("Short");
    javaLangClassesAndInterfaces.add("StackOverflowError");
    javaLangClassesAndInterfaces.add("StackTraceElement");
    javaLangClassesAndInterfaces.add("StackWalker");
    javaLangClassesAndInterfaces.add("StackWalker.Option");
    javaLangClassesAndInterfaces.add("StackWalker.StackFrame");
    javaLangClassesAndInterfaces.add("StrictMath");
    javaLangClassesAndInterfaces.add("String");
    javaLangClassesAndInterfaces.add("StringBuffer");
    javaLangClassesAndInterfaces.add("StringBuilder");
    javaLangClassesAndInterfaces.add("StringIndexOutOfBoundsException");
    javaLangClassesAndInterfaces.add("SuppressWarnings");
    javaLangClassesAndInterfaces.add("System");
    javaLangClassesAndInterfaces.add("System.Logger");
    javaLangClassesAndInterfaces.add("System.Logger.Level");
    javaLangClassesAndInterfaces.add("System.LoggerFinder");
    javaLangClassesAndInterfaces.add("Thread");
    javaLangClassesAndInterfaces.add("Thread.Builder");
    javaLangClassesAndInterfaces.add("Thread.Builder.OfPlatform");
    javaLangClassesAndInterfaces.add("Thread.Builder.OfVirtual");
    javaLangClassesAndInterfaces.add("Thread.State");
    javaLangClassesAndInterfaces.add("Thread.UncaughtExceptionHandler");
    javaLangClassesAndInterfaces.add("ThreadDeath");
    javaLangClassesAndInterfaces.add("ThreadGroup");
    javaLangClassesAndInterfaces.add("ThreadLocal");
    javaLangClassesAndInterfaces.add("Throwable");
    javaLangClassesAndInterfaces.add("TypeNotPresentException");
    javaLangClassesAndInterfaces.add("UnknownError");
    javaLangClassesAndInterfaces.add("UnsatisfiedLinkError");
    javaLangClassesAndInterfaces.add("UnsupportedClassVersionError");
    javaLangClassesAndInterfaces.add("UnsupportedOperationException");
    javaLangClassesAndInterfaces.add("VerifyError");
    javaLangClassesAndInterfaces.add("VirtualMachineError");
    javaLangClassesAndInterfaces.add("Void");
    javaLangClassesAndInterfaces.add("WrongThreadException");

    // I made this list by going through the members of
    // java.lang and checking which were final classes. We
    // can do the same for other packages as needed.
    knownFinalJdkTypes.add("String");
    knownFinalJdkTypes.add("Class");
    knownFinalJdkTypes.add("Integer");
    knownFinalJdkTypes.add("Byte");
    knownFinalJdkTypes.add("Short");
    knownFinalJdkTypes.add("Long");
    knownFinalJdkTypes.add("Double");
    knownFinalJdkTypes.add("Float");
    knownFinalJdkTypes.add("Character");
    knownFinalJdkTypes.add("Character.UnicodeBlock");
    knownFinalJdkTypes.add("Boolean");
    knownFinalJdkTypes.add("Compiler");
    knownFinalJdkTypes.add("Math");
    knownFinalJdkTypes.add("ProcessBuilder");
    knownFinalJdkTypes.add("RuntimePermission");
    knownFinalJdkTypes.add("StackTraceElement");
    knownFinalJdkTypes.add("StrictMath");
    knownFinalJdkTypes.add("StringBuffer");
    knownFinalJdkTypes.add("StringBuilder");
    knownFinalJdkTypes.add("System");
    knownFinalJdkTypes.add("Void");
    Set<String> withJavaLang = new HashSet<>(knownFinalJdkTypes.size());
    for (String s : knownFinalJdkTypes) {
      withJavaLang.add("java.lang." + s);
    }
    knownFinalJdkTypes.addAll(withJavaLang);
  }

  /** The integral primitives. */
  private static final String[] INTEGRAL_PRIMITIVES =
      new String[] {"int", "Integer", "long", "Long", "byte", "Byte", "short", "Short"};

  /** The numeric primitives. */
  private static final String[] NUMERIC_PRIMITIVES =
      new String[] {
        "int", "Integer", "long", "Long", "byte", "Byte", "short", "Short", "float", "Float",
        "double", "Double"
      };

  /**
   * Same as {@link #NUMERIC_PRIMITIVES}, but also with "String". TODO: it would be nice to
   * construct this from NUMERIC_PRIMITIVES, but I don't know how to do that in Java :(
   */
  private static final String[] NUMERIC_PRIMITIVES_AND_STRING =
      new String[] {
        "int", "Integer", "long", "Long", "byte", "Byte", "short", "Short", "float", "Float",
        "double", "Double", "String"
      };

  /** The booleans. */
  private static final String[] BOOLEANS = new String[] {"boolean", "Boolean"};

  /** The numeric primitives and booleans. */
  private static final String[] NUMERIC_PRIMITIVES_AND_BOOLEANS =
      new String[] {
        "int", "Integer", "long", "Long", "byte", "Byte", "short", "Short", "float", "Float",
        "double", "Double", "boolean", "Boolean"
      };

  /**
   * Given the string representation of a binary operator, what are the possible input types? The
   * first element of the result is assumed to be the default if no other information is available.
   * Note that this method is **under** approximate in the case of == and !=: it returns the
   * **primitive** (and boxed) types that are possible; the goal of this method is return the
   * possible types that could be relevant if a "bad operand types for binary operator" error is
   * issued by Javac.
   *
   * @param binOp a string representation of a binary operator, such as "||"
   * @return the set of compatible types, such as ["boolean", "Boolean"]
   */
  public static String[] getTypesForOp(String binOp) {
    switch (binOp) {
      case "*":
      case "/":
      case "%":
        // JLS 15.17
        return NUMERIC_PRIMITIVES;
      case "-":
        // JLS 15.18
        return NUMERIC_PRIMITIVES;
      case "+":
        // JLS 15.18 (see note about "+", which can also mean string concatenation!)
        return NUMERIC_PRIMITIVES_AND_STRING;
      case ">>":
      case ">>>":
      case "<<":
        // JSL 15.19
        return INTEGRAL_PRIMITIVES;
      case "<":
      case "<=":
      case ">":
      case ">=":
        // JLS 15.20.1
        return NUMERIC_PRIMITIVES;
      case "==":
      case "!=":
        // JLS 15.21 says that it's an error if one of the sides of an == or != is a boolean or
        // numeric type, but the other is not. This return value is based on that error condition.
        return NUMERIC_PRIMITIVES_AND_BOOLEANS;
      case "^":
      case "&":
      case "|":
        // JLS 15.22
        return NUMERIC_PRIMITIVES_AND_BOOLEANS;
      case "||":
      case "&&":
        // JLS 15.23 and 15.24
        return BOOLEANS;
      default:
        throw new IllegalArgumentException("unexpected binary operator: " + binOp);
    }
  }

  /**
   * Returns true iff both input types are java.lang.Class followed by some type parameter.
   *
   * @param type1 a type
   * @param type2 another type
   * @return true iff they're both Class
   */
  public static boolean bothAreJavaLangClass(String type1, String type2) {
    boolean type1IsClass = type1.startsWith("Class<") || type1.startsWith("java.lang.Class<");
    boolean type2IsClass = type2.startsWith("Class<") || type2.startsWith("java.lang.Class<");
    return type1IsClass && type2IsClass;
  }

  /**
   * Is the given package name or fully-qualified name in one of the packages provided by the JDK?
   *
   * @param qualifiedName a package name or fully-qualified name of a class or interface
   * @return true if qualifiedName is from the JDK
   */
  public static boolean inJdkPackage(String qualifiedName) {
    // TODO: can we get a list of such packages from the JDK instead of using this relatively-coarse
    // heuristic?
    if (qualifiedName.startsWith("javax.annotation")) {
      return false;
    }

    return qualifiedName.startsWith("java.")
        || qualifiedName.startsWith("javax.")
        || qualifiedName.startsWith("com.sun.")
        || qualifiedName.startsWith("jdk.");
  }

  /**
   * Could the given name be a final class from the JDK, like String?
   *
   * @param name a simple or fully-qualified name
   * @return true if the input might be a final JDK class
   */
  public static boolean isFinalJdkClass(String name) {
    return knownFinalJdkTypes.contains(name);
  }
}
