package retrofit2;

import javax.annotation.Nullable;

final class RequestFactory {
  @Nullable private final ParameterHandler<?>[] parameterHandlers = null;

  void create(Object[] args) {
    @SuppressWarnings("unchecked")
    ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;
    if (args.length != handlers.length) {
      throw new IllegalArgumentException();
    }
  }
}
