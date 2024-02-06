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

package io.camunda.zeebe.process.test.qa.abstracts.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractTimerTest {

  private static final String RESOURCE = "test_timer_events.bpmn";
  private static final String PROCESS_ID = "Process_Timer_Test_01";

  private ZeebeClient client;
  private ZeebeTestEngine engine;
  private RecordStream recordStream;

  @BeforeEach
  void deployProcesses() {
    final DeploymentEvent deploymentEvent = Utilities.deployResource(client, RESOURCE);
    BpmnAssert.assertThat(deploymentEvent).containsProcessesByResourceName(RESOURCE);
  }

  @Test
  void shouldCompareTimersDueDatesCorrectlyForDifferentNowDates() throws Exception {
    final List<OffsetDateTime> dates =
        Arrays.asList(
            OffsetDateTime.of(2023, 10, 5, 15, 50, 0, 0, ZoneOffset.of("+02:00")),
            OffsetDateTime.of(2023, 10, 31, 0, 0, 0, 0, ZoneOffset.of("+02:00")),
            OffsetDateTime.of(2023, 10, 31, 23, 0, 0, 0, ZoneOffset.of("+02:00")),
            OffsetDateTime.of(2023, 10, 31, 23, 59, 0, 0, ZoneOffset.of("+02:00")),
            OffsetDateTime.of(2023, 10, 31, 23, 59, 59, 0, ZoneOffset.of("+02:00")),
            OffsetDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneOffset.of("+02:00")));

    for (final OffsetDateTime nowDate : dates) {
      shouldCompareTimersDueDatesCorrectlyForDifferentNowDates(nowDate);
    }
  }

  void shouldCompareTimersDueDatesCorrectlyForDifferentNowDates(final OffsetDateTime nowDate)
      throws Exception {

    Utilities.increaseTime(engine, Duration.between(OffsetDateTime.now(), nowDate));

    client.newCreateInstanceCommand().bpmnProcessId(PROCESS_ID).latestVersion().send().join();

    final ActivateJobsResponse response = Utilities.activateSingleJob(client, "SimpleLog01");
    final long key = response.getJobs().get(0).getKey();

    client.newCompleteCommand(key).send().join();

    waitForProcessInstanceCompleted();
  }

  private void waitForProcessInstanceCompleted() {
    Awaitility.await()
        .untilAsserted(
            () -> {
              final Optional<Record<ProcessInstanceRecordValue>> processCompleted =
                  StreamSupport.stream(
                          RecordStream.of(engine.getRecordStreamSource())
                              .processInstanceRecords()
                              .spliterator(),
                          false)
                      .filter(r -> r.getValue().getBpmnElementType() == BpmnElementType.PROCESS)
                      .filter(r -> r.getValue().getBpmnProcessId().equals(PROCESS_ID))
                      .filter(r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_COMPLETED)
                      .findFirst();

              assertThat(processCompleted).isNotEmpty();
            });
  }
}
