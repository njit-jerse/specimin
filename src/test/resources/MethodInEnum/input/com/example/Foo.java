package com.example;

class Foo {

    private enum STATUS{
        ON {
            // this method should be emptied. Sadly we can't figure out why it is preserved in the final output.
            // The good thing is that UnsolvedSymbolVisitor will make sure that everything inside this method resolved.
            public void testing() {
                throw new RuntimeException();
            }
        },
        OFF {
            public void testing() {
                throw new RuntimeException();
            }
        };
        public abstract void testing();
    }

    private void bar() {
        STATUS.ON.testing();
    }
}
