package com.example;

@Foo
public class Simple<@Foo T> {
    @Foo
    @AnnoDecl
    public <@Foo U> void baz(@Foo U u) {
        Simple<@Foo String> simple = new Simple<>();
        @Foo
        int x = simple.field;
    }

    @Foo
    public int field;

    @Foo
    public Simple() {
        throw new Error();
    }

    @Foo
    private @interface AnnoDecl {
    }
}
