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
package io.camunda.zeebe.process.test.qa.abstracts.util;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.DeployResourceCommandStep1;
import io.camunda.client.api.response.ActivateJobsResponse;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.response.ProcessInstanceResult;
import io.camunda.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class contains utility methods for our own tests. */
public class Utilities {

  private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

  public static DeploymentEvent deployResource(final CamundaClient client, final String resource) {
    return deployResources(client, resource);
  }

  public static DeploymentEvent deployResources(
      final CamundaClient client, final String... resources) {
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

  public static ProcessInstanceEvent startProcessInstance(
      final ZeebeTestEngine engine, final CamundaClient client, final String processId)
      throws InterruptedException, TimeoutException {
    return startProcessInstance(engine, client, processId, new HashMap<>());
  }

  public static ProcessInstanceEvent startProcessInstance(
      final ZeebeTestEngine engine,
      final CamundaClient client,
      final String processId,
      final Map<String, Object> variables)
      throws InterruptedException, TimeoutException {
    final ProcessInstanceEvent instanceEvent =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(variables)
            .send()
            .join();
    waitForIdleState(engine, Duration.ofSeconds(1));
    return instanceEvent;
  }

  public static ProcessInstanceResult startProcessInstanceWithResult(
      final ZeebeTestEngine engine,
      final CamundaClient client,
      final String processId,
      final Map<String, Object> variables)
      throws InterruptedException, TimeoutException {
    final ProcessInstanceResult instanceResult =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(variables)
            .withResult()
            .send()
            .join();

    waitForIdleState(engine, Duration.ofSeconds(1));
    return instanceResult;
  }

  public static ActivateJobsResponse activateSingleJob(
      final CamundaClient client, final String jobType) {
    return client.newActivateJobsCommand().jobType(jobType).maxJobsToActivate(1).send().join();
  }

  public static void waitForIdleState(final ZeebeTestEngine engine, final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForIdleState(duration);
  }

  public static void waitForBusyState(final ZeebeTestEngine engine, final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForBusyState(duration);
  }

  /**
   * Publishes a message with the given name and correlation key. Waits for the engine to be idle
   * afterward to ensure that the message publication is processed.
   *
   * <p>The message is published without a time to live and without variables. If you need to set a
   * time to live or variables, use {@link #sendMessage(ZeebeTestEngine, CamundaClient, String,
   * String, Duration, Map)} instead.
   *
   * @param engine the engine to wait for to be idle
   * @param client the client to use to publish the message
   * @param messageName the name of the message to publish
   * @param correlationKey the correlation key of the message to publish
   * @return the response of the publish message command
   * @throws InterruptedException if the thread is interrupted while waiting for the engine to be
   *     idle
   * @throws TimeoutException if the engine does not become idle within the timeout
   */
  public static PublishMessageResponse sendMessage(
      final ZeebeTestEngine engine,
      final CamundaClient client,
      final String messageName,
      final String correlationKey)
      throws InterruptedException, TimeoutException {
    return sendMessage(
        engine, client, messageName, correlationKey, Duration.ZERO, Collections.emptyMap());
  }

  /**
   * Publishes a message with the given name, correlation key, time to live, and variables. Waits
   * for the engine to be idle afterward to ensure that the message publication is processed.
   *
   * <p>If you do not need to set a time to live or variables, use {@link
   * #sendMessage(ZeebeTestEngine, CamundaClient, String, String)} instead.
   *
   * @param engine the engine to wait for to be idle
   * @param client the client to use to publish the message
   * @param messageName the name of the message to publish
   * @param correlationKey the correlation key of the message to publish
   * @param timeToLive the time until the message expires after publication
   * @param variables the variables to pass along as payload of the message to publish
   * @return the response of the publish message command
   * @throws InterruptedException if the thread is interrupted while waiting for the engine to be
   *     idle
   * @throws TimeoutException if the engine does not become idle within the timeout
   */
  public static PublishMessageResponse sendMessage(
      final ZeebeTestEngine engine,
      final CamundaClient client,
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
    } catch (final TimeoutException e) {
      // Logging warning for unexpected case while 1 second was not enough.
      // This is done for troubleshooting the following issue:
      // https://github.com/camunda/zeebe-process-test/issues/960
      // These changes should be reverted when the root cause of the flakiness of
      // the UnhappyPathTests.testHasNotExpiredFailure is clear
      LOG.warn("Timeout reached while waiting for idle state", e);
    }

    engine.increaseTime(duration);

    try {
      waitForBusyState(engine, Duration.ofSeconds(1));
    } catch (final TimeoutException e) {
      // Logging warning for unexpected case when 1 second was not enough.
      // This is done for troubleshooting the following issue:
      // https://github.com/camunda/zeebe-process-test/issues/960
      // These changes should be reverted when the root cause of the flakiness of
      // the UnhappyPathTests.testHasNotExpiredFailure is clear
      LOG.warn("Timeout reached while waiting for busy state after time increase", e);
    }
    try {
      waitForIdleState(engine, Duration.ofSeconds(1));
    } catch (final TimeoutException e) {
      // Logging warning for unexpected case when 1 second was not enough.
      // This is done for troubleshooting the following issue:
      // https://github.com/camunda/zeebe-process-test/issues/960
      // These changes should be reverted when the root cause of the flakiness of
      // the UnhappyPathTests.testHasNotExpiredFailure is clear
      LOG.warn("Timeout reached while waiting for idle state after time increase", e);
    }
  }

  public static void completeTask(
      final ZeebeTestEngine engine, final CamundaClient client, final String taskId)
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

  public static void throwErrorCommand(
      final ZeebeTestEngine engine,
      final CamundaClient client,
      final long key,
      final String errorCode,
      final String errorMessage)
      throws InterruptedException, TimeoutException {
    client.newThrowErrorCommand(key).errorCode(errorCode).errorMessage(errorMessage).send().join();
    waitForIdleState(engine, Duration.ofSeconds(1));
  }

  public static final class ProcessPackLoopingServiceTask {

    public static final String RESOURCE_NAME = "looping-servicetask.bpmn";
    public static final String PROCESS_ID = "looping-servicetask";

    public static final String ELEMENT_ID = "servicetask"; // id of the service task
    public static final String JOB_TYPE = "test"; // job type of service task
    public static final String TOTAL_LOOPS =
        "totalLoops"; // variable name to indicate number of loops
    public static final String GATEWAY_ELEMENT_ID = "Gateway_0fhwf5d";
    public static final String LOOP_SEQUENCE_FLOW_ID = "loopSequenceFlow";
    public static final String START_EVENT_ID = "startevent";
    public static final String END_EVENT_ID = "endevent";
  }

  public static final class ProcessPackMultipleTasks {

    public static final String RESOURCE_NAME = "multiple-tasks.bpmn";
    public static final String PROCESS_ID = "multiple-tasks";
    public static final String ELEMENT_ID_1 = "servicetask1";
    public static final String ELEMENT_ID_2 = "servicetask2";
    public static final String ELEMENT_ID_3 = "servicetask3";
  }

  public static final class ProcessPackMessageEvent {

    public static final String RESOURCE_NAME = "message-event.bpmn";
    public static final String PROCESS_ID = "message-event";
    public static final String MESSAGE_NAME = "message";
    public static final String CORRELATION_KEY_VARIABLE = "correlationKey";
  }

  public static final class ProcessPackMessageStartEvent {

    public static final String RESOURCE_NAME = "message-start-event.bpmn";
    public static final String MESSAGE_NAME = "start-message";
    public static final String CORRELATION_KEY = "";
  }

  public static final class ProcessPackTimerStartEvent {

    public static final String RESOURCE_NAME = "timer-start-event-daily.bpmn";
    public static final String TIMER_ID = "timer";
  }

  public static final class ProcessPackMultipleCallActivity {

    public static final String RESOURCE_NAME = "multiple-call-activity.bpmn";
    public static final String PROCESS_ID = "multiple-call-activity";
    public static final String CALL_ACTIVITY_ID_ONE = "callactivityOne";
    public static final String CALLED_RESOURCE_NAME_ONE =
        ProcessPackAlternateStartEndEvent.RESOURCE_NAME;
    public static final String CALLED_PROCESS_ID_ONE = ProcessPackAlternateStartEndEvent.PROCESS_ID;
    public static final String CALL_ACTIVITY_ID_TWO = "callactivityTwo";
    public static final String CALLED_RESOURCE_NAME_TWO = ProcessPackCallActivity.RESOURCE_NAME;
    public static final String CALLED_PROCESS_ID_TWO = ProcessPackCallActivity.PROCESS_ID;
    public static final String CALLED_RESOURCE_NAME_THREE = ProcessPackStartEndEvent.RESOURCE_NAME;
    public static final String CALLED_PROCESS_ID_THREE = ProcessPackStartEndEvent.PROCESS_ID;
  }

  public static final class ProcessPackCallActivity {

    public static final String RESOURCE_NAME = "call-activity.bpmn";
    public static final String PROCESS_ID = "call-activity";
    public static final String CALL_ACTIVITY_ID = "callactivity";
    public static final String CALLED_RESOURCE_NAME = ProcessPackStartEndEvent.RESOURCE_NAME;
    public static final String CALLED_PROCESS_ID = ProcessPackStartEndEvent.PROCESS_ID;
  }

  public static final class ProcessPackStartEndEvent {

    public static final String RESOURCE_NAME = "start-end.bpmn";
    public static final String PROCESS_ID = "start-end";
  }

  public static final class ProcessPackAlternateStartEndEvent {

    public static final String RESOURCE_NAME = "alternate-start-end.bpmn";
    public static final String PROCESS_ID = "alternate-start-end";
  }

  public static final class ProcessPackNamedElements {
    public static final String RESOURCE_NAME = "named-elements.bpmn";
    public static final String RESOURCE_NAME_V2 = "named-elements-v2.bpmn";
    public static final String PROCESS_ID = "NamedProcess";
    public static final String START_EVENT_NAME = "Process started";
    public static final String START_EVENT_ID = "ProcessStartedStartEvent";

    public static final String TASK_NAME = "Do something";

    public static final String END_EVENT_NAME = "Process complete";
    public static final String END_EVENT_ID = "ProcessCompleteEndEvent";
  }

  public static final class FormPack {
    public static final String RESOURCE_NAME = "test-form.form";
    public static final String FORM_V2_RESOURCE_NAME = "test-form-v2.form";
    public static final String FORM_ID = "Form_0w7r08e";
  }
}
