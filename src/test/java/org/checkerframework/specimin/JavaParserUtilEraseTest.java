package org.checkerframework.specimin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** This class unit tests the erase method in JavaParserUtil. */
public class JavaParserUtilEraseTest {

  /** A type with no type arguments at all is its own erasure. */
  @Test
  public void testNoTypeArguments() {
    assertEquals("Lease", JavaParserUtil.erase("Lease"));
    assertEquals("java.util.Map.Entry", JavaParserUtil.erase("java.util.Map.Entry"));
    assertEquals("int[]", JavaParserUtil.erase("int[]"));
    assertEquals("apply(Lease, boolean)", JavaParserUtil.erase("apply(Lease, boolean)"));
  }

  /** A single type argument list is deleted, wherever in the string it ends. */
  @Test
  public void testOneTypeArgumentList() {
    assertEquals("Lease", JavaParserUtil.erase("Lease<InstanceInfo>"));
    assertEquals("Map[]", JavaParserUtil.erase("Map<K, V>[]"));
    assertEquals("java.util.List", JavaParserUtil.erase("java.util.List<java.util.Map<K,V>>"));
    assertEquals("java.util.Map.Entry", JavaParserUtil.erase("java.util.Map.Entry<K,V>"));
    assertEquals("? extends Foo", JavaParserUtil.erase("? extends Foo<Bar>"));
    assertEquals(
        "apply(Lease, boolean)", JavaParserUtil.erase("apply(Lease<InstanceInfo>, boolean)"));
  }

  /**
   * A member type of a parameterized type carries a type argument list of its own (JLS 4.5), so the
   * name of the member type sits between two of them and must survive.
   */
  @Test
  public void testQualifiedMemberType() {
    assertEquals("com.foo.Outer.Inner", JavaParserUtil.erase("com.foo.Outer<A>.Inner<B>"));
    assertEquals("Outer.Middle.Inner", JavaParserUtil.erase("Outer<A>.Middle<B>.Inner<C>"));
  }

  /** Every parameter of a signature has its own type argument list, and its own erasure. */
  @Test
  public void testSignatureWithSeveralGenericParameters() {
    assertEquals(
        "foo(java.util.List, java.util.Map)",
        JavaParserUtil.erase("foo(java.util.List<String>, java.util.Map<K,V>)"));
    assertEquals(
        "foo(Outer.Inner, java.util.List)",
        JavaParserUtil.erase("foo(Outer<A>.Inner<B>, java.util.List<String>)"));
  }

  /**
   * A fragment whose angle brackets do not balance is not a type or a signature, and has no erasure
   * to compute.
   */
  @Test
  public void testUnbalanced() {
    assertEquals("java.util.Map<K", JavaParserUtil.erase("java.util.Map<K"));
    assertEquals("V>", JavaParserUtil.erase("V>"));
  }
}
