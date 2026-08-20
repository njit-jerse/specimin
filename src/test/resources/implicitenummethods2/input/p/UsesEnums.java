package p;

import com.example.Unsolved;

class UsesEnums {
  void target() {
    RenderType[] all = RenderType.values();
    RenderType one = RenderType.valueOf("DEFAULT");
    Outer.Nested[] nested = Outer.Nested.values();
    Unsolved u = new Unsolved();
  }
}
