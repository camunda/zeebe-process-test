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

package io.camunda.zeebe.process.test.examples;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.DeployResourceCommandStep1;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
public class PullRequestProcessTest {

  private static final String PULL_REQUEST_PROCESS_RESOURCE_NAME = "pr-created.bpmn";
  private static final String AUTOMATED_TESTS_PROCESS_RESOURCE_NAME = "automated-tests.bpmn";
  private static final String AUTOMATED_TESTS_PROCESS_ID = "automatedTestsProcess";
  private static final String AUTOMATED_TESTS_RUN_TESTS = "runTests";
  private static final String PR_CREATED_MSG = "prCreated";
  private static final String REVIEW_RECEIVED_MSG = "reviewReceived";
  private static final String PR_ID_VAR = "prId";
  private static final String REVIEW_RESULT_VAR = "reviewResult";
  private static final String REQUEST_REVIEW = "requestReview";
  private static final String REMIND_REVIEWER = "remindReviewer";
  private static final String MAKE_CHANGES = "makeChanges";
  private static final String MERGE_CODE = "mergeCode";
  private static final String DEPLOY_SNAPSHOT = "deploySnapshot";

  private ZeebeTestEngine engine;
  private ZeebeClient client;

  @BeforeEach
  void deployProcesses() {
    final DeploymentEvent deploymentEvent =
        deployResources(
            client, PULL_REQUEST_PROCESS_RESOURCE_NAME, AUTOMATED_TESTS_PROCESS_RESOURCE_NAME);
    BpmnAssert.assertThat(deploymentEvent)
        .containsProcessesByResourceName(
            PULL_REQUEST_PROCESS_RESOURCE_NAME, AUTOMATED_TESTS_PROCESS_RESOURCE_NAME);
  }

  @Test
  void testPullRequestCreatedHappyPath() throws InterruptedException, TimeoutException {
    // Given
    final String prId = "123";
    final PublishMessageResponse prCreatedResponse =
        sendMessage(engine, client, PR_CREATED_MSG, "", Collections.singletonMap(PR_ID_VAR, prId));

    // When
    completeTask(REQUEST_REVIEW);
    sendMessage(
        engine,
        client,
        REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(REVIEW_RESULT_VAR, "approved"));
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(MERGE_CODE);
    completeTask(DEPLOY_SNAPSHOT);

    // Then
    BpmnAssert.assertThat(prCreatedResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElementsInOrder(REQUEST_REVIEW, MERGE_CODE, DEPLOY_SNAPSHOT)
        .hasNotPassedElement(REMIND_REVIEWER)
        .hasNotPassedElement(MAKE_CHANGES)
        .hasVariableWithValue(REVIEW_RESULT_VAR, "approved")
        .extractingLatestCalledProcess(AUTOMATED_TESTS_PROCESS_ID)
        .hasPassedElement(AUTOMATED_TESTS_RUN_TESTS, 3)
        .isCompleted();
  }

  @Test
  void testRemindReviewer() throws InterruptedException, TimeoutException {
    // Given
    final String prId = "123";
    final PublishMessageResponse prCreatedResponse =
        sendMessage(engine, client, PR_CREATED_MSG, "", Collections.singletonMap(PR_ID_VAR, prId));

    // When
    completeTask(REQUEST_REVIEW);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    increaseTime(engine, Duration.ofDays(1));
    completeTask(REMIND_REVIEWER);
    sendMessage(
        engine,
        client,
        REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(REVIEW_RESULT_VAR, "approved"));
    completeTask(MERGE_CODE);
    completeTask(DEPLOY_SNAPSHOT);

    // Then
    BpmnAssert.assertThat(prCreatedResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElementsInOrder(REQUEST_REVIEW, REMIND_REVIEWER, MERGE_CODE, DEPLOY_SNAPSHOT)
        .hasNotPassedElement(MAKE_CHANGES)
        .isCompleted();
  }

  @Test
  void testRejectReview() throws InterruptedException, TimeoutException {
    // Given
    final String prId = "123";
    final PublishMessageResponse prCreatedResponse =
        sendMessage(engine, client, PR_CREATED_MSG, "", Collections.singletonMap(PR_ID_VAR, prId));

    // When
    completeTask(REQUEST_REVIEW);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    sendMessage(
        engine,
        client,
        REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(REVIEW_RESULT_VAR, "rejected"));
    completeTask(MAKE_CHANGES);
    completeTask(REQUEST_REVIEW);
    sendMessage(
        engine,
        client,
        REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(REVIEW_RESULT_VAR, "approved"));
    completeTask(MERGE_CODE);
    completeTask(DEPLOY_SNAPSHOT);

    // Then
    BpmnAssert.assertThat(prCreatedResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElementsInOrder(
            REQUEST_REVIEW, MAKE_CHANGES, REQUEST_REVIEW, MERGE_CODE, DEPLOY_SNAPSHOT)
        .hasNotPassedElement(REMIND_REVIEWER)
        .isCompleted();
  }

  private void completeTask(final String taskId) throws InterruptedException, TimeoutException {
    completeTask(engine, client, taskId);
  }

  private static DeploymentEvent deployResources(
      final ZeebeClient client, final String... resources) {
    final DeployResourceCommandStep1 commandStep1 = client.newDeployResourceCommand();

    DeployResourceCommandStep1.DeployResourceCommandStep2 commandStep2 = null;
    for (final String process : resources) {
      if (commandStep2 == null) {
        commandStep2 = commandStep1.addResourceFromClasspath(process);
      } else {
        commandStep2 = commandStep2.addResourceFromClasspath(process);
      }
    }

    return commandStep2.send().join();
  }

  private static void waitForIdleState(final ZeebeTestEngine engine, final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForIdleState(duration);
  }

  private static void waitForBusyState(final ZeebeTestEngine engine, final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForBusyState(duration);
  }

  private static PublishMessageResponse sendMessage(
      final ZeebeTestEngine engine,
      final ZeebeClient client,
      final String messageName,
      final String correlationKey,
      final Map<String, Object> variables)
      throws InterruptedException, TimeoutException {
    return sendMessage(
        engine, client, messageName, correlationKey, Duration.ofMinutes(1), variables);
  }

  private static PublishMessageResponse sendMessage(
      final ZeebeTestEngine engine,
      final ZeebeClient client,
      final String messageName,
      final String correlationKey,
      final Duration timeToLive,
      final Map<String, Object> variables)
      throws InterruptedException, TimeoutException {
    final PublishMessageResponse response =
        client
            .newPublishMessageCommand()
            .messageName(messageName)
            .correlationKey(correlationKey)
            .timeToLive(timeToLive)
            .variables(variables)
            .send()
            .join();
    waitForIdleState(engine, Duration.ofSeconds(1));
    return response;
  }

  private static void increaseTime(final ZeebeTestEngine engine, final Duration duration)
      throws InterruptedException {
    try {
      waitForIdleState(engine, Duration.ofSeconds(1));
      engine.increaseTime(duration);
      waitForBusyState(engine, Duration.ofSeconds(1));
      waitForIdleState(engine, Duration.ofSeconds(1));
    } catch (final TimeoutException e) {
      // Do nothing. We've waited up to 1 second for processing to start, if it didn't start in this
      // time the engine probably has not got anything left to process.
    }
  }

  private static void completeTask(
      final ZeebeTestEngine engine, final ZeebeClient client, final String taskId)
      throws InterruptedException, TimeoutException {
    final List<Record<JobRecordValue>> records =
        StreamFilter.jobRecords(RecordStream.of(engine.getRecordStreamSource()))
            .withElementId(taskId)
            .withIntent(JobIntent.CREATED)
            .stream()
            .collect(Collectors.toList());

    StreamFilter.jobRecords(RecordStream.of(engine.getRecordStreamSource()))
        .withElementId(taskId)
        .withIntent(JobIntent.COMPLETED)
        .stream()
        .forEach(record -> records.removeIf(r -> record.getKey() == r.getKey()));

    if (!records.isEmpty()) {
      final Record<JobRecordValue> lastRecord;
      lastRecord = records.get(records.size() - 1);
      client.newCompleteCommand(lastRecord.getKey()).send().join();
    } else {
      throw new IllegalStateException(
          String.format("Tried to complete task `%s`, but it was not found", taskId));
    }

    waitForIdleState(engine, Duration.ofSeconds(1));
  }
}
