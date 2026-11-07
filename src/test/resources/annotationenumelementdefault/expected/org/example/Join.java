package org.example;

import static external.TaskType.JOIN;

import external.Component;

@Component(JOIN)
public class Join {

  public boolean execute(Executor e) {
    return e == null;
  }
}
