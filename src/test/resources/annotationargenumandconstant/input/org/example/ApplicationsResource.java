package org.example;

import external.EurekaAccept;
import external.HeaderParam;

public class ApplicationsResource {
  private static final String HEADER_ACCEPT = "Accept";

  public void getContainers(
      @HeaderParam(EurekaAccept.HTTP_X_EUREKA_ACCEPT) String eurekaAccept,
      @HeaderParam(HEADER_ACCEPT) String acceptHeader) {}
}
