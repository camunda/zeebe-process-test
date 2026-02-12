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

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.protocol.record.Agent;
import io.camunda.zeebe.protocol.record.ImmutableRecord;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.RecordValue;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceCreationIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceMigrationIntent;
import io.camunda.zeebe.protocol.record.value.ImmutableJobRecordValue;
import io.camunda.zeebe.protocol.record.value.ImmutableProcessInstanceCreationRecordValue;
import io.camunda.zeebe.protocol.record.value.ImmutableProcessInstanceCreationStartInstructionValue;
import io.camunda.zeebe.protocol.record.value.ImmutableProcessInstanceMigrationMappingInstructionValue;
import io.camunda.zeebe.protocol.record.value.ImmutableProcessInstanceMigrationRecordValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RecordStreamLoggerTest {

  private static final Map<String, Object> TYPED_TEST_VARIABLES = new HashMap<>();

  static {
    TYPED_TEST_VARIABLES.put("stringProperty", "stringValue");
    TYPED_TEST_VARIABLES.put("numberProperty", 123);
    TYPED_TEST_VARIABLES.put("booleanProperty", true);
    TYPED_TEST_VARIABLES.put("complexProperty", Arrays.asList("Element 1", "Element 2"));
    TYPED_TEST_VARIABLES.put("nullProperty", null);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideVariables")
  void testLogVariable(final String key, final Object value) {
    final RecordStreamLogger logger = new RecordStreamLogger(null);

    final String result = logger.logVariables(Collections.singletonMap(key, value));

    assertThat(result).isEqualTo(String.format("(Variables: [%s -> %s])", key, value));
  }

  @Test
  void testLogUnknownRecordAsJson() {
    final RecordStreamLogger logger = new RecordStreamLogger(null);
    final UnknownRecordValueRecord record = new UnknownRecordValueRecord();

    final String result = logger.logRecord(record);

    assertThat(result)
        .contains(record.getRecordType().name())
        .contains(record.getValueType().name())
        .contains(record.getIntent().name())
        .describedAs("Expect that the log contains the full json representation of the value")
        .contains(record.getValue().toJson());
  }

  @Test
  void testLogMultipleVariables() {
    final RecordStreamLogger logger = new RecordStreamLogger(null);

    final String result = logger.logVariables(TYPED_TEST_VARIABLES);

    assertThat(result)
        .contains("Variables: ")
        .contains("stringProperty -> stringValue")
        .contains("numberProperty -> 123")
        .contains("booleanProperty -> true")
        .contains("complexProperty -> [Element 1, Element 2]")
        .contains("nullProperty -> null");
  }

  @ParameterizedTest(name = "logged record {0} should contain {1}")
  @MethodSource("loggedRecordContains")
  void testLoggedRecordContains(final Record<?> typedRecord, final String expected) {
    final RecordStreamLogger logger = new RecordStreamLogger(null);
    final String result = logger.logRecord(typedRecord);
    assertThat(result).contains(expected);
  }

  private static Stream<Arguments> provideVariables() {
    return TYPED_TEST_VARIABLES.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  private static Stream<Arguments> loggedRecordContains() {
    return Stream.of(
        Arguments.of(
            Named.of(
                "PROCESS_INSTANCE_CREATION starting at default none start event",
                ImmutableRecord.builder()
                    .withRecordType(RecordType.EVENT)
                    .withValueType(ValueType.PROCESS_INSTANCE_CREATION)
                    .withIntent(ProcessInstanceCreationIntent.CREATED)
                    .withKey(123)
                    .withValue(
                        ImmutableProcessInstanceCreationRecordValue.builder()
                            .withBpmnProcessId("PROCESS")
                            .withVersion(1)
                            .build())
                    .build()),
            "(Process id: PROCESS), (default start)"),
        Arguments.of(
            Named.of(
                "PROCESS_INSTANCE_CREATION starting at default none start event with variables",
                ImmutableRecord.builder()
                    .withRecordType(RecordType.EVENT)
                    .withValueType(ValueType.PROCESS_INSTANCE_CREATION)
                    .withIntent(ProcessInstanceCreationIntent.CREATED)
                    .withKey(123)
                    .withValue(
                        ImmutableProcessInstanceCreationRecordValue.builder()
                            .withBpmnProcessId("PROCESS")
                            .withVersion(1)
                            .withVariables(
                                new HashMap<String, Object>() {
                                  {
                                    put("key", "value");
                                  }
                                })
                            .build())
                    .build()),
            "(Process id: PROCESS), (Variables: [key -> value]), (default start)"),
        Arguments.of(
            Named.of(
                "PROCESS_INSTANCE_CREATEION starting at given elements",
                ImmutableRecord.builder()
                    .withRecordType(RecordType.EVENT)
                    .withValueType(ValueType.PROCESS_INSTANCE_CREATION)
                    .withIntent(ProcessInstanceCreationIntent.CREATED)
                    .withKey(123)
                    .withValue(
                        ImmutableProcessInstanceCreationRecordValue.builder()
                            .withBpmnProcessId("PROCESS")
                            .withVersion(1)
                            .addStartInstruction(
                                ImmutableProcessInstanceCreationStartInstructionValue.builder()
                                    .withElementId("USER_TASK")
                                    .build())
                            .addStartInstruction(
                                ImmutableProcessInstanceCreationStartInstructionValue.builder()
                                    .withElementId("SERVICE_TASK")
                                    .build())
                            .build())
                    .build()),
            "(Process id: PROCESS), (starting before elements: USER_TASK, SERVICE_TASK)"),
        Arguments.of(
            Named.of(
                "JOB with element id and type",
                ImmutableRecord.builder()
                    .withRecordType(RecordType.EVENT)
                    .withValueType(ValueType.JOB)
                    .withIntent(JobIntent.COMPLETED)
                    .withKey(123)
                    .withValue(
                        ImmutableJobRecordValue.builder()
                            .withType("task")
                            .withElementId("serviceTask1")
                            .build())
                    .build()),
            "(Element id: serviceTask1), (Job type: task)"),
        Arguments.of(
            Named.of(
                "Process instance migration with mapping instructions",
                ImmutableRecord.builder()
                    .withRecordType(RecordType.EVENT)
                    .withValueType(ValueType.PROCESS_INSTANCE_MIGRATION)
                    .withIntent(ProcessInstanceMigrationIntent.MIGRATED)
                    .withKey(123)
                    .withValue(
                        ImmutableProcessInstanceMigrationRecordValue.builder()
                            .withProcessInstanceKey(123)
                            .withTargetProcessDefinitionKey(456)
                            .addMappingInstruction(
                                ImmutableProcessInstanceMigrationMappingInstructionValue.builder()
                                    .withSourceElementId("A")
                                    .withTargetElementId("A")
                                    .build())
                            .addMappingInstruction(
                                ImmutableProcessInstanceMigrationMappingInstructionValue.builder()
                                    .withSourceElementId("B")
                                    .withTargetElementId("C")
                                    .build())
                            .build())
                    .build()),
            "(Process instance key: 123), (Target process definition key: 456), (Mapping instructions: A -> A, B -> C)"));
  }

  private static class UnknownRecordValue implements RecordValue {

    @Override
    public String toJson() {
      return "{ \"foo\": \"bar\" }";
    }
  }

  private static class UnknownRecordValueRecord implements Record<UnknownRecordValue> {

    @Override
    public long getPosition() {
      return 0;
    }

    @Override
    public long getSourceRecordPosition() {
      return 0;
    }

    @Override
    public long getKey() {
      return 0;
    }

    @Override
    public long getTimestamp() {
      return 0;
    }

    @Override
    public Intent getIntent() {
      return Intent.UNKNOWN;
    }

    @Override
    public int getPartitionId() {
      return 0;
    }

    @Override
    public RecordType getRecordType() {
      return RecordType.EVENT;
    }

    @Override
    public RejectionType getRejectionType() {
      return null;
    }

    @Override
    public String getRejectionReason() {
      return "";
    }

    @Override
    public String getBrokerVersion() {
      return "";
    }

    @Override
    public Map<String, Object> getAuthorizations() {
      return new HashMap<>();
    }

    @Override
    public Agent getAgent() {
      return null;
    }

    @Override
    public int getRecordVersion() {
      return 0;
    }

    @Override
    public ValueType getValueType() {
      return ValueType.SBE_UNKNOWN;
    }

    @Override
    public UnknownRecordValue getValue() {
      return new UnknownRecordValue();
    }

    @Override
    public long getOperationReference() {
      return 0;
    }

    @Override
    public String toString() {
      return getValue().toJson();
    }
  }
}
