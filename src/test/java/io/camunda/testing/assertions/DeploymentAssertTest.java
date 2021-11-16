package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.BpmnAssert.assertThat;
import static io.camunda.testing.assertions.ProcessInstanceAssertTest.PROCESS_INSTANCE_BPMN;
import static io.camunda.testing.assertions.ProcessInstanceAssertTest.PROCESS_INSTANCE_ID;
import static io.camunda.testing.util.Utilities.deployProcess;
import static io.camunda.testing.util.Utilities.deployProcesses;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.testing.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.testing.util.Utilities.ProcessPackMultipleTasks;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import org.assertj.core.api.Assertions;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeAssertions
class DeploymentAssertTest {

  public static final String WRONG_VALUE = "wrong value";

  private ZeebeEngine engine;

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;
    private ZeebeClient client;

    @Test
    public void testContainsProcessesById() {
      // when
      final DeploymentEvent deploymentEvent =
          deployProcesses(
              client,
              ProcessPackLoopingServiceTask.RESOURCE_NAME,
              ProcessPackMultipleTasks.RESOURCE_NAME);

      // then
      assertThat(deploymentEvent)
          .containsProcessesByBpmnProcessId(
              ProcessPackLoopingServiceTask.PROCESS_ID, ProcessPackMultipleTasks.PROCESS_ID);
    }

    @Test
    public void testContainsProcessesByResourceName() {
      // when
      final DeploymentEvent deploymentEvent =
          deployProcesses(
              client,
              ProcessPackLoopingServiceTask.RESOURCE_NAME,
              ProcessPackMultipleTasks.RESOURCE_NAME);

      // then
      assertThat(deploymentEvent)
          .containsProcessesByResourceName(
              ProcessPackLoopingServiceTask.RESOURCE_NAME, ProcessPackMultipleTasks.RESOURCE_NAME);
    }

    @Test
    public void testExtractingProcessByBpmnProcessId() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      Assertions.assertThat(processAssert).isNotNull();
    }

    @Test
    public void testExtractingProcessByResourceName() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByResourceName(ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // then
      Assertions.assertThat(processAssert).isNotNull();
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private RecordStreamSource recordStreamSource;
    private ZeebeClient client;

    @Test
    public void testContainsProcessesByIdFailure() {
      // when
      final DeploymentEvent deploymentEvent = deployProcess(client, PROCESS_INSTANCE_BPMN);

      // then
      assertThatThrownBy(
              () -> assertThat(deploymentEvent).containsProcessesByBpmnProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(WRONG_VALUE, PROCESS_INSTANCE_ID);
    }

    @Test
    public void testContainsProcessesByResourceNameFailure() {
      // when
      final DeploymentEvent deploymentEvent = deployProcess(client, PROCESS_INSTANCE_BPMN);

      // then
      assertThatThrownBy(
              () -> assertThat(deploymentEvent).containsProcessesByResourceName(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(WRONG_VALUE, PROCESS_INSTANCE_BPMN);
    }

    @Test
    public void testExtractingProcessByBpmnProcessIdFailure() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // then
      assertThatThrownBy(
              () -> assertThat(deploymentEvent).extractingProcessByBpmnProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one process for BPMN process id 'wrong value' but found 0: []");
    }

    @Test
    public void testExtractingProcessByResourceName() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // then
      assertThatThrownBy(
              () -> assertThat(deploymentEvent).extractingProcessByResourceName(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one process for resource name 'wrong value' but found 0: []");
    }
  }
}
