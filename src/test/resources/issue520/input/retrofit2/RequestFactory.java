package retrofit2;

import javax.annotation.Nullable;

final class RequestFactory {
  @Nullable private final ParameterHandler<?>[] parameterHandlers;

  RequestFactory(Builder builder) {
    parameterHandlers = builder.parameterHandlers;
  }

  void create(Object[] args) {
    @SuppressWarnings("unchecked")
    ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;
    if (args.length != handlers.length) {
      throw new IllegalArgumentException();
    }
  }

  static final class Builder {
    @Nullable ParameterHandler<?>[] parameterHandlers;

    RequestFactory build() {
      parameterHandlers = new ParameterHandler<?>[1];
      return new RequestFactory(this);
    }
  }
}
