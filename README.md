# Specimin: the specification slicer

**Note: Specimin is a work in progress, and is not yet fully functional. Please check
back later or contact the authors if you want to use the tool.**

This document describes **Specimin** (SPECIfication MINimizer).
Specimin's goal is, given a Java Program *P*
and a set of methods in that program *M*, produce a compilable, dependency-free
version of *P* that contains (1) the body of each method in *M*
and (2) as little else as possible while preserving the specifications
(i.e., the signatures of methods, the structure of classes, etc.) used
in the methods in *M*.

Specimin’s goal is to “stub out” everything that’s used by
the target method(s), for the purpose of static analysis of the methods
in the target set.

# Usage instructions

Download the project and the project directory in your shell.

To run the tool, use `./gradlew run --args='[OPTIONS]'`.

The available options are (required options in **bold**, repeatable options in *italics*):
* **--root**: specifies the root directory of the target project.
* ***--targetFile***: a source file in which to search for target methods
* ***--targetMethod***: a target method that must be preserved, and whose dependencies should be stubbed out. Use the format `class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...)`
* **--outputDirectory**: the directory in which to place the output. The directory must be writeable and will be created if it does not exist.
* *--jarPath*: the absolute path of a Jar file for Specimin to take as input.


Options may be specified in any order. When supplying repeatable options more than once, the option must be repeated for each value.

Here is a sample command to run the tool: `./gradlew run --args='--outputDirectory "tempDir" --root "src/test/resources/twofilesimple/input/" --targetFile "com/example/Foo.java" --targetFile "com/example/Baz.java" --targetMethod "com.example.Foo#bar()"'`

# Input/output examples

The following examples illustrate the kinds of programs that Specimin
produces.

## A very simple example

Consider the program below:

```
class Foo {
  void bar() {
    Object obj = new Object();
    obj = baz(obj);
  }

  Object baz(Object obj) {
    return obj.toString();
  }
}
```

Suppose that the user asks Specimin to target the method `bar()`.
The result should be the following program:

```
class Foo {
  void bar() {
    Object obj = new Object();
    obj = baz(obj);
  }

  Object baz(Object obj) {
    throw new Error();
  }
}
```

Note how `baz()`’s body has been replaced with `throw new Error()` -
this is the sort of transformation that Specimin should do to any
method that isn’t a target, but is used. The result of this change
has an identical specification to the original - annotations and types
will be preserved - but its behavior is empty. This illustrates that
Specimin is not intended to produce runnable output: its output is only
usefule for static analysis.

If the target had been `baz()` instead of `bar()`, then `bar()` would
be removed entirely: it is not referenced from `baz()`.

## Using another class

Consider the program below:

```
import com.foobar.baz.Baz;

class Foo {
  void bar() {
    Baz obj = new Baz();
  }
}
```

This program relies on the `Baz` class (supposing that we’re interested
in the `bar()` method of `Foo`). The resulting program should contain two
files (`Foo.java` and `Baz.java`, in appropriate source folders):

```
> cat Foo.java

import com.foobar.baz.Baz;

class Foo {
  void bar() {
    Baz obj = new Baz();
  }
}
```

```
> cat com/foobar/baz/Baz.java

package com.foobar.baz.Baz;

class Baz extends Object {
  public Baz() { }
}
```

This should be the result, regardless of whether `Baz` was originally
defined in the original program or in a library, because Specimin is
supposed to generate a dependency-free version of the target
program. That said, Specimin does need to respect the specification of
`Baz` - for example, if `Baz` has a superclass other than `Object`, that
superclass needs to be included, too. For example, the result could
have replaced the contents of `Baz.java` with the following, if that had
been the original definition of `Baz` (either in the Java source or in
the bytecode):

```
> cat com/foobar/baz/Baz.java

package com.foobar.baz.Baz;

class Baz extends Qux {
  public Baz() { }
}
```

```
> cat com/foobar/baz/Qux.java

package com.foobar.baz.Baz;

class Qux extends Object { }
```

A similar rule applies to type arguments of `Baz` and their bounds,
which should be fully included in the result:

```
> cat com/foobar/baz/Baz.java

package com.foobar.baz.Baz;

class Baz<T extends Qux> extends T {
  public Baz<T>() { }
}
```

```
> cat com/foobar/baz/Qux.java

package com.foobar.baz.Baz;

class Qux extends Object { }
```

The above could also be the result for `Baz`. The point here is that
at the specification level - including types, generics, superclasses,
implemented interfaces, annotations, etc. - the resulting program should
be the same as the original (with “dead” code that isn’t used by the
target method(s) removed, of course). But, at the implementation level,
nothing has to work - recall that Specimin’s goal is basically to
“stub out” everything that’s used by the target method.

## Annotations

Consider the program below:

```
class Foo {
  void bar() {
    Object obj = getObj();
    if (obj != null) {
      obj.toString();
    }
  }

  @Nullable Object getObj() { ... }
}
```

It is required that Specimin preserves annotations (especially type
annotations, but all annotations are supposed to be preserved) on
elements of the target program that will appear in the output.

## Java standard library

Consider the program below:

```
import java.util.List;

class Foo {
  void bar(List<Object> objs, int i) {
    Object obj = objs.get(i);
    if (obj != null) {
      obj.toString();
    }
  }
 }
 ```

Given that our specification for Specimin says that the produced
program should have no dependencies, should the generated program
include a (fake) implementation for `java.util.List`? This question is
tricky to answer. On the one hand, doing so is (1) more consistent
with Specimin’s treatment of other libraries, and (2) will make the
resulting programs self-contained, which will make them easier to
annotate later, if desired. On the other hand, generating stubbed
variants of standard library methods and classes might be problematic:
(1) it won’t be possible for every class - some classes are hard-coded
into the JVM (`Object`, `String`, a few others?  Definitely not that many,
though.), and (2) it will require the resulting program to be compiled
with unusual options, (i.e. `-Xbootclasspath=` the empty set or some
such nonsense), which would limit its suitability for generating test
cases. For now, I think this specification should permit Specimin to
do either.

TODO: decide which of these modes to support, or add support for both
and a switch.
