package org.example;

import external.TaskUtils;
import java.util.List;
import java.util.stream.Collectors;

public class ExclusiveJoin {

  public List<String> appendIterations(List<String> joinOn, int iteration) {
    return joinOn.stream()
        .map(name -> TaskUtils.appendIteration(name, iteration))
        .collect(Collectors.toList());
  }
}
