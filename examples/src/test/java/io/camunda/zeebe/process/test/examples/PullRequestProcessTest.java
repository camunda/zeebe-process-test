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

import static java.util.Collections.singletonMap;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.DeployResourceCommandStep1;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
/*
 * This annotation {@code import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;}
 * is recommended for Java 17+. It uses an embedded engine and is the fastest way
 * to run the tests.
 *
 * For Java 8+ use {@code import io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest;)
 * It will start the embedded engine in a Docker container.
 *
 * Both implementations are interchangeable
 */
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

  // injected by ZeebeProcessTest annotation
  private ZeebeTestEngine engine;
  // injected by ZeebeProcessTest annotation
  private ZeebeClient client;

  @BeforeEach
  void deployProcesses() {
    // The embedded engine is completely reset before each test run.

    // Therefore, we need to deploy the process each time
    final DeploymentEvent deploymentEvent =
        deployResources(PULL_REQUEST_PROCESS_RESOURCE_NAME, AUTOMATED_TESTS_PROCESS_RESOURCE_NAME);

    BpmnAssert.assertThat(deploymentEvent)
        .containsProcessesByResourceName(
            PULL_REQUEST_PROCESS_RESOURCE_NAME, AUTOMATED_TESTS_PROCESS_RESOURCE_NAME);
  }

  @Test
  void testPullRequestCreatedHappyPath() throws InterruptedException, TimeoutException {
    // Given
    final String pullRequestId = "123";

    // When

    //  -> send message to create process instance
    final PublishMessageResponse prCreatedResponse =
        sendMessage(PR_CREATED_MSG, "", singletonMap(PR_ID_VAR, pullRequestId));

    //  -> complete user task; user tasks and service tasks can be tested in similar ways
    completeUserTask(REQUEST_REVIEW);

    //  -> send another message to drive the process forward
    sendMessage(REVIEW_RECEIVED_MSG, pullRequestId, singletonMap(REVIEW_RESULT_VAR, "approved"));

    /*  -> on a parallel branch of the process, a sub process is called, which spawns three service
     *     tasks as part of a multi instance embedded sub process. These lines complete the called
     *     service tasks
     */
    completeServiceTasks(AUTOMATED_TESTS_RUN_TESTS, 3);

    //  -> back on the main process, there are two more tasks to complete to reach the end
    completeUserTask(MERGE_CODE);
    completeServiceTask(DEPLOY_SNAPSHOT);

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

    // When
    final PublishMessageResponse prCreatedResponse =
        sendMessage(PR_CREATED_MSG, "", singletonMap(PR_ID_VAR, prId));
    completeUserTask(REQUEST_REVIEW);

    completeServiceTasks(AUTOMATED_TESTS_RUN_TESTS, 3);

    //  This is how you can manipulate the time of the engine to trigger timer events
    increaseTime(Duration.ofDays(1));

    completeServiceTask(REMIND_REVIEWER);

    sendMessage(REVIEW_RECEIVED_MSG, prId, singletonMap(REVIEW_RESULT_VAR, "approved"));

    completeUserTask(MERGE_CODE);
    completeServiceTask(DEPLOY_SNAPSHOT);

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

    // When
    final PublishMessageResponse prCreatedResponse =
        sendMessage(PR_CREATED_MSG, "", singletonMap(PR_ID_VAR, prId));

    completeUserTask(REQUEST_REVIEW);

    completeServiceTasks(AUTOMATED_TESTS_RUN_TESTS, 3);

    sendMessage(REVIEW_RECEIVED_MSG, prId, singletonMap(REVIEW_RESULT_VAR, "rejected"));

    completeUserTask(MAKE_CHANGES);

    completeUserTask(REQUEST_REVIEW);

    sendMessage(REVIEW_RECEIVED_MSG, prId, singletonMap(REVIEW_RESULT_VAR, "approved"));

    completeUserTask(MERGE_CODE);
    completeServiceTask(DEPLOY_SNAPSHOT);

    // Then
    BpmnAssert.assertThat(prCreatedResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElementsInOrder(
            REQUEST_REVIEW, MAKE_CHANGES, REQUEST_REVIEW, MERGE_CODE, DEPLOY_SNAPSHOT)
        .hasNotPassedElement(REMIND_REVIEWER)
        .isCompleted();
  }

  private DeploymentEvent deployResources(final String... resources) {
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

  /* These two methods deal with the asynchronous nature of the engine. It is recommended
   * to wait for an idle state before you assert on the state of the engine. Otherwise, you
   * may run into race conditions and flaky tests, depending on whether the engine
   * is still busy processing your last commands.
   *
   * Also note that many of the helper functions used in this test (e.g. {@code sendMessage(..)}
   * have a call to this method at the end. This is to ensure that each command sent to the engine
   * is fully processed before moving on. Without that you can run into issues, where e.g. you want
   * to complete a task, but the task has not been activated yet.
   *
   * Note that the duration is not like a {@code Thread.sleep()}. The tests will continue as soon as
   * an idle state is reached. Only if no idle state is reached during the {@code duration}
   * passed in as argument, then a timeout exception will be thrown.
   */
  private void waitForIdleState(final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForIdleState(duration);
  }

  private void waitForBusyState(final Duration duration)
      throws InterruptedException, TimeoutException {
    engine.waitForBusyState(duration);
  }

  private PublishMessageResponse sendMessage(
      final String messageName, final String correlationKey, final Map<String, Object> variables)
      throws InterruptedException, TimeoutException {
    final PublishMessageResponse response =
        client
            .newPublishMessageCommand()
            .messageName(messageName)
            .correlationKey(correlationKey)
            .variables(variables)
            .send()
            .join();
    waitForIdleState(Duration.ofSeconds(1));
    return response;
  }

  private void increaseTime(final Duration duration) throws InterruptedException, TimeoutException {
    // this method increases the time in a deterministic manner

    /* Process all existing commands to make sure that timer subscriptions related to the process
     * so far have been created
     */
    waitForIdleState(Duration.ofSeconds(1));

    /* Increase time in the engine. This will not take immediate effect, though. There is a
     * real-time delay of a couple of ms until the updated time is picked up by the scheduler
     */
    engine.increaseTime(duration);

    try {
      /* This code assumes that the increase of time will trigger timer events. Therefore, we wait
       * until the engine is busy. This means that it started triggering events.
       *
       * And after that, we wait for it to become idle again. That means it is waiting for new commands
       */
      waitForBusyState(Duration.ofSeconds(1));
      waitForIdleState(Duration.ofSeconds(1));
    } catch (final TimeoutException e) {
      // Do nothing. We've waited up to 1 second for processing to start, if it didn't start in this
      // time the engine probably has not got anything left to process.
    }
  }

  private void completeServiceTask(final String jobType)
      throws InterruptedException, TimeoutException {
    completeServiceTasks(jobType, 1);
  }

  private void completeServiceTasks(final String jobType, final int count)
      throws InterruptedException, TimeoutException {

    final var activateJobsResponse =
        client.newActivateJobsCommand().jobType(jobType).maxJobsToActivate(count).send().join();

    final int activatedJobCount = activateJobsResponse.getJobs().size();
    if (activatedJobCount < count) {
      Assertions.fail(
          "Unable to activate %d jobs, because only %d were activated."
              .formatted(count, activatedJobCount));
    }

    for (int i = 0; i < count; i++) {
      final var job = activateJobsResponse.getJobs().get(i);

      client.newCompleteCommand(job.getKey()).send().join();
    }

    waitForIdleState(Duration.ofSeconds(1));
  }

  private void completeUserTask(final String elementId)
      throws InterruptedException, TimeoutException {
    // user tasks can be controlled similarly to service tasks. All user tasks share a common job
    // type
    final var activateJobsResponse =
        client
            .newActivateJobsCommand()
            .jobType("io.camunda.zeebe:userTask")
            .maxJobsToActivate(100)
            .send()
            .join();

    boolean userTaskWasCompleted = false;

    for (final ActivatedJob userTask : activateJobsResponse.getJobs()) {
      if (userTask.getElementId().equals(elementId)) {
        // complete the user task we care about
        client.newCompleteCommand(userTask).send().join();
        userTaskWasCompleted = true;
      } else {
        // fail all other user tasks that were activated
        // failing a task with a retry value >0 means the task can be reactivated in the future
        client.newFailCommand(userTask).retries(Math.max(userTask.getRetries(), 1)).send().join();
      }
    }

    waitForIdleState(Duration.ofSeconds(1));

    if (!userTaskWasCompleted) {
      Assertions.fail("Tried to complete task `%s`, but it was not found".formatted(elementId));
    }
  }
}
