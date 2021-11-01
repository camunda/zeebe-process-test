package io.camunda.testing.assertions;

import org.camunda.community.eze.RecordStreamSource;

public abstract class BpmnAssertions {

  static ThreadLocal<RecordStreamSource> recordStreamSource = new ThreadLocal<>();

  public static void init(RecordStreamSource recordStreamSource) {
    BpmnAssertions.recordStreamSource.set(recordStreamSource);
  }
}
