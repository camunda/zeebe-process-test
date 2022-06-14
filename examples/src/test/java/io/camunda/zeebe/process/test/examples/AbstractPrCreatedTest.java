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
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.examples.Utilities.ProcessPackPRCreated;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is an abstract test class so we can test these tests with both our extensions (embedded and
 * testcontainer) without the need to duplicate our test code.
 *
 * <p>Users would not do this. They would create a regular test class and annotate this with the
 * preferred annotation to include the extension they need.
 */
public abstract class AbstractPrCreatedTest {

  private ZeebeTestEngine engine;
  private ZeebeClient client;

  @BeforeEach
  void deployProcesses() {
    // Normally these fields get injected by our annotation. Since we want to reuse these tests we
    // need to use these abstract methods to obtain them, as they get injected in the extending test
    // classes. Users would not need to do this.
    engine = getEngine();
    client = getClient();

    final DeploymentEvent deploymentEvent =
        Utilities.deployResources(
            client,
            ProcessPackPRCreated.RESOURCE_NAME,
            ProcessPackPRCreated.AUTOMATED_TESTS_RESOURCE_NAME);
    BpmnAssert.assertThat(deploymentEvent)
        .containsProcessesByResourceName(
            ProcessPackPRCreated.RESOURCE_NAME, ProcessPackPRCreated.AUTOMATED_TESTS_RESOURCE_NAME);
  }

  @Test
  void testPRCreatedHappyPath() throws InterruptedException, TimeoutException {
    // Given
    final String prId = "123";
    final PublishMessageResponse prCreatedResponse =
        Utilities.sendMessage(
            engine,
            client,
            ProcessPackPRCreated.PR_CREATED_MSG,
            "",
            Collections.singletonMap(ProcessPackPRCreated.PR_ID_VAR, prId));

    // When
    completeTask(ProcessPackPRCreated.REQUEST_REVIEW);
    Utilities.sendMessage(
        engine,
        client,
        ProcessPackPRCreated.REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(ProcessPackPRCreated.REVIEW_RESULT_VAR, "approved"));
    completeTask(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS);
    completeTask(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS);
    completeTask(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS);
    completeTask(ProcessPackPRCreated.MERGE_CODE);
    completeTask(ProcessPackPRCreated.DEPLOY_SNAPSHOT);

    // Then
    BpmnAssert.assertThat(prCreatedResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElementsInOrder(
            ProcessPackPRCreated.REQUEST_REVIEW,
            ProcessPackPRCreated.MERGE_CODE,
            ProcessPackPRCreated.DEPLOY_SNAPSHOT)
        .hasNotPassedElement(ProcessPackPRCreated.REMIND_REVIEWER)
        .hasNotPassedElement(ProcessPackPRCreated.MAKE_CHANGES)
        .hasVariableWithValue(ProcessPackPRCreated.REVIEW_RESULT_VAR, "approved")
        .extractingLatestCalledProcess(ProcessPackPRCreated.AUTOMATED_TESTS_PROCESS_ID)
        .hasPassedElement(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS, 3)
        .isCompleted();
  }

  @Test
  void testRemindReviewer() throws InterruptedException, TimeoutException {
    // Given
    final String prId = "123";
    final PublishMessageResponse prCreatedResponse =
        Utilities.sendMessage(
            engine,
            client,
            ProcessPackPRCreated.PR_CREATED_MSG,
            "",
            Collections.singletonMap(ProcessPackPRCreated.PR_ID_VAR, prId));

    // When
    completeTask(ProcessPackPRCreated.REQUEST_REVIEW);
    completeTask(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS);
    completeTask(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS);
    completeTask(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS);
    Utilities.increaseTime(engine, Duration.ofDays(1));
    completeTask(ProcessPackPRCreated.REMIND_REVIEWER);
    Utilities.sendMessage(
        engine,
        client,
        ProcessPackPRCreated.REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(ProcessPackPRCreated.REVIEW_RESULT_VAR, "approved"));
    completeTask(ProcessPackPRCreated.MERGE_CODE);
    completeTask(ProcessPackPRCreated.DEPLOY_SNAPSHOT);

    // Then
    BpmnAssert.assertThat(prCreatedResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElementsInOrder(
            ProcessPackPRCreated.REQUEST_REVIEW,
            ProcessPackPRCreated.REMIND_REVIEWER,
            ProcessPackPRCreated.MERGE_CODE,
            ProcessPackPRCreated.DEPLOY_SNAPSHOT)
        .hasNotPassedElement(ProcessPackPRCreated.MAKE_CHANGES)
        .isCompleted();
  }

  @Test
  void testRejectReview() throws InterruptedException, TimeoutException {
    // Given
    final String prId = "123";
    final PublishMessageResponse prCreatedResponse =
        Utilities.sendMessage(
            engine,
            client,
            ProcessPackPRCreated.PR_CREATED_MSG,
            "",
            Collections.singletonMap(ProcessPackPRCreated.PR_ID_VAR, prId));

    // When
    completeTask(ProcessPackPRCreated.REQUEST_REVIEW);
    completeTask(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS);
    completeTask(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS);
    completeTask(ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS);
    Utilities.sendMessage(
        engine,
        client,
        ProcessPackPRCreated.REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(ProcessPackPRCreated.REVIEW_RESULT_VAR, "rejected"));
    completeTask(ProcessPackPRCreated.MAKE_CHANGES);
    completeTask(ProcessPackPRCreated.REQUEST_REVIEW);
    Utilities.sendMessage(
        engine,
        client,
        ProcessPackPRCreated.REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(ProcessPackPRCreated.REVIEW_RESULT_VAR, "approved"));
    completeTask(ProcessPackPRCreated.MERGE_CODE);
    completeTask(ProcessPackPRCreated.DEPLOY_SNAPSHOT);

    // Then
    BpmnAssert.assertThat(prCreatedResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElementsInOrder(
            ProcessPackPRCreated.REQUEST_REVIEW,
            ProcessPackPRCreated.MAKE_CHANGES,
            ProcessPackPRCreated.REQUEST_REVIEW,
            ProcessPackPRCreated.MERGE_CODE,
            ProcessPackPRCreated.DEPLOY_SNAPSHOT)
        .hasNotPassedElement(ProcessPackPRCreated.REMIND_REVIEWER)
        .isCompleted();
  }

  private void completeTask(final String taskId) throws InterruptedException, TimeoutException {
    Utilities.completeTask(engine, client, taskId);
  }

  public abstract ZeebeClient getClient();

  public abstract ZeebeTestEngine getEngine();
}
