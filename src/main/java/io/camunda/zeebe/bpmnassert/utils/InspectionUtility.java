package io.camunda.zeebe.bpmnassert.utils;

import io.camunda.zeebe.bpmnassert.filters.StreamFilter;

public class InspectionUtility {

  public static ProcessEventInspections findProcessEvents() {
    return new ProcessEventInspections(StreamFilter.processEventRecords(
        RecordStreamSourceStore.getRecordStreamSource()));
  }

  public static ProcessInstanceInspections findProcessInstances() {
    return new ProcessInstanceInspections(StreamFilter.processInstance(
        RecordStreamSourceStore.getRecordStreamSource()));
  }
}
