package org.example;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

public class Target {

  public void target(@Context UriInfo unused) {}
}
