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
        @LocalVariable
        int x = simple.field;
        EnumTest e = EnumTest.A;
        List<String> y;
    }

    @Field
    public int field;

    @Constructor
    public Simple() {
        throw new Error();
    }

    @AnnotationDeclaration
    @Target({ METHOD })
    private @interface AnnoDecl {
    }

    enum EnumTest {

        @EnumConstantDeclaration
        A
    }
}
