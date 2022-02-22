package io.camunda.zeebe.process.test.api;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.*;

public interface RecordStreamSource {

  /** @return an iterable of all {@link Record} */
  Iterable<Record<?>> records();
}
