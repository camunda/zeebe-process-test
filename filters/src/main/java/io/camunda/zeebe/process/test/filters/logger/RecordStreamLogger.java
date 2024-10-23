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
package io.camunda.zeebe.process.test.filters.logger;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.value.ClockRecordValue;
import io.camunda.zeebe.protocol.record.value.CommandDistributionRecordValue;
import io.camunda.zeebe.protocol.record.value.CompensationSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.DeploymentRecordValue;
import io.camunda.zeebe.protocol.record.value.ErrorRecordValue;
import io.camunda.zeebe.protocol.record.value.EscalationRecordValue;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.JobBatchRecordValue;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageBatchRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageCorrelationRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageStartEventSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessEventRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceCreationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceCreationRecordValue.ProcessInstanceCreationStartInstructionValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceMigrationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceModificationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceModificationRecordValue.ProcessInstanceModificationActivateInstructionValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceModificationRecordValue.ProcessInstanceModificationTerminateInstructionValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceModificationRecordValue.ProcessInstanceModificationVariableInstructionValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceResultRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.ResourceDeletionRecordValue;
import io.camunda.zeebe.protocol.record.value.SignalRecordValue;
import io.camunda.zeebe.protocol.record.value.SignalSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.TimerRecordValue;
import io.camunda.zeebe.protocol.record.value.UserRecordValue;
import io.camunda.zeebe.protocol.record.value.UserTaskRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableDocumentRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.FormMetadataValue;
import io.camunda.zeebe.protocol.record.value.deployment.ProcessMetadataValue;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordStreamLogger {

  private static final Logger LOG = LoggerFactory.getLogger(RecordStreamLogger.class);

  private final RecordStream recordStream;
  private final Map<ValueType, Function<Record<?>, String>> valueTypeLoggers = new HashMap<>();

  public RecordStreamLogger(final RecordStreamSource recordStreamSource) {
    recordStream = RecordStream.of(recordStreamSource);
    valueTypeLoggers.put(ValueType.JOB, this::logJobRecordValue);
    valueTypeLoggers.put(ValueType.DEPLOYMENT, this::logDeploymentRecordValue);
    valueTypeLoggers.put(ValueType.PROCESS_INSTANCE, this::logProcessInstanceRecordValue);
    valueTypeLoggers.put(ValueType.INCIDENT, this::logIncidentRecordValue);
    valueTypeLoggers.put(ValueType.MESSAGE, this::logMessageRecordValue);
    valueTypeLoggers.put(ValueType.MESSAGE_BATCH, this::logMessageBatchRecordValue);
    valueTypeLoggers.put(ValueType.MESSAGE_SUBSCRIPTION, this::logMessageSubscriptionRecordValue);
    valueTypeLoggers.put(
        ValueType.PROCESS_MESSAGE_SUBSCRIPTION, this::logProcessMessageSubscriptionRecordValue);
    valueTypeLoggers.put(ValueType.JOB_BATCH, this::logJobBatchRecordValue);
    valueTypeLoggers.put(ValueType.TIMER, this::logTimerRecordValue);
    valueTypeLoggers.put(
        ValueType.MESSAGE_START_EVENT_SUBSCRIPTION,
        this::logMessageStartEventSubscriptionRecordValue);
    valueTypeLoggers.put(ValueType.VARIABLE, this::logVariableRecordValue);
    valueTypeLoggers.put(ValueType.VARIABLE_DOCUMENT, this::logVariableDocumentRecordValue);
    valueTypeLoggers.put(
        ValueType.PROCESS_INSTANCE_CREATION, this::logProcessInstanceCreationRecordValue);
    valueTypeLoggers.put(ValueType.ERROR, this::logErrorRecordValue);
    valueTypeLoggers.put(
        ValueType.PROCESS_INSTANCE_RESULT, this::logProcessInstanceResultRecordValue);
    valueTypeLoggers.put(ValueType.PROCESS, this::logProcessRecordValue);
    valueTypeLoggers.put(ValueType.PROCESS_EVENT, this::logProcessEventRecordValue);
    valueTypeLoggers.put(ValueType.ESCALATION, this::logEscalationRecordValue);

    // These records don't have any interesting extra information for the user to log
    valueTypeLoggers.put(ValueType.DEPLOYMENT_DISTRIBUTION, Object::toString);
    valueTypeLoggers.put(ValueType.SBE_UNKNOWN, Object::toString);
    valueTypeLoggers.put(ValueType.NULL_VAL, Object::toString);

    // DMN will not be part of the initial 1.4 release
    valueTypeLoggers.put(ValueType.DECISION, Object::toString);
    valueTypeLoggers.put(ValueType.DECISION_REQUIREMENTS, Object::toString);
    valueTypeLoggers.put(ValueType.DECISION_EVALUATION, Object::toString);

    // checkpoint isn't meant to be read by the engine
    valueTypeLoggers.put(ValueType.CHECKPOINT, Object::toString);

    valueTypeLoggers.put(
        ValueType.PROCESS_INSTANCE_MODIFICATION, this::logProcessInstanceModificationRecordValue);

    valueTypeLoggers.put(ValueType.SIGNAL_SUBSCRIPTION, this::logSignalSubscriptionRecordValue);
    valueTypeLoggers.put(ValueType.SIGNAL, this::logSignalRecordValue);

    valueTypeLoggers.put(ValueType.RESOURCE_DELETION, this::logResourceDeletionRecordValue);

    valueTypeLoggers.put(ValueType.COMMAND_DISTRIBUTION, this::logCommandDistributionRecordValue);
    valueTypeLoggers.put(ValueType.PROCESS_INSTANCE_BATCH, Object::toString);
    valueTypeLoggers.put(ValueType.FORM, this::logFormRecordValue);
    valueTypeLoggers.put(ValueType.USER_TASK, this::logUserTaskRecordValue);
    valueTypeLoggers.put(
        ValueType.PROCESS_INSTANCE_MIGRATION, this::logProcessInstanceMigrationRecordValue);
    valueTypeLoggers.put(
        ValueType.COMPENSATION_SUBSCRIPTION, this::logCompensationSubscriptionRecordValue);
    valueTypeLoggers.put(ValueType.MESSAGE_CORRELATION, this::logMessageCorrelationRecordValue);
    valueTypeLoggers.put(ValueType.USER, this::logUsersRecordValue);
    valueTypeLoggers.put(ValueType.CLOCK, this::logClockRecordValue);
    valueTypeLoggers.put(ValueType.AUTHORIZATION, Object::toString);
    valueTypeLoggers.put(ValueType.ROLE, Object::toString);
    valueTypeLoggers.put(ValueType.TENANT, Object::toString);
    valueTypeLoggers.put(ValueType.SCALE, Object::toString);
    valueTypeLoggers.put(ValueType.GROUP, Object::toString);
    valueTypeLoggers.put(ValueType.MAPPING, Object::toString);
  }

  public void log() {
    final StringBuilder stringBuilder = new StringBuilder().append(System.lineSeparator());
    logRecords(stringBuilder);
  }

  private void logRecords(final StringBuilder stringBuilder) {
    stringBuilder.append("The following records have been recorded during this test:");
    recordStream.records().forEach(record -> stringBuilder.append(logRecord(record)));

    LOG.info(stringBuilder.toString());
  }

  protected String logRecord(final Record<?> record) {
    return logGenericRecord(record) + logRecordDetails(record);
  }

  private String logGenericRecord(final Record<?> record) {
    return System.lineSeparator()
        + String.format("| %-20s", record.getRecordType())
        + String.format("%-35s", record.getValueType())
        + String.format("%-30s| ", record.getIntent());
  }

  private String logRecordDetails(final Record<?> record) {
    return valueTypeLoggers.getOrDefault(record.getValueType(), var -> "").apply(record);
  }

  private String logJobRecordValue(final Record<?> record) {
    final JobRecordValue value = (JobRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    // These fields are empty for commands
    if (record.getRecordType().equals(RecordType.EVENT)) {
      joiner.add(String.format("(Element id: %s)", value.getElementId()));
      joiner.add(String.format("(Job type: %s)", value.getType()));
      if (!value.getVariables().isEmpty()) {
        joiner.add(logVariables(value.getVariables()));
      }
    }
    return joiner.toString();
  }

  private String logDeploymentRecordValue(final Record<?> record) {
    final DeploymentRecordValue value = (DeploymentRecordValue) record.getValue();
    final StringBuilder stringBuilder = new StringBuilder();
    if (!value.getResources().isEmpty()) {
      final StringJoiner joiner = new StringJoiner(", ", "[", "]");
      value.getResources().forEach(resource -> joiner.add(resource.getResourceName()));
      stringBuilder.append(String.format("(Processes: %s)", joiner));
    }
    return stringBuilder.toString();
  }

  private String logProcessInstanceRecordValue(final Record<?> record) {
    final ProcessInstanceRecordValue value = (ProcessInstanceRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Element id: %s)", value.getElementId()));
    joiner.add(String.format("(Element type: %s)", value.getBpmnElementType()));
    joiner.add(String.format("(Event type: %s)", value.getBpmnEventType()));
    joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    return joiner.toString();
  }

  private String logIncidentRecordValue(final Record<?> record) {
    final IncidentRecordValue value = (IncidentRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    if (record.getRecordType().equals(RecordType.EVENT)) {
      joiner.add(String.format("(Element id: %s)", value.getElementId()));
      joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    }
    return joiner.toString();
  }

  private String logMessageRecordValue(final Record<?> record) {
    final MessageRecordValue value = (MessageRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Message name: %s)", value.getName()));
    joiner.add(String.format("(Correlation key: %s)", value.getCorrelationKey()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logMessageBatchRecordValue(final Record<?> record) {
    final MessageBatchRecordValue value = (MessageBatchRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Message Keys: %s)", value.getMessageKeys()));
    return joiner.toString();
  }

  private String logMessageSubscriptionRecordValue(final Record<?> record) {
    final MessageSubscriptionRecordValue value = (MessageSubscriptionRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Message name: %s)", value.getMessageName()));
    joiner.add(String.format("(Correlation key: %s)", value.getCorrelationKey()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logProcessMessageSubscriptionRecordValue(final Record<?> record) {
    final ProcessMessageSubscriptionRecordValue value =
        (ProcessMessageSubscriptionRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Message name: %s)", value.getMessageName()));

    // These fields are empty for commands
    if (record.getRecordType().equals(RecordType.EVENT)) {
      joiner.add(String.format("(Correlation key: %s)", value.getCorrelationKey()));
      joiner.add(String.format("(Element id: %s)", value.getElementId()));
    }

    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logJobBatchRecordValue(final Record<?> record) {
    final JobBatchRecordValue value = (JobBatchRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Worker: %s)", value.getWorker()));
    joiner.add(String.format("(Job type: %s)", value.getType()));
    return joiner.toString();
  }

  private String logTimerRecordValue(final Record<?> record) {
    final TimerRecordValue value = (TimerRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Element id: %s)", value.getTargetElementId()));
    joiner.add(String.format("(Due date: %s)", new Date(value.getDueDate())));
    return joiner.toString();
  }

  private String logMessageStartEventSubscriptionRecordValue(final Record<?> record) {
    final MessageStartEventSubscriptionRecordValue value =
        (MessageStartEventSubscriptionRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    joiner.add(String.format("(Start event id: %s)", value.getStartEventId()));
    joiner.add(String.format("(Message name: %s)", value.getMessageName()));
    joiner.add(String.format("(Correlation key: %s)", value.getCorrelationKey()));
    return joiner.toString();
  }

  private String logVariableRecordValue(final Record<?> record) {
    final VariableRecordValue value = (VariableRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Name: %s)", value.getName()));
    joiner.add(String.format("(Value: %s)", value.getValue()));
    return joiner.toString();
  }

  private String logVariableDocumentRecordValue(final Record<?> record) {
    final VariableDocumentRecordValue value = (VariableDocumentRecordValue) record.getValue();
    return logVariables(value.getVariables());
  }

  private String logProcessInstanceCreationRecordValue(final Record<?> record) {
    final ProcessInstanceCreationRecordValue value =
        (ProcessInstanceCreationRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    if (!value.getVariables().isEmpty()) {
      joiner.add(logVariables(value.getVariables()));
    }
    joiner.add(logStartInstructions(value.getStartInstructions()));
    return joiner.toString();
  }

  private String logErrorRecordValue(final Record<?> record) {
    final ErrorRecordValue value = (ErrorRecordValue) record.getValue();
    return String.format("(Exception message: %s)", value.getExceptionMessage());
  }

  private String logProcessInstanceResultRecordValue(final Record<?> record) {
    final ProcessInstanceResultRecordValue value =
        (ProcessInstanceResultRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logProcessRecordValue(final Record<?> record) {
    final ProcessMetadataValue value = (ProcessMetadataValue) record.getValue();
    return String.format("(Process: %s)", value.getResourceName());
  }

  private String logProcessEventRecordValue(final Record<?> record) {
    final ProcessEventRecordValue value = (ProcessEventRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Target element id: %s)", value.getTargetElementId()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logEscalationRecordValue(final Record<?> record) {
    final EscalationRecordValue value = (EscalationRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process id: %s)", value.getProcessInstanceKey()));
    joiner.add(String.format("(Escalation code: %s)", value.getEscalationCode()));
    joiner.add(String.format("(Throw element id: %s)", value.getThrowElementId()));
    joiner.add(String.format("(Catch element id: %s)", value.getCatchElementId()));
    return joiner.toString();
  }

  private String logProcessInstanceModificationRecordValue(final Record<?> record) {
    final ProcessInstanceModificationRecordValue value =
        (ProcessInstanceModificationRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Target process instance: %d)", value.getProcessInstanceKey()));
    joiner.add(logActivateInstructions(value.getActivateInstructions()));
    joiner.add(logTerminateInstructions(value.getTerminateInstructions()));
    return joiner.toString();
  }

  protected String logVariables(final Map<String, Object> variables) {
    if (variables.isEmpty()) {
      return "";
    }

    final StringJoiner joiner = new StringJoiner(", ", "[", "]");
    variables.forEach((key, value) -> joiner.add(key + " -> " + value));
    return String.format("(Variables: %s)", joiner);
  }

  private String logStartInstructions(
      final List<ProcessInstanceCreationStartInstructionValue> startInstructions) {
    if (startInstructions.isEmpty()) {
      return "(default start)";
    } else {
      return startInstructions.stream()
          .map(ProcessInstanceCreationStartInstructionValue::getElementId)
          .collect(Collectors.joining(", ", "(starting before elements: ", ")"));
    }
  }

  private String logActivateInstructions(
      final List<ProcessInstanceModificationActivateInstructionValue> instructions) {
    if (instructions.isEmpty()) {
      return "(no activate)";
    } else {
      return instructions.stream()
          .map(this::logActivateInstruction)
          .collect(Collectors.joining(", ", "(activating elements: ", ")"));
    }
  }

  private String logActivateInstruction(
      final ProcessInstanceModificationActivateInstructionValue instruction) {
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Target element id: %s)", instruction.getElementId()));
    joiner.add(String.format("(Ancestor scope key: %d)", instruction.getAncestorScopeKey()));
    joiner.add(logVariableInstructions(instruction.getVariableInstructions()));
    return joiner.toString();
  }

  private String logVariableInstructions(
      final List<ProcessInstanceModificationVariableInstructionValue> variables) {
    if (variables.isEmpty()) {
      return "(no variables)";
    } else {
      return variables.stream()
          .map(this::logVariableInstruction)
          .collect(Collectors.joining(", ", "(with variable instruction: ", ")"));
    }
  }

  private String logVariableInstruction(
      final ProcessInstanceModificationVariableInstructionValue variable) {
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Target element: %s", variable.getElementId()));

    joiner.add(logVariables(variable.getVariables()));
    return joiner.toString();
  }

  private String logTerminateInstructions(
      final List<ProcessInstanceModificationTerminateInstructionValue> instructions) {
    if (instructions.isEmpty()) {
      return "(no terminate)";
    } else {
      return instructions.stream()
          .map(ProcessInstanceModificationTerminateInstructionValue::getElementInstanceKey)
          .map(String::valueOf)
          .collect(Collectors.joining(", ", "(terminating elements: ", ")"));
    }
  }

  private String logSignalSubscriptionRecordValue(final Record<?> record) {
    final SignalSubscriptionRecordValue value = (SignalSubscriptionRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    joiner.add(String.format("(Catch event id: %s)", value.getCatchEventId()));
    joiner.add(String.format("(Signal name: %s)", value.getSignalName()));
    joiner.add(String.format("(Catch event instance key: %s)", value.getCatchEventInstanceKey()));
    return joiner.toString();
  }

  private String logSignalRecordValue(final Record<?> record) {
    final SignalRecordValue value = (SignalRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Signal name: %s)", value.getSignalName()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logResourceDeletionRecordValue(final Record<?> record) {
    final ResourceDeletionRecordValue value = (ResourceDeletionRecordValue) record.getValue();
    return String.format("(Resource key: %d", value.getResourceKey());
  }

  private String logCommandDistributionRecordValue(final Record<?> record) {
    final CommandDistributionRecordValue value = (CommandDistributionRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(To partition id: %d)", value.getPartitionId()));
    joiner.add(String.format("(Value type: %s)", value.getValueType()));
    return joiner.toString();
  }

  private String logFormRecordValue(final Record<?> record) {
    final FormMetadataValue value = (FormMetadataValue) record.getValue();
    return String.format("(Form: %s)", value.getResourceName());
  }

  private String logUserTaskRecordValue(final Record<?> record) {
    final UserTaskRecordValue value = (UserTaskRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    // These fields are empty for commands
    if (record.getRecordType().equals(RecordType.EVENT)) {
      joiner.add(String.format("(Element id: %s)", value.getElementId()));
    }
    return joiner.toString();
  }

  private String logProcessInstanceMigrationRecordValue(final Record<?> record) {
    final ProcessInstanceMigrationRecordValue value =
        (ProcessInstanceMigrationRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    // These fields are empty for commands
    if (record.getRecordType().equals(RecordType.EVENT)) {
      joiner.add(String.format("(Process instance key: %d)", value.getProcessInstanceKey()));
      joiner.add(
          String.format(
              "(Target process definition key: %d)", value.getTargetProcessDefinitionKey()));
      joiner.add(
          String.format(
              "(Mapping instructions: %s)",
              value.getMappingInstructions().stream()
                  .map(i -> i.getSourceElementId() + " -> " + i.getTargetElementId())
                  .collect(Collectors.joining(", "))));
    }
    return joiner.toString();
  }

  private String logCompensationSubscriptionRecordValue(final Record<?> record) {
    final CompensationSubscriptionRecordValue value =
        (CompensationSubscriptionRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process instance key: %d)", value.getProcessInstanceKey()));
    joiner.add(String.format("(Process definition key: %d)", value.getProcessDefinitionKey()));
    joiner.add(String.format("(Compensable activity id: %s)", value.getCompensableActivityId()));
    return joiner.toString();
  }

  private String logMessageCorrelationRecordValue(final Record<?> record) {
    final MessageCorrelationRecordValue value = (MessageCorrelationRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process instance key: %d)", value.getProcessInstanceKey()));
    joiner.add(String.format("(Message name: %s)", value.getName()));
    joiner.add(String.format("(Message correlation key id: %s)", value.getCorrelationKey()));
    return joiner.toString();
  }

  protected Map<ValueType, Function<Record<?>, String>> getValueTypeLoggers() {
    return valueTypeLoggers;
  }

  private String logUsersRecordValue(final Record<?> record) {
    final UserRecordValue value = (UserRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Username: %s)", value.getUsername()));
    joiner.add(String.format("(Name: %s)", value.getName()));
    joiner.add(String.format("(Email: %s)", value.getEmail()));
    joiner.add(String.format("(Password: %s)", value.getPassword()));
    return joiner.toString();
  }

  private String logClockRecordValue(final Record<?> record) {
    final ClockRecordValue value = (ClockRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Time: %d", value.getTime()));
    return joiner.toString();
  }
}
