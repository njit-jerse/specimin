package org.example;

import javax.ws.rs.core.Response;

public class Target {
  public Response target() {
    return Response.ok("body").build();
  }
}
