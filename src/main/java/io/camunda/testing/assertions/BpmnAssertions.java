package io.camunda.testing.assertions;

import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.camunda.community.eze.RecordStreamSource;

public abstract class BpmnAssertions {

  static ThreadLocal<RecordStreamSource> recordStreamSource = new ThreadLocal<>();

  public static void init(RecordStreamSource recordStreamSource) {
    BpmnAssertions.recordStreamSource.set(recordStreamSource);
  }

  public static void reset() {
    recordStreamSource.remove();
  }

  public static ProcessInstanceAssertions assertThat(final ProcessInstanceEvent instanceEvent) {
    return new ProcessInstanceAssertions(instanceEvent, getRecordStreamSource());
  }

  private static RecordStreamSource getRecordStreamSource() {
    if (recordStreamSource.get() == null) {
      throw new AssertionError(
          "No RecordStreamSource is set. Please use @ZeebeAssertions extension and "
              + "declare a RecordStreamSource field");
    }
    return recordStreamSource.get();
  }
}
