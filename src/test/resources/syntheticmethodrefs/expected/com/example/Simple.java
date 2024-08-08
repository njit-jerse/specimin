package com.example;

public class Simple {

    public void foo() {
        Foo.mapList(Bar::noParamVoid);
        Foo.mapList2(Bar::noParamNonVoid);
        Foo.mapList3(Bar::oneParamVoid);
        Foo.mapList4(Bar::oneParamNonVoid);
        Foo.mapList5(Bar::twoParamVoid);
        Foo.mapList6(Bar::twoParamNonVoid);
        Foo.mapList7(Bar::threeParamVoid);
        Foo.mapList8(Bar::threeParamNonVoid);
    }
}
