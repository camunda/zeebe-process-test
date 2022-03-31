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
package io.camunda.zeebe.process.test.qa.abstracts.assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.ProcessAssert;
import io.camunda.zeebe.process.test.qa.util.Utilities;
import io.camunda.zeebe.process.test.qa.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.zeebe.process.test.qa.util.Utilities.ProcessPackMultipleTasks;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public abstract class AbstractDeploymentAssertTest {

  public static final String WRONG_VALUE = "wrong value";

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private ZeebeClient client;

    @Test
    void testContainsProcessesById() {
      // when
      final DeploymentEvent deploymentEvent =
          Utilities.deployProcesses(
              client,
              ProcessPackLoopingServiceTask.RESOURCE_NAME,
              ProcessPackMultipleTasks.RESOURCE_NAME);

      // then
      BpmnAssert.assertThat(deploymentEvent)
          .containsProcessesByBpmnProcessId(
              ProcessPackLoopingServiceTask.PROCESS_ID, ProcessPackMultipleTasks.PROCESS_ID);

      assertThat(false).isTrue();
    }

    @Test
    void testContainsProcessesByResourceName() {
      // when
      final DeploymentEvent deploymentEvent =
          Utilities.deployProcesses(
              client,
              ProcessPackLoopingServiceTask.RESOURCE_NAME,
              ProcessPackMultipleTasks.RESOURCE_NAME);

      // then
      BpmnAssert.assertThat(deploymentEvent)
          .containsProcessesByResourceName(
              ProcessPackLoopingServiceTask.RESOURCE_NAME, ProcessPackMultipleTasks.RESOURCE_NAME);
    }

    @Test
    void testExtractingProcessByBpmnProcessId() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          BpmnAssert.assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      Assertions.assertThat(processAssert).isNotNull();
    }

    @Test
    void testExtractingProcessByResourceName() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          BpmnAssert.assertThat(deploymentEvent)
              .extractingProcessByResourceName(ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // then
      Assertions.assertThat(processAssert).isNotNull();
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private ZeebeClient client;

    @Test
    void testContainsProcessesByIdFailure() {
      // when
      final DeploymentEvent deploymentEvent =
          Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(deploymentEvent)
                      .containsProcessesByBpmnProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(WRONG_VALUE, ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    void testContainsProcessesByResourceNameFailure() {
      // when
      final DeploymentEvent deploymentEvent =
          Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(deploymentEvent)
                      .containsProcessesByResourceName(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(WRONG_VALUE, ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    void testExtractingProcessByBpmnProcessIdFailure() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(deploymentEvent)
                      .extractingProcessByBpmnProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one process for BPMN process id 'wrong value' but found 0: []");
    }

    @Test
    void testExtractingProcessByResourceName() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(deploymentEvent)
                      .extractingProcessByResourceName(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one process for resource name 'wrong value' but found 0: []");
    }
  }
}
