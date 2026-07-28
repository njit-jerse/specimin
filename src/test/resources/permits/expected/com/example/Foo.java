package com.example;

public sealed class Foo permits Bar, Baz, Qux {
    void foo() { }
}

final class Bar extends Foo { }
non-sealed class Baz extends Foo { }
sealed class Qux extends Foo permits Quux { }

final class Quux extends Qux { }