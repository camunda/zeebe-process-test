package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.BpmnAssert.assertThat;
import static io.camunda.testing.util.Utilities.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.camunda.community.eze.ZeebeEngineClock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeAssertions
class MessageAssertTest {

  public static final String CORRELATION_KEY = "correlationkey";
  public static final String WRONG_CORRELATION_KEY = "wrongcorrelationkey";
  public static final String WRONG_MESSAGE_NAME = "wrongmessagename";

  private ZeebeEngine engine;
  private ZeebeEngineClock clock;

  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;
    private ZeebeClient client;

    @Test
    void testHasBeenCorrelated() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThat(response).hasBeenCorrelated();
    }

    @Test
    void testHasMessageStartEventBeenCorrelated() {
      // given
      deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(
              engine,
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThat(response).hasCreatedProcessInstance();
    }

    @Test
    void testHasNotBeenCorrelated() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThat(response).hasNotBeenCorrelated();
    }

    @Test
    void testHasMessageStartEventNotBeenCorrelated() {
      // given
      deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(
              engine, client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThat(response).hasNotCreatedProcessInstance();
    }

    @Test
    void testHasExpired() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Duration timeToLive = Duration.ofDays(1);

      // when
      final PublishMessageResponse response =
          sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY, timeToLive);
      increaseTime(engine, timeToLive.plusMinutes(1));

      // then
      assertThat(response).hasExpired();
    }

    @Test
    void testHasNotExpired() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThat(response).hasNotExpired();
    }

    @Test
    void testExtractingProcessInstance() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThat(response).extractingProcessInstance().isCompleted();
    }

    @Test
    void testExtractingProcessInstance_messageStartEvent() {
      // given
      deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(
              engine,
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThat(response).extractingProcessInstance().isCompleted();
    }
  }

  @Nested
  class UnhappyPathTests {

    private RecordStreamSource recordStreamSource;
    private ZeebeClient client;

    @Test
    void testHasBeenCorrelatedFailure() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).hasBeenCorrelated())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d was not correlated", response.getMessageKey());
    }

    @Test
    void testHasMessageStartEventBeenCorrelatedFailure() {
      // given
      deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(
              engine, client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).hasCreatedProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Message with key %d did not lead to the creation of a process instance",
              response.getMessageKey());
    }

    @Test
    void testHasNotBeenCorrelatedFailure() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).hasNotBeenCorrelated())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Message with key %d was correlated to process instance %s",
              response.getMessageKey(), instanceEvent.getProcessInstanceKey());
    }

    @Test
    void testHasMessageStartEventNotBeenCorrelatedFailure() {
      // given
      deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(
              engine,
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).hasNotCreatedProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessageContaining(
              "Message with key %d was correlated to process instance", response.getMessageKey());
    }

    @Test
    void testHasExpiredFailure() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).hasExpired())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d has not expired", response.getMessageKey());
    }

    @Test
    void testHasNotExpiredFailure() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Duration timeToLive = Duration.ofDays(1);

      // when
      final PublishMessageResponse response =
          sendMessage(
              engine, client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY, timeToLive);
      increaseTime(engine, timeToLive.plusMinutes(1));

      // then
      assertThatThrownBy(() -> assertThat(response).hasNotExpired())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d has expired", response.getMessageKey());
    }

    @Test
    void testExtractingProcessInstanceFailure() {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      startProcessInstance(engine, client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          sendMessage(engine, client, ProcessPackMessageEvent.MESSAGE_NAME, WRONG_CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).extractingProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one correlated process instance for message key %d but found %d: %s",
              response.getMessageKey(), 0, "[]");
    }

    @Test
    void testExtractingProcessInstanceFailure_messageStartEvent() {
      // given
      deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(
              engine, client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).extractingProcessInstance())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one correlated process instance for message key %d but found %d: %s",
              response.getMessageKey(), 0, "[]");
    }
  }
}
