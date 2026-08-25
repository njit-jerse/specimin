package retrofit2;

final class RequestFactory {
  Object create(ParameterHandler[] handlers) {
    return handlers.clone();
  }
}
