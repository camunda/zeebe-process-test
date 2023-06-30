/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  public static ProcessDefinitionRecordStreamFilter processDefinitionRecords(
      final RecordStream recordStream) {
    return new ProcessDefinitionRecordStreamFilter(recordStream.processRecords());
  }
}
