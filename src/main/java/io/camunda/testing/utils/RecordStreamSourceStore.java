package io.camunda.testing.utils;

import org.camunda.community.eze.RecordStreamSource;

public abstract class RecordStreamSourceStore {

  static ThreadLocal<RecordStreamSource> recordStreamSource = new ThreadLocal<>();

  public static void init(final RecordStreamSource recordStreamSource) {
    RecordStreamSourceStore.recordStreamSource.set(recordStreamSource);
  }

  public static void reset() {
    recordStreamSource.remove();
  }

  public static RecordStreamSource getRecordStreamSource() {
    if (recordStreamSource.get() == null) {
      throw new AssertionError(
          "No RecordStreamSource is set. Please use @ZeebeAssertions extension and "
              + "declare a RecordStreamSource field");
    }
    return recordStreamSource.get();
  }
}
