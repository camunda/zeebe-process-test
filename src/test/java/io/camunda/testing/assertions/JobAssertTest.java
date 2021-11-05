package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.BpmnAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.data.Offset.offset;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// TODO remove Thread.sleeps
@ZeebeAssertions
class JobAssertTest {

  public static final String PROCESS_INSTANCE_BPMN = "looping-servicetask.bpmn";
  public static final String PROCESS_INSTANCE_ID = "looping-servicetask";
  public static final String ELEMENT_ID = "servicetask";
  public static final String WRONG_VALUE = "wrong value";

  private ZeebeClient client;
  private ZeebeEngine engine;

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    void testHasElementId() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          client.newActivateJobsCommand().jobType("test").maxJobsToActivate(1).send().join();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasElementId(ELEMENT_ID);
    }

    @Test
    void testHasDeadline() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final long expectedDeadline = System.currentTimeMillis() + 100;
      final ActivateJobsResponse jobActivationResponse =
          client
              .newActivateJobsCommand()
              .jobType("test")
              .maxJobsToActivate(1)
              .timeout(Duration.ofMillis(100))
              .send()
              .join();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasDeadline(expectedDeadline, offset(20L));
    }

    @Test
    void testHasBpmnProcessId() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          client.newActivateJobsCommand().jobType("test").maxJobsToActivate(1).send().join();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasBpmnProcessId(PROCESS_INSTANCE_ID);
    }

    @Test
    void testHasRetries() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          client.newActivateJobsCommand().jobType("test").maxJobsToActivate(1).send().join();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasRetries(1);
    }

    @Test
    void testExtractingVariables() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          client.newActivateJobsCommand().jobType("test").maxJobsToActivate(1).send().join();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual)
          .extractingVariables()
          .containsOnly(entry("totalLoops", 1), entry("loopAmount", 0));
    }

    @Test
    void testExtractingHeaders() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          client.newActivateJobsCommand().jobType("test").maxJobsToActivate(1).send().join();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).extractingHeaders().isEmpty();
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {
    private RecordStreamSource recordStreamSource;

    @Test
    void testHasElementIdFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          client.newActivateJobsCommand().jobType("test").maxJobsToActivate(1).send().join();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> assertThat(actual).hasElementId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(ELEMENT_ID, WRONG_VALUE);
    }

    @Test
    void testHasDeadlineFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final long expectedDeadline = System.currentTimeMillis() + 100;
      final ActivateJobsResponse jobActivationResponse =
          client
              .newActivateJobsCommand()
              .jobType("test")
              .maxJobsToActivate(1)
              .timeout(Duration.ofMillis(100))
              .send()
              .join();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> assertThat(actual).hasDeadline(-1, offset(20L)))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("Deadline", "-1", "20");
    }

    @Test
    void testHasBpmnProcessIdFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          client.newActivateJobsCommand().jobType("test").maxJobsToActivate(1).send().join();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> assertThat(actual).hasBpmnProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(PROCESS_INSTANCE_ID, WRONG_VALUE);
    }

    @Test
    void testHasRetriesFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          client.newActivateJobsCommand().jobType("test").maxJobsToActivate(1).send().join();

      // then

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> assertThat(actual).hasRetries(12345))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("1", "12345");
    }
  }

  private void deployProcess(final String process) {
    client.newDeployCommand().addResourceFromClasspath(process).send().join();
  }

  private ProcessInstanceEvent startProcessInstance(final String processId)
      throws InterruptedException {
    return startProcessInstance(processId, new HashMap<>());
  }

  private ProcessInstanceEvent startProcessInstance(
      final String processId, final Map<String, Object> variables) throws InterruptedException {
    final ProcessInstanceEvent instanceEvent =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(variables)
            .send()
            .join();
    Thread.sleep(100);
    return instanceEvent;
  }

  // TODO we need a proper way to complete jobs instead of this hack
  private void completeTask(final String elementId) throws InterruptedException {
    Thread.sleep(100);
    Record<JobRecordValue> lastRecord = null;
    for (final Record<JobRecordValue> record : engine.jobRecords().withElementId(elementId)) {
      if (record.getIntent().equals(JobIntent.CREATED)) {
        lastRecord = record;
      }
    }
    if (lastRecord != null) {
      client.newCompleteCommand(lastRecord.getKey()).send().join();
    }
    Thread.sleep(100);
  }

  private void sendMessage(final String messsageName, final String correlationKey)
      throws InterruptedException {
    client
        .newPublishMessageCommand()
        .messageName(messsageName)
        .correlationKey(correlationKey)
        .send()
        .join();
    Thread.sleep(100);
  }
}
