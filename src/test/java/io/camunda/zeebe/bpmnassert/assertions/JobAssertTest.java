package io.camunda.zeebe.bpmnassert.assertions;

import static io.camunda.zeebe.bpmnassert.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.bpmnassert.util.Utilities.ProcessPackLoopingServiceTask;
import static io.camunda.zeebe.bpmnassert.util.Utilities.activateSingleJob;
import static io.camunda.zeebe.bpmnassert.util.Utilities.deployProcess;
import static io.camunda.zeebe.bpmnassert.util.Utilities.startProcessInstance;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.data.Offset.offset;

import io.camunda.zeebe.bpmnassert.extensions.ZeebeProcessTest;
import io.camunda.zeebe.bpmnassert.testengine.InMemoryEngine;
import io.camunda.zeebe.bpmnassert.testengine.RecordStreamSource;
import io.camunda.zeebe.bpmnassert.util.Utilities;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
class JobAssertTest {

  public static final String WRONG_VALUE = "wrong value";
  public static final String ERROR_CODE = "error";
  public static final String ERROR_MSG = "error occurred";

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {
    private ZeebeClient client;
    private RecordStreamSource recordStreamSource;
    private InMemoryEngine engine;

    @Test
    void testHasElementId() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasElementId(ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    void testHasDeadline() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

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
    void testHasBpmnProcessId() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    void testHasRetries() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasRetries(1);
    }

    @Test
    void testHasAnyIncidents() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MSG)
          .send()
          .join();

      // then

      assertThat(actual).hasAnyIncidents();
    }

    @Test
    void testHasNoIncidents() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).hasNoIncidents();
    }

    @Test
    void testExtractLatestIncident() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MSG)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      // then
      Assertions.assertThat(incidentAssert).isNotNull();
      incidentAssert
          .isUnresolved()
          .hasErrorType(ErrorType.UNHANDLED_ERROR_EVENT)
          .occurredDuringJob(actual);
    }

    @Test
    void testExtractingVariables() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual)
          .extractingVariables()
          .containsOnly(
              entry(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1), entry("loopAmount", 0));
    }

    @Test
    void testExtractingHeaders() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThat(actual).extractingHeaders().isEmpty();
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {
    private RecordStreamSource recordStreamSource;
    private ZeebeClient client;
    private InMemoryEngine engine;

    @Test
    void testHasElementIdFailure() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> assertThat(actual).hasElementId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job is not associated with expected element id '%s' but is instead associated with '%s'.",
              WRONG_VALUE, ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    void testHasDeadlineFailure() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

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
    void testHasBpmnProcessIdFailure() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> assertThat(actual).hasBpmnProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job is not associated with BPMN process id '%s' but is instead associated with '%s'.",
              WRONG_VALUE, ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    void testHasRetriesFailure() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> assertThat(actual).hasRetries(12345))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job does not have %d retries, as expected, but instead has %d retries.", 12345, 1);
    }

    @Test
    void testHasAnyIncidentsFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> assertThat(actual).hasAnyIncidents())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No incidents were raised for this job");
    }

    @Test
    void testHasNoIncidentsFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MSG)
          .send()
          .join();

      // then
      assertThatThrownBy(() -> assertThat(actual).hasNoIncidents())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Incidents were raised for this job");
    }

    @Test
    void testExtractLatestIncidentFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> assertThat(actual).extractLatestIncident())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No incidents were raised for this job");
    }
  }
}
