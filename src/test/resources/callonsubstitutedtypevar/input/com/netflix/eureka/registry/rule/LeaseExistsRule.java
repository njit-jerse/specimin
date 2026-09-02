package com.netflix.eureka.registry.rule;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.lease.Lease;

public final class LeaseExistsRule {
  public void apply(Lease<InstanceInfo> existingLease) {
    existingLease.getHolder().getStatus();
  }
}
