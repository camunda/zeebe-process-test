package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.BpmnAssert.assertThat;
import static io.camunda.testing.util.Utilities.deployProcess;
import static io.camunda.testing.util.Utilities.increaseTime;
import static io.camunda.testing.util.Utilities.sendMessage;
import static io.camunda.testing.util.Utilities.startProcessInstance;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.testing.util.Utilities.ProcessPackMessageEvent;
import io.camunda.testing.util.Utilities.ProcessPackMessageStartEvent;
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

  private ZeebeClient client;
  private ZeebeEngine engine;
  private ZeebeEngineClock clock;

  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    void testHasBeenCorrelated() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThat(response).hasBeenCorrelated();
    }

    @Test
    void testHasNotBeenCorrelated() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThat(response).hasNotBeenCorrelated();
    }

    @Test
    void testHasExpired() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Duration timeToLive = Duration.ofDays(1);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY, timeToLive);
      increaseTime(clock, timeToLive.plusMinutes(1));

      // then
      assertThat(response).hasExpired();
    }

    @Test
    void testHasNotExpired() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThat(response).hasNotExpired();
    }

    @Test
    void testExtractingProcessInstance() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThat(response).extractingProcessInstance(false).isCompleted();
    }

    @Test
    void testExtractingProcessInstance_messageStartEvent() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(
              client,
              ProcessPackMessageStartEvent.MESSAGE_NAME,
              ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThat(response).extractingProcessInstance(true).isCompleted();
    }
  }

  @Nested
  class UnhappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    void testHasBeenCorrelatedFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).hasBeenCorrelated())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d was not correlated", response.getMessageKey());
    }

    @Test
    void testHasNotBeenCorrelatedFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).hasNotBeenCorrelated())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Message with key %d was correlated to process instance %s",
              response.getMessageKey(), instanceEvent.getProcessInstanceKey());
    }

    @Test
    void testHasExpiredFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).hasExpired())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d was not expired", response.getMessageKey());
    }

    @Test
    void testHasNotExpiredFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Duration timeToLive = Duration.ofDays(1);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, CORRELATION_KEY, timeToLive);
      increaseTime(clock, timeToLive.plusMinutes(1));

      // then
      assertThatThrownBy(() -> assertThat(response).hasNotExpired())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Message with key %d was expired", response.getMessageKey());
    }

    @Test
    void testExtractingProcessInstanceFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, CORRELATION_KEY);
      startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      final PublishMessageResponse response =
          sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, WRONG_CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).extractingProcessInstance(false))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one correlated process instance for message key %d but found %d: %s",
              response.getMessageKey(), 0, "[]");
    }

    @Test
    void testExtractingProcessInstanceFailure_messageStartEvent() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageStartEvent.RESOURCE_NAME);

      // when
      final PublishMessageResponse response =
          sendMessage(client, WRONG_MESSAGE_NAME, ProcessPackMessageStartEvent.CORRELATION_KEY);

      // then
      assertThatThrownBy(() -> assertThat(response).extractingProcessInstance(true))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected to find one correlated process instance for message key %d but found %d: %s",
              response.getMessageKey(), 0, "[]");
    }
  }
}
