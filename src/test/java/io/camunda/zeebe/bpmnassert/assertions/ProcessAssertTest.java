package io.camunda.zeebe.bpmnassert.assertions;

import static io.camunda.zeebe.bpmnassert.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.bpmnassert.util.Utilities.deployProcess;
import static io.camunda.zeebe.bpmnassert.util.Utilities.startProcessInstance;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.zeebe.bpmnassert.extensions.ZeebeProcessTest;
import io.camunda.zeebe.bpmnassert.testengine.InMemoryEngine;
import io.camunda.zeebe.bpmnassert.testengine.RecordStreamSource;
import io.camunda.zeebe.bpmnassert.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
class ProcessAssertTest {

  public static final String WRONG_VALUE = "wrong value";

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;
    private ZeebeClient client;
    private InMemoryEngine engine;

    @Test
    public void testHasBPMNProcessId() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      processAssert.hasBPMNProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    public void testHasVersion() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      processAssert.hasVersion(1);
    }

    @Test
    public void testHasResourceName() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      processAssert.hasResourceName(ProcessPackLoopingServiceTask.RESOURCE_NAME);
    }

    @Test
    public void testHasAnyInstances() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      processAssert.hasAnyInstances();
    }

    @Test
    public void testHasNoInstances() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      processAssert.hasNoInstances();
    }

    @Test
    public void testHasInstances() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      processAssert.hasInstances(2);
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private RecordStreamSource recordStreamSource;
    private ZeebeClient client;
    private InMemoryEngine engine;

    @Test
    public void testHasBPMNProcessIdFailure() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> processAssert.hasBPMNProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected BPMN process ID to be '%s' but was '%s' instead.",
              WRONG_VALUE, ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    public void testHasVersionFailure() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> processAssert.hasVersion(12345))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected version to be 12345 but was 1 instead");
    }

    @Test
    public void testHasResourceNameFailure() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> processAssert.hasResourceName(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected resource name to be '%s' but was '%s' instead.",
              WRONG_VALUE, ProcessPackLoopingServiceTask.RESOURCE_NAME);
    }

    @Test
    public void testHasAnyInstancesFailure() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(processAssert::hasAnyInstances)
          .isInstanceOf(AssertionError.class)
          .hasMessage("The process has no instances");
    }

    @Test
    public void testHasNoInstances() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(processAssert::hasNoInstances)
          .isInstanceOf(AssertionError.class)
          .hasMessage("The process does have instances");
    }

    @Test
    public void testHasInstances() {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      final ProcessAssert processAssert =
          assertThat(deploymentEvent)
              .extractingProcessByBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> processAssert.hasInstances(2))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected number of instances to be 2 but was 3 instead");
    }
  }
}
