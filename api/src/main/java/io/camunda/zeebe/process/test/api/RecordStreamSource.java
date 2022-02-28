package io.camunda.zeebe.process.test.api;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.*;

/**
 * The source of records that have been processed by the test engine
 */
public interface RecordStreamSource {

  /**
   * Gets an iterable of all records that have been published by the test engine.
   *
   * @return an iterable {@link Record}
   */
  Iterable<Record<?>> records();
}
