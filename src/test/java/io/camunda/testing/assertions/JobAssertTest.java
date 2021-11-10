package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.BpmnAssert.assertThat;
import static io.camunda.testing.util.Utilities.deployProcess;
import static io.camunda.testing.util.Utilities.startProcessInstance;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.data.Offset.offset;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.testing.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeAssertions
class JobAssertTest {

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
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasElementId(ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    void testHasDeadline() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final long expectedDeadline = System.currentTimeMillis() + 100;
      final ActivateJobsResponse jobActivationResponse =
          client
              .newActivateJobsCommand()
              .jobType(ProcessPackLoopingServiceTask.JOB_TYPE)
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
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    void testHasRetries() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasRetries(1);
    }

    @Test
    void testHasAnyIncidents() throws InterruptedException {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode("error")
          .errorMessage("error occurred")
          .send()
          .join();

      // then

      assertThat(actual).hasAnyIncidents();
    }

    @Test
    void testHasNoIncidents() throws InterruptedException {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasNoIncidents();
    }

    @Test
    void testExtractingVariables() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual)
          .extractingVariables()
          .containsOnly(
              entry(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1), entry("loopAmount", 0));
    }

    @Test
    void testExtractingHeaders() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

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
        .jobType(ProcessPackLoopingServiceTask.JOB_TYPE)
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
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> assertThat(actual).hasElementId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job is not associated with expected element id '%s' but is instead associated with '%s'.",
              WRONG_VALUE, ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    void testHasDeadlineFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final long expectedDeadline = System.currentTimeMillis() + 100;
      final ActivateJobsResponse jobActivationResponse =
          client
              .newActivateJobsCommand()
              .jobType(ProcessPackLoopingServiceTask.JOB_TYPE)
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
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> assertThat(actual).hasBpmnProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job is not associated with BPMN process id '%s' but is instead associated with '%s'.",
              WRONG_VALUE, ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    void testHasRetriesFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> assertThat(actual).hasRetries(12345))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job does not have %d retries, as expected, but instead has %d retries.", 12345, 1);
    }

    @Test
    void testHasAnyIncidentsFailure() throws InterruptedException {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> assertThat(actual).hasAnyIncidents())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No incidents were raised for this job");
    }

    @Test
    void testHasNoIncidentsFailure() throws InterruptedException {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode("error")
          .errorMessage("error occurred")
          .send()
          .join();

      // then
      assertThatThrownBy(() -> assertThat(actual).hasNoIncidents())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Incidents were raised for this job");
    }
  }
}
