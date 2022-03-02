package io.camunda.zeebe.process.test.inspections.model;

/**
 * Helper object to gain access to {@link
 * io.camunda.zeebe.process.test.assertions.ProcessInstanceAssert}. All this object does is wrap a
 * process instance key. This object is required to use {@link
 * io.camunda.zeebe.process.test.assertions.BpmnAssert#assertThat(InspectedProcessInstance)}.
 *
 * <p>The helper object enabled asserting process instances which were not started with a command
 * send by the client (e.g. by a timer or a call activity).
 */
public class InspectedProcessInstance {

  private final long processInstanceKey;

  public InspectedProcessInstance(final long processInstanceKey) {
    this.processInstanceKey = processInstanceKey;
  }

  /**
   * Get the process instance key.
   *
   * @return the wrapped process instance key
   */
  public long getProcessInstanceKey() {
    return processInstanceKey;
  }
}
