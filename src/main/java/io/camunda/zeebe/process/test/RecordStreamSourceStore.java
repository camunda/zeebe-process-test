package io.camunda.zeebe.process.test;

import io.camunda.zeebe.process.test.testengine.RecordStreamSource;

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
