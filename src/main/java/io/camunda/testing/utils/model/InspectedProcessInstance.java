package io.camunda.testing.utils.model;

public class InspectedProcessInstance {

  private final long processInstanceKey;

  public InspectedProcessInstance(final long processInstanceKey) {
    this.processInstanceKey = processInstanceKey;
  }

  public long getProcessInstanceKey() {
    return processInstanceKey;
  }
}
