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
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class Utilities {

  public static DeploymentEvent deployResources(
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

  public static void waitForIdleState(final ZeebeTestEngine engine, final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForIdleState(duration);
  }

  public static void waitForBusyState(final ZeebeTestEngine engine, final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForBusyState(duration);
  }

  public static PublishMessageResponse sendMessage(
      final ZeebeTestEngine engine,
      final ZeebeClient client,
      final String messageName,
      final String correlationKey,
      final Map<String, Object> variables)
      throws InterruptedException, TimeoutException {
    return sendMessage(
        engine, client, messageName, correlationKey, Duration.ofMinutes(1), variables);
  }

  public static PublishMessageResponse sendMessage(
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

  public static void increaseTime(final ZeebeTestEngine engine, final Duration duration)
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

  public static void completeTask(
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

  public static final class ProcessPackPRCreated {

    public static final String RESOURCE_NAME = "pr-created.bpmn";
    public static final String PR_CREATED_MSG = "prCreated";
    public static final String REVIEW_RECEIVED_MSG = "reviewReceived";
    public static final String PR_ID_VAR = "prId";
    public static final String REVIEW_RESULT_VAR = "reviewResult";
    public static final String REQUEST_REVIEW = "requestReview";
    public static final String REMIND_REVIEWER = "remindReviewer";
    public static final String MAKE_CHANGES = "makeChanges";
    public static final String MERGE_CODE = "mergeCode";
    public static final String DEPLOY_SNAPSHOT = "deploySnapshot";

    public static final String AUTOMATED_TESTS_RESOURCE_NAME = "automated-tests.bpmn";
    public static final String AUTOMATED_TESTS_PROCESS_ID = "automatedTestsProcess";
    public static final String AUTOMATED_TESTS_RUN_TESTS = "runTests";
  }
}
