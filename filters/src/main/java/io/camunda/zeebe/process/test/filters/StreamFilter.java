package io.camunda.zeebe.process.test.filters;

public class StreamFilter {

  public static ProcessInstanceRecordStreamFilter processInstance(final RecordStream recordStream) {
    return new ProcessInstanceRecordStreamFilter(recordStream.processInstanceRecords());
  }

  public static ProcessMessageSubscriptionRecordStreamFilter processMessageSubscription(
      final RecordStream recordStream) {
    return new ProcessMessageSubscriptionRecordStreamFilter(
        recordStream.processMessageSubscriptionRecords());
  }

  public static VariableRecordStreamFilter variable(final RecordStream recordStream) {
    return new VariableRecordStreamFilter(recordStream.variableRecords());
  }

  public static MessageRecordStreamFilter message(final RecordStream recordStream) {
    return new MessageRecordStreamFilter(recordStream.messageRecords());
  }

  public static IncidentRecordStreamFilter incident(final RecordStream recordStream) {
    return new IncidentRecordStreamFilter(recordStream.incidentRecords());
  }

  public static MessageStartEventSubscriptionStreamFilter messageStartEventSubscription(
      final RecordStream recordStream) {
    return new MessageStartEventSubscriptionStreamFilter(
        recordStream.messageStartEventSubscriptionRecords());
  }

  public static ProcessEventRecordStreamFilter processEventRecords(
      final RecordStream recordStream) {
    return new ProcessEventRecordStreamFilter(recordStream.records());
  }

  public static JobRecordStreamFilter jobRecords(final RecordStream recordStream) {
    return new JobRecordStreamFilter(recordStream.jobRecords());
  }

  public static TimerRecordStreamFilter timerRecords(final RecordStream recordStream) {
    return new TimerRecordStreamFilter(recordStream.timerRecords());
  }
}
