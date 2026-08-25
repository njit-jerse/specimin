package retrofit2;

import java.util.function.IntFunction;
import java.util.function.Supplier;

final class RequestFactory {
  void create(ParameterHandler[] handlers) {
    Supplier<Object> s = handlers::clone;
    IntFunction<ParameterHandler[]> f = ParameterHandler[]::new;
    s.get();
    f.apply(1);
  }
}
