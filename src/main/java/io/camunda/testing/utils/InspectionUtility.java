package io.camunda.testing.utils;

import static io.camunda.testing.utils.RecordStreamSourceStore.getRecordStreamSource;

import io.camunda.testing.filters.StreamFilter;

public class InspectionUtility {

  public static ProcessEventInspections findProcessEvents() {
    return new ProcessEventInspections(StreamFilter.processEventRecords(getRecordStreamSource()));
  }
}
