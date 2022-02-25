package io.camunda.zeebe.process.test.inspections;

import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.StreamFilter;

/**
 * This class is a utility class for finding process instances that have been started without being
 * manually triggered (e.g. timer start events or call activities).
 */
public class InspectionUtility {

  public static ProcessEventInspections findProcessEvents() {
    return new ProcessEventInspections(
        StreamFilter.processEventRecords(BpmnAssert.getRecordStream()));
  }

  public static ProcessInstanceInspections findProcessInstances() {
    return new ProcessInstanceInspections(
        StreamFilter.processInstance(BpmnAssert.getRecordStream()));
  }
}
