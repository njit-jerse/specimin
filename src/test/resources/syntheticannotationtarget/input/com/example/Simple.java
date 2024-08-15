package com.example;

import java.lang.annotation.Target;
import java.util.*;

import static java.lang.annotation.ElementType.*;

@Type
public class Simple<@TypeParam T> {
    @Method
    @AnnoDecl
    public <@TypeParam U> void baz(@Param U u) {
        @LocalVariable
        Simple<@TypeParam String> simple = new Simple<>();
        @LocalVariable int x = simple.field;
        EnumTest e = EnumTest.A;
        List<String> y;
    }

    @Field
    public int field;

    @Constructor
    public Simple() { }

    @AnnotationDeclaration
    // METHOD should be the only one that remains
    @Target({METHOD, FIELD, TYPE_USE})
    private @interface AnnoDecl {

    }

    enum EnumTest {
        @EnumConstantDeclaration
        A
    }
}
