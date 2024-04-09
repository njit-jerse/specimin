package com.example;

// This kind of declaration will confuse InheritancePreserveVisitor, making it adding Baz to the list of added classes again, which will lead to a possible infinite loop.
public interface Baz extends Comparable<Baz> {

}