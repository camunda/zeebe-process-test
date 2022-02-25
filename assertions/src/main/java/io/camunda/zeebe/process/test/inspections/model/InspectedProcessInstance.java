package io.camunda.zeebe.process.test.inspections.model;

/**
 * Helper object to gain access to {@link
 * io.camunda.zeebe.process.test.assertions.ProcessInstanceAssert}. All this object does is wrap a
 * process instance key. This object is required to use {@link
 * io.camunda.zeebe.process.test.assertions.BpmnAssert#assertThat(InspectedProcessInstance)}.
 *
 * <p>Without this object we would need a method {@code assertThat(long)}. This would conflict with
 * the similar method in AssertJ, which is often imported in a static way.
 *
 * <p>Besides that it would also conflict with other assertions, such as {@link
 * io.camunda.zeebe.process.test.assertions.IncidentAssert}, which could also be accessed using a
 * long.
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
