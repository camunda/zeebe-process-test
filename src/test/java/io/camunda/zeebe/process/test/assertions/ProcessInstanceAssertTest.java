package io.camunda.zeebe.process.test.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.extensions.ZeebeProcessTest;
import io.camunda.zeebe.process.test.testengine.InMemoryEngine;
import io.camunda.zeebe.process.test.util.Utilities;
import io.camunda.zeebe.process.test.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.zeebe.process.test.util.Utilities.ProcessPackMessageEvent;
import io.camunda.zeebe.process.test.util.Utilities.ProcessPackMultipleTasks;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
class ProcessInstanceAssertTest {
  private static final String LINE_SEPARATOR = System.lineSeparator();
  private static final Map<String, Object> TYPED_TEST_VARIABLES = new HashMap<>();

  static {
    TYPED_TEST_VARIABLES.put("stringProperty", "stringValue");
    TYPED_TEST_VARIABLES.put("numberProperty", 123);
    TYPED_TEST_VARIABLES.put("booleanProperty", true);
    TYPED_TEST_VARIABLES.put("complexProperty", List.of("Element 1", "Element 2"));
  }

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private ZeebeClient client;
    private InMemoryEngine engine;

    @Test
    public void testProcessInstanceIsStarted() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      BpmnAssert.assertThat(instanceEvent).isStarted();
    }

    @Test
    public void testProcessInstanceIsActive() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      BpmnAssert.assertThat(instanceEvent).isActive();
    }

    @Test
    public void testProcessInstanceIsCompleted() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      BpmnAssert.assertThat(instanceEvent).isCompleted();
    }

    @Test
    public void testProcessInstanceIsNotCompleted() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      BpmnAssert.assertThat(instanceEvent).isNotCompleted();
    }

    @Test
    public void testProcessInstanceIsTerminated() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      client.newCancelInstanceCommand(instanceEvent.getProcessInstanceKey()).send().join();
      Utilities.waitForIdleState(engine);

      // then
      BpmnAssert.assertThat(instanceEvent).isTerminated();
    }

    @Test
    public void testProcessInstanceIsNotTerminated() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      BpmnAssert.assertThat(instanceEvent).isNotTerminated();
    }

    @Test
    public void testProcessInstanceHasPassedElement() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .hasPassedElement(ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasNotPassedElement() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .hasNotPassedElement(ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasPassedElementMultipleTimes() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final int totalLoops = 5;
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, totalLoops);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      for (int i = 0; i < 5; i++) {
        Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);
      }

      // then
      BpmnAssert.assertThat(instanceEvent)
          .hasPassedElement(ProcessPackLoopingServiceTask.ELEMENT_ID, totalLoops);
    }

    @Test
    public void testProcessInstanceHasPassedElementsInOrder() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .hasPassedElementInOrder(
              ProcessPackLoopingServiceTask.START_EVENT_ID,
              ProcessPackLoopingServiceTask.ELEMENT_ID,
              ProcessPackLoopingServiceTask.END_EVENT_ID);
    }

    @Test
    public void testProcessInstanceIsWaitingAt() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessIsWaitingAtMultipleElements() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isWaitingAtElement(
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAt() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isNotWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtMultipleElements() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_2);
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_3);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isNotWaitingAtElement(
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtNonExistingElement() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);
      final String nonExistingElementId = "non-existing-task";

      // then
      BpmnAssert.assertThat(instanceEvent).isNotWaitingAtElement(nonExistingElementId);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElements() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isWaitingExactlyAtElements(
              ProcessPackMultipleTasks.ELEMENT_ID_2, ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsWaitingForMessage() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, "key");

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isWaitingForMessage(ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceIsNotWaitingForMessage() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, correlationKey);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      Utilities.sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, correlationKey);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isNotWaitingForMessage(ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceHasVariable() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      BpmnAssert.assertThat(instanceEvent).hasVariable(ProcessPackLoopingServiceTask.TOTAL_LOOPS);
    }

    @Test
    public void testProcessInstanceHasVariableWithValue() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, TYPED_TEST_VARIABLES);

      // then
      final SoftAssertions softly = new SoftAssertions();

      TYPED_TEST_VARIABLES.forEach(
          (key, value) -> BpmnAssert.assertThat(instanceEvent).hasVariableWithValue(key, value));

      softly.assertAll();
    }

    @Test
    public void testHasCorrelatedMessageByName() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      Utilities.sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, correlationKey);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .hasCorrelatedMessageByName(ProcessPackMessageEvent.MESSAGE_NAME, 1);
    }

    @Test
    public void testHasCorrelatedMessageByCorrelationKey() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, correlationKey);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      Utilities.sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, correlationKey);

      // then
      BpmnAssert.assertThat(instanceEvent).hasCorrelatedMessageByCorrelationKey(correlationKey, 1);
    }

    @Test
    public void testHasAnyIncidents() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackLoopingServiceTask.TOTAL_LOOPS, "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because ProcessPackLoopingServiceTask.TOTAL_LOOPS is a string, but needs to be an int */
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      BpmnAssert.assertThat(instanceEvent).hasAnyIncidents();
    }

    @Test
    public void testHasNoIncidents() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      BpmnAssert.assertThat(instanceEvent).hasNoIncidents();
    }

    @Test
    public void testExtractLatestIncident() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackLoopingServiceTask.TOTAL_LOOPS, "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because ProcessPackLoopingServiceTask.TOTAL_LOOPS is a string, but needs to be an int */
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(instanceEvent).extractLatestIncident();

      // then

      Assertions.assertThat(incidentAssert).isNotNull();
      incidentAssert
          .isUnresolved()
          .hasErrorType(ErrorType.EXTRACT_VALUE_ERROR)
          .wasRaisedInProcessInstance(instanceEvent);
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private ZeebeClient client;
    private InMemoryEngine engine;

    @Test
    public void testProcessInstanceIsStartedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent mockInstanceEvent = mock(ProcessInstanceEvent.class);

      // when
      when(mockInstanceEvent.getProcessInstanceKey()).thenReturn(-1L);

      // then
      org.junit.jupiter.api.Assertions.assertThrows(
          AssertionError.class,
          BpmnAssert.assertThat(mockInstanceEvent)::isStarted,
          "Process with key -1 was not started");
    }

    @Test
    public void testProcessInstanceIsNotStartedIfProcessInstanceKeyNoMatch() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);
      final ProcessInstanceEvent mockInstanceEvent = mock(ProcessInstanceEvent.class);

      // when
      when(mockInstanceEvent.getProcessInstanceKey()).thenReturn(-1L);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(mockInstanceEvent).isStarted())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key -1 was not started");
    }

    @Test
    public void testProcessInstanceIsActiveFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine,
              client,
              ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // when
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).isActive())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s is not active", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsCompletedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine,
              client,
              ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).isCompleted())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s was not completed", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsNotCompletedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine,
              client,
              ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // when
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).isNotCompleted())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s was completed", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsTerminatedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine,
              client,
              ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).isTerminated())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s was not terminated", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsNotTerminatedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine,
              client,
              ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // when
      client.newCancelInstanceCommand(instanceEvent.getProcessInstanceKey()).send().join();
      Utilities.waitForIdleState(engine);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).isNotTerminated())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s was terminated", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceHasPassedElementFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine,
              client,
              ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .hasPassedElement(ProcessPackLoopingServiceTask.ELEMENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected element with id %s to be passed 1 times",
              ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasNotPassedElementFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine,
              client,
              ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // when
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .hasNotPassedElement(ProcessPackLoopingServiceTask.ELEMENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected element with id %s to be passed 0 times",
              ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasPassedElementsInOrderFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .hasPassedElementInOrder(
                          ProcessPackLoopingServiceTask.END_EVENT_ID,
                          ProcessPackLoopingServiceTask.ELEMENT_ID,
                          ProcessPackLoopingServiceTask.START_EVENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "[Ordered elements] "
                  + LINE_SEPARATOR
                  + "expected: [\"endevent\", \"servicetask\", \"startevent\"]"
                  + LINE_SEPARATOR
                  + " but was: [\"startevent\", \"servicetask\", \"endevent\"]");
    }

    @Test
    public void testProcessInstanceIsWaitingAtFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .isWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain", ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessInstanceIsWaitingAtMultipleElementsFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_2);
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_3);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .isWaitingAtElement(
                          ProcessPackMultipleTasks.ELEMENT_ID_1,
                          ProcessPackMultipleTasks.ELEMENT_ID_2,
                          ProcessPackMultipleTasks.ELEMENT_ID_3))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              "to contain:",
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceWaitingAtNonExistingElementFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);
      final String nonExistingTaskId = "non-existing-task";

      // then
      assertThatThrownBy(
              () -> BpmnAssert.assertThat(instanceEvent).isWaitingAtElement(nonExistingTaskId))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain", nonExistingTaskId);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .isNotWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtMulitpleElementsFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .isNotWaitingAtElement(
                          ProcessPackMultipleTasks.ELEMENT_ID_1,
                          ProcessPackMultipleTasks.ELEMENT_ID_2,
                          ProcessPackMultipleTasks.ELEMENT_ID_3))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              "not to contain",
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooManyElements() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .isWaitingExactlyAtElements(ProcessPackMultipleTasks.ELEMENT_ID_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              ProcessPackMultipleTasks.ELEMENT_ID_3)
          .hasMessageNotContaining(ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooLittleElements() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_2);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .isWaitingExactlyAtElements(
                          ProcessPackMultipleTasks.ELEMENT_ID_1,
                          ProcessPackMultipleTasks.ELEMENT_ID_2,
                          ProcessPackMultipleTasks.ELEMENT_ID_3))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is not waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2)
          .hasMessageNotContaining(ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_combination() {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_2);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .isWaitingExactlyAtElements(
                          ProcessPackMultipleTasks.ELEMENT_ID_1,
                          ProcessPackMultipleTasks.ELEMENT_ID_2))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is not waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              String.format(
                  "Process with key %s is waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsWaitingForMessageFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      Utilities.sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, correlationKey);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .isWaitingForMessage(ProcessPackMessageEvent.MESSAGE_NAME))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain:", ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceIsNotWaitingForMessageFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .isNotWaitingForMessage(ProcessPackMessageEvent.MESSAGE_NAME))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceHasVariableFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final String expectedVariable = "variable";
      final String actualVariable = "loopAmount";

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).hasVariable(expectedVariable))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s does not contain variable with name `%s`. Available variables are: [%s]",
              instanceEvent.getProcessInstanceKey(), expectedVariable, actualVariable);
    }

    @Test
    public void testProcessInstanceHasVariableWithValueFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final String variable = "variable";
      final String expectedValue = "expectedValue";
      final String actualValue = "actualValue";
      final Map<String, Object> variables = Collections.singletonMap(variable, actualValue);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .hasVariableWithValue(variable, expectedValue))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "The variable '%s' does not have the expected value. The value passed in"
                  + " ('%s') is internally mapped to a JSON String that yields '\"%s\"'. However, the "
                  + "actual value (as JSON String) is '\"%s\".",
              variable, expectedValue, expectedValue, actualValue);
    }

    @Test
    public void testHasCorrelatedMessageByNameFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .hasCorrelatedMessageByName(ProcessPackMessageEvent.MESSAGE_NAME, 1))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected message with name '%s' to be correlated %d times, but was %d times",
              ProcessPackMessageEvent.MESSAGE_NAME, 1, 0);
    }

    @Test
    public void testHasCorrelatedMessageByCorrelationKeyFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // then
      assertThatThrownBy(
              () ->
                  BpmnAssert.assertThat(instanceEvent)
                      .hasCorrelatedMessageByCorrelationKey(correlationKey, 1))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected message with correlation key '%s' to be correlated %d "
                  + "times, but was %d times",
              correlationKey, 1, 0);
    }

    @Test
    public void testHasAnyIncidentsFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).hasAnyIncidents())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No incidents were raised for this process instance");
    }

    @Test
    public void testHasNoIncidentsFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackLoopingServiceTask.TOTAL_LOOPS, "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because ProcessPackLoopingServiceTask.TOTAL_LOOPS is a string, but needs to be an int */
      Utilities.completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).hasNoIncidents())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Incidents were raised for this process instance");
    }

    @Test
    public void testExtractLatestIncidentFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).extractLatestIncident())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No incidents were raised for this process instance");
    }
  }

  // These tests validate bug fixes for bugs that have occurred in the past
  @Nested
  class RegressionTests {

    private ZeebeClient client;
    private InMemoryEngine engine;

    @Test // regression test for #78
    public void testShouldCaptureLatestValueOfVariable() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables1 =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, "1");

      final Map<String, Object> variables2 =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, "2");

      final Map<String, Object> variables3 =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, "3");

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables1);

      client
          .newSetVariablesCommand(instanceEvent.getProcessInstanceKey())
          .variables(variables2)
          .send()
          .join();
      client
          .newSetVariablesCommand(instanceEvent.getProcessInstanceKey())
          .variables(variables3)
          .send()
          .join();

      Utilities.waitForIdleState(engine);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .hasVariableWithValue(ProcessPackLoopingServiceTask.TOTAL_LOOPS, "3");
    }
  }
}
