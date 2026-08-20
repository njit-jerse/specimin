package p;

import com.example.Unsolved;

class UsesEnums {
  void target(Unsolved u) {
    Unsolved.consume(RenderType.values());
    u.take(RenderType.valueOf("DEFAULT"));
  }
}
