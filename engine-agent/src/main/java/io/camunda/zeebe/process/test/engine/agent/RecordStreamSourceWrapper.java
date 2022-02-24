package io.camunda.zeebe.process.test.engine.agent;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import java.util.ArrayList;
import java.util.List;

public class RecordStreamSourceWrapper {

  private final List<String> mappedRecords = new ArrayList<>();
  private final RecordStreamSource recordStreamSource;

  public RecordStreamSourceWrapper(final RecordStreamSource recordStreamSource) {
    this.recordStreamSource = recordStreamSource;
  }

  public List<String> getMappedRecords() {
    synchronized (mappedRecords) {
      mappedRecords.clear();
      recordStreamSource.records().forEach(record -> mappedRecords.add(record.toJson()));
    }
    return new ArrayList<>(mappedRecords);
  }
}
