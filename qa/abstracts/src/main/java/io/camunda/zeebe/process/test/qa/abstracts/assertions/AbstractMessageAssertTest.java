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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackMessageEvent;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackMessageStartEvent;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public abstract class AbstractMessageAssertTest {

  public static final String CORRELATION_KEY = "correlationkey";
  public static final String WRONG_CORRELATION_KEY = "wrongcorrelationkey";
  public static final String WRONG_MESSAGE_NAME = "wrongmessagename";

  @Nested
  class HappyPathTests {

    private ZeebeClient client;
    private ZeebeTestEngine engine;

    @Test
    void testHasBeenCorrelated() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      Utilities.startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).hasBeenCorrelated();
    }

    @Test
    void testHasMessageStartEventBeenCorrelated() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).hasCreatedProcessInstance();
    }

    @Test
    void testHasNotBeenCorrelated() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).hasNotBeenCorrelated();
    }

    @Test
    void testHasMessageStartEventNotBeenCorrelated() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).hasNotCreatedProcessInstance();
    }

    @Test
    void testHasExpired() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Duration timeToLive = Duration.ofDays(1);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageEvent.MESSAGE_NAME,
              CORRELATION_KEY,
              timeToLive,
              Collections.emptyMap());
      Utilities.increaseTime(engine, timeToLive.plusMinutes(1));

      // then
      BpmnAssert.assertThat(response).hasExpired();
    }

    @Test
    void testHasNotExpired() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageEvent.MESSAGE_NAME,
              CORRELATION_KEY,
              Duration.ofMinutes(1),
              Collections.emptyMap());

      // then
      BpmnAssert.assertThat(response).hasNotExpired();
    }

    @Test
    void testExtractingProcessInstance() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      Utilities.startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).extractingProcessInstance().isCompleted();
    }

    @Test
    void testExtractingProcessInstance_messageStartEvent()
        throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      BpmnAssert.assertThat(response).extractingProcessInstance().isCompleted();
    }
  }

  @Nested
  class UnhappyPathTests {

    private ZeebeClient client;
    private ZeebeTestEngine engine;

    @Test
    void testHasBeenCorrelatedFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasBeenCorrelated())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d was not correlated", response.getMessageKey());
    }

    @Test
    void testHasMessageStartEventBeenCorrelatedFailure()
        throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasCreatedProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Message with key %d did not lead to the creation of a process instance",
              response.getMessageKey());
    }

    @Test
    void testHasNotBeenCorrelatedFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasNotBeenCorrelated())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Message with key %d was correlated to process instance %s",
              response.getMessageKey(), instanceEvent.getProcessInstanceKey());
    }

    @Test
    void testHasMessageStartEventNotBeenCorrelatedFailure()
        throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasNotCreatedProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessageContaining(
              "Message with key %d was correlated to process instance", response.getMessageKey());
    }

    @Test
    void testHasExpiredFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageEvent.MESSAGE_NAME,
              CORRELATION_KEY,
              Duration.ofMinutes(1),
              Collections.emptyMap());

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasExpired())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d has not expired", response.getMessageKey());
    }

    @Test
    void testHasNotExpiredFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Duration timeToLive = Duration.ofDays(1);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine,
              client,
              ProcessPackMessageEvent.MESSAGE_NAME,
              CORRELATION_KEY,
              timeToLive,
              Collections.emptyMap());
      Utilities.increaseTime(engine, timeToLive.plusMinutes(1));

      // then
      try {
        assertThatThrownBy(() -> BpmnAssert.assertThat(response).hasNotExpired())
            .isInstanceOf(AssertionError.class)
            .hasMessage("Message with key %d has expired", response.getMessageKey());
      } catch (final Exception e) {
        // Logging current stream of events for assert troubleshooting for this flaky test
        // https://github.com/camunda/zeebe-process-test/issues/960
        // this code should be removed once the flakiness is resolved
        RecordStream.of(engine.getRecordStreamSource()).print(true);
        throw e;
      }
    }

    @Test
    void testExtractingProcessInstanceFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      Utilities.startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, WRONG_CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).extractingProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one correlated process instance for message key %d but found %d: %s",
              response.getMessageKey(), 0, "[]");
    }

    @Test
    void testExtractingProcessInstanceFailure_messageStartEvent()
        throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          Utilities.sendMessage(
              engine, client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(response).extractingProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one correlated process instance for message key %d but found %d: %s",
              response.getMessageKey(), 0, "[]");
    }
  }
}
