package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.BpmnAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.data.Offset.offset;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import java.time.Duration;
import java.util.Collections;
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
  private static final String TOTAL_LOOPS = "totalLoops";
  private static final String JOB_TYPE_TEST = "test";

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
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasElementId(ELEMENT_ID);
    }

    @Test
    void testHasDeadline() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final long expectedDeadline = System.currentTimeMillis() + 100;
      final ActivateJobsResponse jobActivationResponse =
          client
              .newActivateJobsCommand()
              .jobType(JOB_TYPE_TEST)
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
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasBpmnProcessId(PROCESS_INSTANCE_ID);
    }

    @Test
    void testHasRetries() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasRetries(1);
    }

    @Test
    void testExtractingVariables() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual)
          .extractingVariables()
          .containsOnly(entry(TOTAL_LOOPS, 1), entry("loopAmount", 0));
    }

    @Test
    void testExtractingHeaders() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).extractingHeaders().isEmpty();
    }
  }

  private ActivateJobsResponse activateSingleJob() {
    return client
        .newActivateJobsCommand()
        .jobType(JOB_TYPE_TEST)
        .maxJobsToActivate(1)
        .send()
        .join();
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {
    private RecordStreamSource recordStreamSource;

    @Test
    void testHasElementIdFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> assertThat(actual).hasElementId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job is not associated with expected element id '%s' but is instead associated with '%s'.",
              WRONG_VALUE, ELEMENT_ID);
    }

    @Test
    void testHasDeadlineFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final long expectedDeadline = System.currentTimeMillis() + 100;
      final ActivateJobsResponse jobActivationResponse =
          client
              .newActivateJobsCommand()
              .jobType(JOB_TYPE_TEST)
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
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> assertThat(actual).hasBpmnProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job is not associated with BPMN process id '%s' but is instead associated with '%s'.",
              WRONG_VALUE, PROCESS_INSTANCE_ID);
    }

    @Test
    void testHasRetriesFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(TOTAL_LOOPS, 1);
      startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> assertThat(actual).hasRetries(12345))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job does not have %d retries, as expected, but instead has %d retries.", 12345, 1);
    }
  }

  private void deployProcess(final String process) {
    client.newDeployCommand().addResourceFromClasspath(process).send().join();
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
}
