package io.camunda.zeebe.process.test.assertions;

import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.process.test.util.Utilities.startProcessInstance;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.data.Offset.offset;

import io.camunda.zeebe.process.test.extensions.ZeebeProcessTest;
import io.camunda.zeebe.process.test.testengine.InMemoryEngine;
import io.camunda.zeebe.process.test.util.Utilities;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.process.test.util.Utilities.ProcessPackLoopingServiceTask;
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
    private InMemoryEngine engine;

    @Test
    void testHasElementId() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      BpmnAssert.assertThat(actual).hasElementId(ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    void testHasDeadline() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

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
      BpmnAssert.assertThat(actual).hasDeadline(expectedDeadline, offset(20L));
    }

    @Test
    void testHasBpmnProcessId() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      BpmnAssert.assertThat(actual).hasBpmnProcessId(ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    void testHasRetries() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      BpmnAssert.assertThat(actual).hasRetries(1);
    }

    @Test
    void testHasAnyIncidents() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MSG);

      // then

      BpmnAssert.assertThat(actual).hasAnyIncidents();
    }

    @Test
    void testHasNoIncidents() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      BpmnAssert.assertThat(actual).hasNoIncidents();
    }

    @Test
    void testExtractLatestIncident() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MSG);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

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
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      BpmnAssert.assertThat(actual)
          .extractingVariables()
          .containsOnly(
              entry(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1), entry("loopAmount", 0));
    }

    @Test
    void testExtractingHeaders() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      BpmnAssert.assertThat(actual).extractingHeaders().isEmpty();
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private ZeebeClient client;
    private InMemoryEngine engine;

    @Test
    void testHasElementIdFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> BpmnAssert.assertThat(actual).hasElementId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job is not associated with expected element id '%s' but is instead associated with '%s'.",
              WRONG_VALUE, ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    void testHasDeadlineFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

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
      assertThatThrownBy(() -> BpmnAssert.assertThat(actual).hasDeadline(-1, offset(20L)))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("Deadline", "-1", "20");
    }

    @Test
    void testHasBpmnProcessIdFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      assertThatThrownBy(() -> BpmnAssert.assertThat(actual).hasBpmnProcessId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Job is not associated with BPMN process id '%s' but is instead associated with '%s'.",
              WRONG_VALUE, ProcessPackLoopingServiceTask.PROCESS_ID);
    }

    @Test
    void testHasRetriesFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> BpmnAssert.assertThat(actual).hasRetries(12345))
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
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> BpmnAssert.assertThat(actual).hasAnyIncidents())
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
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MSG);

      // then
      assertThatThrownBy(() -> BpmnAssert.assertThat(actual).hasNoIncidents())
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
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      // then
      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      assertThatThrownBy(() -> BpmnAssert.assertThat(actual).extractLatestIncident())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No incidents were raised for this job");
    }
  }
}
