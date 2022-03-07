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
package io.camunda.zeebe.process.test.qa.testcontainer.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.IncidentAssert;
import io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest;
import io.camunda.zeebe.process.test.qa.util.Utilities;
import io.camunda.zeebe.process.test.qa.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.zeebe.process.test.qa.util.Utilities.ProcessPackMessageEvent;
import io.camunda.zeebe.process.test.qa.util.Utilities.ProcessPackMultipleTasks;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
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
    TYPED_TEST_VARIABLES.put("complexProperty", Arrays.asList("Element 1", "Element 2"));
  }

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private ZeebeClient client;
    private ZeebeTestEngine engine;

    @Test
    void testProcessInstanceIsStarted() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsActive() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsCompleted() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsNotCompleted() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsTerminated() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      client.newCancelInstanceCommand(instanceEvent.getProcessInstanceKey()).send().join();
      Utilities.waitForIdleState(engine, Duration.ofSeconds(1));

      // then
      BpmnAssert.assertThat(instanceEvent).isTerminated();
    }

    @Test
    void testProcessInstanceIsNotTerminated() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceHasPassedElement() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceHasNotPassedElement() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceHasPassedElementMultipleTimes()
        throws InterruptedException, TimeoutException {
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
    void testProcessInstanceHasPassedElementsInOrder()
        throws InterruptedException, TimeoutException {
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
          .hasPassedElementsInOrder(
              ProcessPackLoopingServiceTask.START_EVENT_ID,
              ProcessPackLoopingServiceTask.ELEMENT_ID,
              ProcessPackLoopingServiceTask.END_EVENT_ID);
    }

    @Test
    void testProcessInstanceIsWaitingAt() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isWaitingAtElements(ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    void testProcessIsWaitingAtMultipleElements() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isWaitingAtElements(
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    void testProcessInstanceIsNotWaitingAt() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      Utilities.completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);

      // then
      BpmnAssert.assertThat(instanceEvent)
          .isNotWaitingAtElements(ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    void testProcessInstanceIsNotWaitingAtMultipleElements()
        throws InterruptedException, TimeoutException {
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
          .isNotWaitingAtElements(
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    void testProcessInstanceIsNotWaitingAtNonExistingElement()
        throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);
      final String nonExistingElementId = "non-existing-task";

      // then
      BpmnAssert.assertThat(instanceEvent).isNotWaitingAtElements(nonExistingElementId);
    }

    @Test
    void testProcessInstanceIsWaitingExactlyAtElements()
        throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsWaitingForMessage() throws InterruptedException, TimeoutException {
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
          .isWaitingForMessages(ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    void testProcessInstanceIsNotWaitingForMessage() throws InterruptedException, TimeoutException {
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
          .isNotWaitingForMessages(ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    void testProcessInstanceHasVariable() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceHasVariableWithValue() throws InterruptedException, TimeoutException {
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
    void testHasCorrelatedMessageByName() throws InterruptedException, TimeoutException {
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
    void testHasCorrelatedMessageByCorrelationKey() throws InterruptedException, TimeoutException {
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
    void testHasAnyIncidents() throws InterruptedException, TimeoutException {
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
    void testHasNoIncidents() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      BpmnAssert.assertThat(instanceEvent).hasNoIncidents();
    }

    @Test
    void testExtractLatestIncident() throws InterruptedException, TimeoutException {
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
          BpmnAssert.assertThat(instanceEvent).extractingLatestIncident();

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
    private ZeebeTestEngine engine;

    @Test
    void testProcessInstanceIsStartedFailure() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsNotStartedIfProcessInstanceKeyNoMatch()
        throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsActiveFailure() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsCompletedFailure() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsNotCompletedFailure() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsTerminatedFailure() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsNotTerminatedFailure() throws InterruptedException, TimeoutException {
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
      Utilities.waitForIdleState(engine, Duration.ofSeconds(1));

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).isNotTerminated())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s was terminated", instanceEvent.getProcessInstanceKey());
    }

    @Test
    void testProcessInstanceHasPassedElementFailure()
        throws InterruptedException, TimeoutException {
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
              "Expected element with id %s to be passed 1 times, but was 0",
              ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    void testProcessInstanceHasNotPassedElementFailure()
        throws InterruptedException, TimeoutException {
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
              "Expected element with id %s to be passed 0 times, but was 1",
              ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    void testProcessInstanceHasPassedElementsInOrderFailure()
        throws InterruptedException, TimeoutException {
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
                  .hasPassedElementsInOrder(
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
    void testProcessInstanceIsWaitingAtFailure() throws InterruptedException, TimeoutException {
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
                  .isWaitingAtElements(ProcessPackMultipleTasks.ELEMENT_ID_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain", ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    void testProcessInstanceIsWaitingAtMultipleElementsFailure()
        throws InterruptedException, TimeoutException {
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
                  .isWaitingAtElements(
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
    void testProcessInstanceWaitingAtNonExistingElementFailure()
        throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);
      final String nonExistingTaskId = "non-existing-task";

      // then
      assertThatThrownBy(
          () -> BpmnAssert.assertThat(instanceEvent).isWaitingAtElements(nonExistingTaskId))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain", nonExistingTaskId);
    }

    @Test
    void testProcessInstanceIsNotWaitingAtFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThatThrownBy(
          () ->
              BpmnAssert.assertThat(instanceEvent)
                  .isNotWaitingAtElements(ProcessPackMultipleTasks.ELEMENT_ID_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    void testProcessInstanceIsNotWaitingAtMulitpleElementsFailure()
        throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThatThrownBy(
          () ->
              BpmnAssert.assertThat(instanceEvent)
                  .isNotWaitingAtElements(
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
    void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooManyElements()
        throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooLittleElements()
        throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsWaitingExactlyAtElementsFailure_combination()
        throws InterruptedException, TimeoutException {
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
    void testProcessInstanceIsWaitingForMessageFailure()
        throws InterruptedException, TimeoutException {
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
                  .isWaitingForMessages(ProcessPackMessageEvent.MESSAGE_NAME))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain:", ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    void testProcessInstanceIsNotWaitingForMessageFailure()
        throws InterruptedException, TimeoutException {
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
                  .isNotWaitingForMessages(ProcessPackMessageEvent.MESSAGE_NAME))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    void testProcessInstanceHasVariableFailure() throws InterruptedException, TimeoutException {
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
    void testProcessInstanceHasVariableWithValueFailure()
        throws InterruptedException, TimeoutException {
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
                  + "actual value (as JSON String) is '\"%s\"'.",
              variable, expectedValue, expectedValue, actualValue);
    }

    @Test
    void testHasCorrelatedMessageByNameFailure() throws InterruptedException, TimeoutException {
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
    void testHasCorrelatedMessageByCorrelationKeyFailure()
        throws InterruptedException, TimeoutException {
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
    void testHasAnyIncidentsFailure() throws InterruptedException, TimeoutException {
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
    void testHasNoIncidentsFailure() throws InterruptedException, TimeoutException {
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
    void testExtractLatestIncidentFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(instanceEvent).extractingLatestIncident())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No incidents were raised for this process instance");
    }
  }

  // These tests validate bug fixes for bugs that have occurred in the past
  @Nested
  class RegressionTests {

    private ZeebeClient client;
    private ZeebeTestEngine engine;

    @Test // regression test for #78
    public void testShouldCaptureLatestValueOfVariable()
        throws InterruptedException, TimeoutException {
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

      Utilities.waitForIdleState(engine, Duration.ofSeconds(1));

      // then
      BpmnAssert.assertThat(instanceEvent)
          .hasVariableWithValue(ProcessPackLoopingServiceTask.TOTAL_LOOPS, "3");
    }
  }
}
