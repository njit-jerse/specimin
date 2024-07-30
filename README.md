# Specimin: a specification slicer

This document describes **Specimin** (SPECIfication MINimizer).
Specimin's goal is, given a Java Program *P*
and a set of methods or fields in that program *M*, produce an independently-compilable
version of *P* that contains (1) the body of each method or initializer of each field in *M*
and (2) as little else as possible while preserving the specifications
(i.e., the signatures of methods, the structure of classes, etc.) used
by anything in *M*.

Specimin is useful as a static program reduction tool when debugging a compiler or type
system that does *modular* program analysis, such as a [Checker Framework](checkerframework.org)
checker or the type system of Java itself. Specimin's output should preserve the output
of a type-system-like analysis (such as a crash, false positive, or false negative).

Specimin supports two *modes*: exact and approximate specification slicing. Exact mode
is automatically used if all relevant source or class files are provided to Specimin. If
a relevant source or class file is used but missing, Specimin will enter approximate mode
and create synthetic Java code based on the context in which that missing source or class
file is used. In approximate mode, ambiguity in the context may cause Specimin to fail
to preserve the behavior of an analysis tool. However, approximate mode is very useful
for analysis debugging (when it works), because the user need not supply the classpath
of the target program. When debugging crashes or false positives reported by users, this
avoids the need to interact with the user's build system at all.

# Usage instructions

Clone the project and `cd` the project directory in your shell.

To run the tool, use `./gradlew run --args='[OPTIONS]'`.

The available options are (required options in **bold**, repeatable options in *italics*):
* **--root**: specifies the root directory of the target project.
* ***--targetFile***: a source file in which to search for target methods
* *--targetMethod*: a target method that must be preserved, and whose dependencies should be stubbed out. Use the format `class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...)`. Note: If a target method has a receiver parameter, i.e., (.. this), exclude that parameter from the signature. Check this [documentation](https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-ReceiverParameter) for more info.
* *--targetField*: a target field that must be preserved (including its initializer). Uses the same format as `--targetMethod`, but without the parameter list. The `--targetMethod` and `--targetField` options can be freely combined, but if at least one of the two is not provided then Specimin will always produce empty output.

* **--outputDirectory**: the directory in which to place the output. The directory must be writeable and will be created if it does not exist.
* *--jarPath*: a directory path that contains all the jar files for Specimin to take as input.
* --modularityModel: the name of the modularity model to use. Modularity models are named after the analysis that they represent. Available options: "javac" for the [Javac typechecker](https://en.wikipedia.org/wiki/Javac), "cf" for the [Checker Framework](checkerframework.org), or "nullaway" for [NullAway](https://github.com/uber/NullAway). Default: "cf".

Options may be specified in any order. When supplying repeatable options more than once, the option must be repeated for each value.

Here is a sample command to run the tool: `./gradlew run --args='--outputDirectory "tempDir" --root "src/test/resources/twofilesimple/input/" --targetFile "com/example/Foo.java" --targetFile "com/example/Baz.java" --targetMethod "com.example.Foo#bar()" --jarpath "path/to/jar/directory"'`

# Important limitations and caveats

The implementation makes use of heuristics to distinguish simple names from fully-qualified names
at several points during approximate slicing. In particular, it assumes that package and class names
follow Java convention:
* class names always begin with an uppercase letter
* package names always begin with a lowercase letter

Specimin will likely produce incorrect output if the input program does not follow this convention
when the program's full classpath is not provided as a `--jarPath` input.

# Input/output examples

The following examples illustrate the kinds of programs that Specimin
produces. You can find (many) more such examples in Specimin's
[test suite](https://github.com/kelloggm/specimin/tree/main/src/test/resources).

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

Specimin usually produces programs that have no dependencies.
In a case like this one, though, should the generated program
include a (synthetic) implementation for `java.util.List`? This question is
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
cases.

In cases like these, Specimin chooses the latter: it assumes that the JDK
is available. If you want to target part of the JDK itself with Specimin,
this means that it's necessary to *relocate* JDK classes (into a non `java.*`
package); see the CF-577 test in the integration tests for an example.

### Reporting issues and contributing

We welcome bug reports. Please make a GitHub issue with the command
that you used to run Specimin and describe what went wrong, and we'll
look into it as soon as we can.

If you'd like to contribute to Specimin, we have a separate document
with [developer documentation](https://github.com/njit-jerse/specimin/DEVELOPERS.md).
