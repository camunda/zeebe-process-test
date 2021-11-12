package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.BpmnAssert.assertThat;
import static io.camunda.testing.util.Utilities.deployProcess;
import static io.camunda.testing.util.Utilities.startProcessInstance;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.testing.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import java.util.Collections;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.StringAssert;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeAssertions
class IncidentAssertTest {

  public static final String WRONG_VALUE = "wrong value";
  public static final String ERROR_CODE = "error";
  public static final String ERROR_MESSAGE = "error occurred";

  private ZeebeClient client;
  private ZeebeEngine engine;

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    void testHasErrorType() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      // then
      incidentAssert.hasErrorType(ErrorType.UNHANDLED_ERROR_EVENT);
    }

    @Test
    void testHasErrorMessage() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      // then
      incidentAssert.hasErrorMessage(
          "Expected to throw an error event with the code 'error' with message 'error occurred', but it was not caught. No error events are available in the scope.");
    }

    @Test
    void testExtractErrorMessage() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      final StringAssert messageAssert = incidentAssert.extractErrorMessage();

      // then
      Assertions.assertThat(messageAssert).isNotNull();
      messageAssert.isEqualTo(
          "Expected to throw an error event with the code 'error' with message 'error occurred', but it was not caught. No error events are available in the scope.");
    }

    @Test
    void testWasRaisedInProcessInstance() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent processInstanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      // then
      incidentAssert.wasRaisedInProcessInstance(processInstanceEvent);
      incidentAssert.wasRaisedInProcessInstance(processInstanceEvent.getProcessInstanceKey());
    }

    @Test
    void testOccurredOnElement() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackLoopingServiceTask.TOTAL_LOOPS, "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because VAR_TOTAL_LOOPS is a string, but needs to be an int */
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();
      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      client.newCompleteCommand(job.getKey()).send().join();

      // TODO remove this
      Thread.sleep(100);

      final IncidentAssert incidentAssert = assertThat(instanceEvent).extractLatestIncident();

      // then
      incidentAssert.occurredOnElement(ProcessPackLoopingServiceTask.GATEWAY_ELEMENT_ID);
    }

    @Test
    void testOccurredDuringJob() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(job.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(job).extractLatestIncident();

      // then
      incidentAssert.occurredDuringJob(job);
      incidentAssert.occurredDuringJob(job.getKey());
    }

    @Test
    void testIsResolved() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      final long incidentKey = incidentAssert.getIncidentKey();
      client.newResolveIncidentCommand(incidentKey).send().join();

      // TODO remove
      Thread.sleep(100);

      // then
      incidentAssert.isResolved();
    }

    @Test
    void testIsUnresolved() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      // then
      incidentAssert.isUnresolved();
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
    void testHasErrorTypeFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.hasErrorType(ErrorType.IO_MAPPING_ERROR))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Error type was not 'IO_MAPPING_ERROR' but was 'UNHANDLED_ERROR_EVENT' instead");
    }

    @Test
    void testHasErrorMessageFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.hasErrorMessage(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Error message was not 'wrong value' but was 'Expected to throw an error event with the code 'error' with message 'error occurred', but it was not caught. No error events are available in the scope.' instead");
    }

    @Test
    void testWasRaisedInProcessInstanceFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent processInstanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.wasRaisedInProcessInstance(-1))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Incident was not raised in process instance -1 but was raised in %d instead",
              processInstanceEvent.getProcessInstanceKey());
    }

    @Test
    void testOccurredOnElementFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      final Map<String, Object> variables =
          Collections.singletonMap(
              ProcessPackLoopingServiceTask.TOTAL_LOOPS, "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because VAR_TOTAL_LOOPS is a string, but needs to be an int */
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();
      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      client.newCompleteCommand(job.getKey()).send().join();

      // TODO remove this
      Thread.sleep(100);

      final IncidentAssert incidentAssert = assertThat(instanceEvent).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.occurredOnElement(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Error type was not raised on element '%s' but was raised on '%s' instead",
              WRONG_VALUE, ProcessPackLoopingServiceTask.GATEWAY_ELEMENT_ID);
    }

    @Test
    void testOccurredDuringJobFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(job.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(job).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.occurredDuringJob(-1))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Incident was not raised during job instance -1 but was raised in %d instead",
              job.getKey());
    }

    @Test
    void testIsResolvedFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.isResolved())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Incident is not resolved");
    }

    @Test
    void testIsUnresolvedFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse = activateSingleJob();

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      client
          .newThrowErrorCommand(actual.getKey())
          .errorCode(ERROR_CODE)
          .errorMessage(ERROR_MESSAGE)
          .send()
          .join();

      final IncidentAssert incidentAssert = assertThat(actual).extractLatestIncident();

      final long incidentKey = incidentAssert.getIncidentKey();
      client.newResolveIncidentCommand(incidentKey).send().join();

      // TODO remove
      Thread.sleep(100);

      // then
      assertThatThrownBy(() -> incidentAssert.isUnresolved())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Incident is already resolved");
    }
  }
}
