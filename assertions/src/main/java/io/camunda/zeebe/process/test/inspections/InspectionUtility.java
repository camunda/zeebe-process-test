package io.camunda.zeebe.process.test.inspections;

import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.StreamFilter;

public class InspectionUtility {

  public static ProcessEventInspections findProcessEvents() {
    return new ProcessEventInspections(
        StreamFilter.processEventRecords(BpmnAssert.getRecordStreamSource()));
  }

  public static ProcessInstanceInspections findProcessInstances() {
    return new ProcessInstanceInspections(
        StreamFilter.processInstance(BpmnAssert.getRecordStreamSource()));
  }
}
