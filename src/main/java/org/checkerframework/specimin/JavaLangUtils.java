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
  }
}
