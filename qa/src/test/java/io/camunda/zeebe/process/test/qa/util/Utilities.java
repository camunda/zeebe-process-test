package io.camunda.zeebe.process.test.qa.util;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.DeployProcessCommandStep1;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class Utilities {

  public static final class ProcessPackLoopingServiceTask {
    public static final String RESOURCE_NAME = "looping-servicetask.bpmn";
    public static final String PROCESS_ID = "looping-servicetask";

    public static final String ELEMENT_ID = "servicetask"; // id of the service task
    public static final String JOB_TYPE = "test"; // job type of service task
    public static final String TOTAL_LOOPS =
        "totalLoops"; // variable name to indicate number of loops
    public static final String GATEWAY_ELEMENT_ID = "Gateway_0fhwf5d";
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

  public static DeploymentEvent deployProcess(final ZeebeClient client, final String process) {
    return deployProcesses(client, process);
  }

  public static DeploymentEvent deployProcesses(
      final ZeebeClient client, final String... processes) {
    final DeployProcessCommandStep1 commandStep1 = client.newDeployCommand();

    DeployProcessCommandStep1.DeployProcessCommandBuilderStep2 commandStep2 = null;
    for (final String process : processes) {
      if (commandStep2 == null) {
        commandStep2 = commandStep1.addResourceFromClasspath(process);
      } else {
        commandStep2 = commandStep2.addResourceFromClasspath(process);
      }
    }

    return commandStep2.send().join();
  }

  public static ProcessInstanceEvent startProcessInstance(
      final InMemoryEngine engine, final ZeebeClient client, final String processId) {
    return startProcessInstance(engine, client, processId, new HashMap<>());
  }

  public static ProcessInstanceEvent startProcessInstance(
      final InMemoryEngine engine,
      final ZeebeClient client,
      final String processId,
      final Map<String, Object> variables) {
    final ProcessInstanceEvent instanceEvent =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(variables)
            .send()
            .join();
    waitForIdleState(engine);
    return instanceEvent;
  }

  public static ActivateJobsResponse activateSingleJob(
      final ZeebeClient client, final String jobType) {
    return client.newActivateJobsCommand().jobType(jobType).maxJobsToActivate(1).send().join();
  }

  public static void waitForIdleState(final InMemoryEngine engine) {
    engine.waitForIdleState();
  }

  public static PublishMessageResponse sendMessage(
      final InMemoryEngine engine,
      final ZeebeClient client,
      final String messageName,
      final String correlationKey) {
    return sendMessage(engine, client, messageName, correlationKey, Duration.ofMinutes(1));
  }

  public static PublishMessageResponse sendMessage(
      final InMemoryEngine engine,
      final ZeebeClient client,
      final String messageName,
      final String correlationKey,
      final Duration timeToLive) {
    final PublishMessageResponse response =
        client
            .newPublishMessageCommand()
            .messageName(messageName)
            .correlationKey(correlationKey)
            .timeToLive(timeToLive)
            .send()
            .join();
    waitForIdleState(engine);
    return response;
  }

  public static void increaseTime(final InMemoryEngine engine, final Duration duration)
      throws InterruptedException {
    engine.increaseTime(duration);
    try {
      engine.waitForProcessingState(Duration.ofSeconds(1));
      waitForIdleState(engine);
    } catch (TimeoutException e) {
      // Do nothing. We've waited up to 250 ms for processing to start, if it didn't start in this
      // time the engine probably has not got anything left to process.
    }
  }

  public static void completeTask(
      final InMemoryEngine engine, final ZeebeClient client, final String taskId) {
    final List<Record<JobRecordValue>> records =
        StreamFilter.jobRecords(RecordStream.of(engine.getRecordStreamSource()))
            .withElementId(taskId)
            .withIntent(JobIntent.CREATED)
            .stream()
            .collect(Collectors.toList());

    if (!records.isEmpty()) {
      final Record<JobRecordValue> lastRecord;
      lastRecord = records.get(records.size() - 1);
      client.newCompleteCommand(lastRecord.getKey()).send().join();
    } else {
      throw new IllegalStateException(
          String.format("Tried to complete task `%s`, but it was not found", taskId));
    }

    waitForIdleState(engine);
  }

  public static void throwErrorCommand(
      final InMemoryEngine engine,
      final ZeebeClient client,
      final long key,
      final String errorCode,
      final String errorMessage) {
    client.newThrowErrorCommand(key).errorCode(errorCode).errorMessage(errorMessage).send().join();
    waitForIdleState(engine);
  }
}
