package io.camunda.zeebe.process.test.qa.regular.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.IncidentAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import io.camunda.zeebe.process.test.qa.util.Utilities;
import io.camunda.zeebe.process.test.qa.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import java.util.Collections;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.StringAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
class IncidentAssertTest {

  public static final String WRONG_VALUE = "wrong value";
  public static final String ERROR_CODE = "error";
  public static final String ERROR_MESSAGE = "error occurred";

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private ZeebeClient client;
    private InMemoryEngine engine;

    @Test
    void testHasErrorType() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      // then
      incidentAssert.hasErrorType(ErrorType.UNHANDLED_ERROR_EVENT);
    }

    @Test
    void testHasErrorMessage() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      // then
      incidentAssert.hasErrorMessage(
          "Expected to throw an error event with the code 'error' with message 'error occurred', but it was not caught. No error events are available in the scope.");
    }

    @Test
    void testExtractErrorMessage() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      final StringAssert messageAssert = incidentAssert.extractErrorMessage();

      // then
      Assertions.assertThat(messageAssert).isNotNull();
      messageAssert.isEqualTo(
          "Expected to throw an error event with the code 'error' with message 'error occurred', but it was not caught. No error events are available in the scope.");
    }

    @Test
    void testWasRaisedInProcessInstance() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent processInstanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      // then
      incidentAssert.wasRaisedInProcessInstance(processInstanceEvent);
      incidentAssert.wasRaisedInProcessInstance(processInstanceEvent.getProcessInstanceKey());
    }

    @Test
    void testOccurredOnElement() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackLoopingServiceTask.TOTAL_LOOPS, "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because VAR_TOTAL_LOOPS is a string, but needs to be an int */
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);
      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      client.newCompleteCommand(job.getKey()).send().join();

      engine.waitForIdleState();

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(instanceEvent).extractLatestIncident();

      // then
      incidentAssert.occurredOnElement(ProcessPackLoopingServiceTask.GATEWAY_ELEMENT_ID);
    }

    @Test
    void testOccurredDuringJob() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, job.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(job).extractLatestIncident();

      // then
      incidentAssert.occurredDuringJob(job);
      incidentAssert.occurredDuringJob(job.getKey());
    }

    @Test
    void testIsResolved() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      final long incidentKey = incidentAssert.getIncidentKey();
      client.newResolveIncidentCommand(incidentKey).send().join();

      Utilities.waitForIdleState(engine);

      // then
      incidentAssert.isResolved();
    }

    @Test
    void testIsUnresolved() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      // then
      incidentAssert.isUnresolved();
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private ZeebeClient client;
    private InMemoryEngine engine;

    @Test
    void testHasErrorTypeFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.hasErrorType(ErrorType.IO_MAPPING_ERROR))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Error type was not 'IO_MAPPING_ERROR' but was 'UNHANDLED_ERROR_EVENT' instead");
    }

    @Test
    void testHasErrorMessageFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.hasErrorMessage(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Error message was not 'wrong value' but was 'Expected to throw an error event with the code 'error' with message 'error occurred', but it was not caught. No error events are available in the scope.' instead");
    }

    @Test
    void testWasRaisedInProcessInstanceFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent processInstanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.wasRaisedInProcessInstance(-1))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Incident was not raised in process instance -1 but was raised in %d instead",
              processInstanceEvent.getProcessInstanceKey());
    }

    @Test
    void testOccurredOnElementFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackLoopingServiceTask.TOTAL_LOOPS, "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because VAR_TOTAL_LOOPS is a string, but needs to be an int */
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);
      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      client.newCompleteCommand(job.getKey()).send().join();

      engine.waitForIdleState();

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(instanceEvent).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.occurredOnElement(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Error type was not raised on element '%s' but was raised on '%s' instead",
              WRONG_VALUE, ProcessPackLoopingServiceTask.GATEWAY_ELEMENT_ID);
    }

    @Test
    void testOccurredDuringJobFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, job.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(job).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.occurredDuringJob(-1))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Incident was not raised during job instance -1 but was raised in %d instead",
              job.getKey());
    }

    @Test
    void testIsResolvedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      // then
      assertThatThrownBy(incidentAssert::isResolved)
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith("Incident is not resolved");
    }

    @Test
    void testIsUnresolvedFailure() {
      // given
      Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(actual).extractLatestIncident();

      final long incidentKey = incidentAssert.getIncidentKey();
      client.newResolveIncidentCommand(incidentKey).send().join();

      engine.waitForIdleState();

      // then
      assertThatThrownBy(incidentAssert::isUnresolved)
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith("Incident is already resolved");
    }
  }
}
