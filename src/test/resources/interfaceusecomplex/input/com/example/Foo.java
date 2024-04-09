package com.example;

class Foo {
   static class Qux<E> implements Baz<E> {

       Baz<? extends E> c;

       public boolean containsAll(Baz<?> coll) {
           return c.containsAll(coll);
       }
    }
}
