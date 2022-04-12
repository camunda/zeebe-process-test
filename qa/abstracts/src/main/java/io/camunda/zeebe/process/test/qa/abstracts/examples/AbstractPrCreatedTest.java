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

package io.camunda.zeebe.process.test.qa.abstracts.examples;

import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.AUTOMATED_TESTS_PROCESS_ID;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.AUTOMATED_TESTS_RESOURCE_NAME;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.AUTOMATED_TESTS_RUN_TESTS;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.DEPLOY_SNAPSHOT;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.MAKE_CHANGES;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.MERGE_CODE;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.PR_CREATED_MSG;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.PR_ID_VAR;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.REMIND_REVIEWER;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.REQUEST_REVIEW;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.RESOURCE_NAME;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.REVIEW_RECEIVED_MSG;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackPRCreated.REVIEW_RESULT_VAR;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
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
        Utilities.deployProcesses(client, RESOURCE_NAME, AUTOMATED_TESTS_RESOURCE_NAME);
    assertThat(deploymentEvent)
        .containsProcessesByResourceName(RESOURCE_NAME, AUTOMATED_TESTS_RESOURCE_NAME);
  }

  @Test
  void testPRCreateddHappyPath() throws InterruptedException, TimeoutException {
    // Given
    final String prId = "123";
    final PublishMessageResponse prCreatedResponse =
        Utilities.sendMessage(
            engine, client, PR_CREATED_MSG, "", Collections.singletonMap(PR_ID_VAR, prId));

    // When
    completeTask(REQUEST_REVIEW);
    Utilities.sendMessage(
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
    assertThat(prCreatedResponse)
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
        Utilities.sendMessage(
            engine, client, PR_CREATED_MSG, "", Collections.singletonMap(PR_ID_VAR, prId));

    // When
    completeTask(REQUEST_REVIEW);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    Utilities.increaseTime(engine, Duration.ofDays(1));
    completeTask(REMIND_REVIEWER);
    Utilities.sendMessage(
        engine,
        client,
        REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(REVIEW_RESULT_VAR, "approved"));
    completeTask(MERGE_CODE);
    completeTask(DEPLOY_SNAPSHOT);

    // Then
    assertThat(prCreatedResponse)
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
        Utilities.sendMessage(
            engine, client, PR_CREATED_MSG, "", Collections.singletonMap(PR_ID_VAR, prId));

    // When
    completeTask(REQUEST_REVIEW);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    completeTask(AUTOMATED_TESTS_RUN_TESTS);
    Utilities.sendMessage(
        engine,
        client,
        REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(REVIEW_RESULT_VAR, "rejected"));
    completeTask(MAKE_CHANGES);
    completeTask(REQUEST_REVIEW);
    Utilities.sendMessage(
        engine,
        client,
        REVIEW_RECEIVED_MSG,
        prId,
        Collections.singletonMap(REVIEW_RESULT_VAR, "approved"));
    completeTask(MERGE_CODE);
    completeTask(DEPLOY_SNAPSHOT);

    // Then
    assertThat(prCreatedResponse)
        .hasCreatedProcessInstance()
        .extractingProcessInstance()
        .hasPassedElementsInOrder(
            REQUEST_REVIEW, MAKE_CHANGES, REQUEST_REVIEW, MERGE_CODE, DEPLOY_SNAPSHOT)
        .hasNotPassedElement(REMIND_REVIEWER)
        .isCompleted();
  }

  private void completeTask(final String taskId) throws InterruptedException, TimeoutException {
    Utilities.completeTask(engine, client, taskId);
  }

  public abstract ZeebeClient getClient();

  public abstract ZeebeTestEngine getEngine();
}
