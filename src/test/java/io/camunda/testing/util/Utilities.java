package io.camunda.testing.util;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.DeployProcessCommandStep1;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.camunda.community.eze.ZeebeEngine;
import org.camunda.community.eze.ZeebeEngineClock;

// @TODO Use this also in other tests
public class Utilities {

  public static final class ProcessPackLoopingServiceTask {
    public static final String RESOURCE_NAME = "looping-servicetask.bpmn";
    public static final String PROCESS_ID = "looping-servicetask";

    public static final String ELEMENT_ID = "servicetask"; // id of the service task
    public static final String JOB_TYPE = "test"; // job type of service task
    public static final String TOTAL_LOOPS =
        "totalLoops"; // variable name to indicate number of loops
    public static final String GATEWAY_ELEMENT_ID = "Gateway_0fhwf5d";
  }

  public static final class ProcessPackMultipleTasks {
    public static final String RESOURCE_NAME = "multiple-tasks.bpmn";
    public static final String PROCESS_ID = "multiple-tasks";
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
      final ZeebeClient client, final String processId) throws InterruptedException {
    return startProcessInstance(client, processId, new HashMap<>());
  }

  public static ProcessInstanceEvent startProcessInstance(
      final ZeebeClient client, final String processId, final Map<String, Object> variables)
      throws InterruptedException {
    final ProcessInstanceEvent instanceEvent =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(variables)
            .send()
            .join();
    Thread.sleep(100);
    return instanceEvent;
  }

  public static ActivateJobsResponse activateSingleJob(
      final ZeebeClient client, final String jobType) {
    return client.newActivateJobsCommand().jobType(jobType).maxJobsToActivate(1).send().join();
  }

  // TODO find a better solution for this
  public static void waitForIdleState(final ZeebeEngine engine) {
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {
      e.printStackTrace();
      throw new IllegalStateException("Sleep was interrupted");
    }
  }

  public static PublishMessageResponse sendMessage(
      final ZeebeClient client, final String messsageName, final String correlationKey)
      throws InterruptedException {
    return sendMessage(client, messsageName, correlationKey, Duration.ofDays(99999));
  }

  public static PublishMessageResponse sendMessage(
      final ZeebeClient client,
      final String messsageName,
      final String correlationKey,
      final Duration timeToLive)
      throws InterruptedException {
    final PublishMessageResponse response =
        client
            .newPublishMessageCommand()
            .messageName(messsageName)
            .correlationKey(correlationKey)
            .timeToLive(timeToLive)
            .send()
            .join();
    Thread.sleep(100);
    return response;
  }

  public static void increaseTime(final ZeebeEngineClock clock, final Duration duration)
      throws InterruptedException {
    clock.increaseTime(duration);
    Thread.sleep(100);
  }
}
