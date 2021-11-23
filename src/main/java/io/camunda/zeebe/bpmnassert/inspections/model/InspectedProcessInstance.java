package io.camunda.zeebe.bpmnassert.inspections.model;

public class InspectedProcessInstance {

  private final long processInstanceKey;

  public InspectedProcessInstance(final long processInstanceKey) {
    this.processInstanceKey = processInstanceKey;
  }

  public long getProcessInstanceKey() {
    return processInstanceKey;
  }
}
