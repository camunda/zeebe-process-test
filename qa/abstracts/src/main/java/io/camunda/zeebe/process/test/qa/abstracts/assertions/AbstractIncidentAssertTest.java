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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivateJobsResponse;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.IncidentAssert;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.StringAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public abstract class AbstractIncidentAssertTest {

  public static final String WRONG_VALUE = "wrong value";
  public static final String ERROR_CODE = "error";
  public static final String ERROR_MESSAGE = "error occurred";

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private CamundaClient client;
    private ZeebeTestEngine engine;

    @Test
    void testHasErrorType() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      // then
      incidentAssert.hasErrorType(ErrorType.UNHANDLED_ERROR_EVENT);
    }

    @Test
    void testHasErrorMessage() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      // then
      incidentAssert.hasErrorMessage(
          "Expected to throw an error event with the code 'error' with message 'error occurred', but it was not caught. No error events are available in the scope.");
    }

    @Test
    void testExtractErrorMessage() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      final StringAssert messageAssert = incidentAssert.extractingErrorMessage();

      // then
      Assertions.assertThat(messageAssert).isNotNull();
      messageAssert.isEqualTo(
          "Expected to throw an error event with the code 'error' with message 'error occurred', but it was not caught. No error events are available in the scope.");
    }

    @Test
    void testWasRaisedInProcessInstance() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent processInstanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      // then
      incidentAssert.wasRaisedInProcessInstance(processInstanceEvent);
      incidentAssert.wasRaisedInProcessInstance(processInstanceEvent.getProcessInstanceKey());
    }

    @Test
    void testOccurredOnElement() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

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

      Utilities.waitForIdleState(engine, Duration.ofSeconds(1));

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(instanceEvent).extractingLatestIncident();

      // then
      incidentAssert.occurredOnElement(ProcessPackLoopingServiceTask.GATEWAY_ELEMENT_ID);
    }

    @Test
    void testOccurredDuringJob() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, job.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(job).extractingLatestIncident();

      // then
      incidentAssert.occurredDuringJob(job);
      incidentAssert.occurredDuringJob(job.getKey());
    }

    @Test
    void testIsResolved() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      final long incidentKey = incidentAssert.getIncidentKey();
      client.newResolveIncidentCommand(incidentKey).send().join();

      Utilities.waitForIdleState(engine, Duration.ofSeconds(1));

      // then
      incidentAssert.isResolved();
    }

    @Test
    void testIsUnresolved() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      // then
      incidentAssert.isUnresolved();
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private CamundaClient client;
    private ZeebeTestEngine engine;

    @Test
    void testHasErrorTypeFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.hasErrorType(ErrorType.IO_MAPPING_ERROR))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Error type was not 'IO_MAPPING_ERROR' but was 'UNHANDLED_ERROR_EVENT' instead");
    }

    @Test
    void testHasErrorMessageFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.hasErrorMessage(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Error message was not 'wrong value' but was 'Expected to throw an error event with the code 'error' with message 'error occurred', but it was not caught. No error events are available in the scope.' instead");
    }

    @Test
    void testWasRaisedInProcessInstanceFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent processInstanceEvent =
          Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.wasRaisedInProcessInstance(-1))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Incident was not raised in process instance -1 but was raised in %d instead",
              processInstanceEvent.getProcessInstanceKey());
    }

    @Test
    void testOccurredOnElementFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

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

      Utilities.waitForIdleState(engine, Duration.ofSeconds(1));

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(instanceEvent).extractingLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.occurredOnElement(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Error type was not raised on element '%s' but was raised on '%s' instead",
              WRONG_VALUE, ProcessPackLoopingServiceTask.GATEWAY_ELEMENT_ID);
    }

    @Test
    void testOccurredDuringJobFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob job = jobActivationResponse.getJobs().get(0);
      Utilities.throwErrorCommand(engine, client, job.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert = BpmnAssert.assertThat(job).extractingLatestIncident();

      // then
      assertThatThrownBy(() -> incidentAssert.occurredDuringJob(-1))
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith(
              "Incident was not raised during job instance -1 but was raised in %d instead",
              job.getKey());
    }

    @Test
    void testIsResolvedFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      // then
      assertThatThrownBy(incidentAssert::isResolved)
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith("Incident is not resolved");
    }

    @Test
    void testIsUnresolvedFailure() throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      Utilities.startProcessInstance(engine, client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // when
      final ActivateJobsResponse jobActivationResponse =
          Utilities.activateSingleJob(client, ProcessPackLoopingServiceTask.JOB_TYPE);

      final ActivatedJob actual = jobActivationResponse.getJobs().get(0);

      Utilities.throwErrorCommand(engine, client, actual.getKey(), ERROR_CODE, ERROR_MESSAGE);

      final IncidentAssert incidentAssert =
          BpmnAssert.assertThat(actual).extractingLatestIncident();

      final long incidentKey = incidentAssert.getIncidentKey();
      client.newResolveIncidentCommand(incidentKey).send().join();

      Utilities.waitForIdleState(engine, Duration.ofSeconds(1));

      // then
      assertThatThrownBy(incidentAssert::isUnresolved)
          .isInstanceOf(AssertionError.class)
          .hasMessageStartingWith("Incident is already resolved");
    }
  }
}
