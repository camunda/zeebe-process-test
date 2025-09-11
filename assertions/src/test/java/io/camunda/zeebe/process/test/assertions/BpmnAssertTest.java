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
package io.camunda.zeebe.process.test.assertions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BpmnAssertTest {

  @BeforeEach
  void beforeEach() {
    BpmnAssert.initRecordStream(mock(RecordStream.class));
  }

  @AfterEach
  void afterEach() {
    BpmnAssert.resetRecordStream();
  }

  @Test
  @DisplayName("Should return ProcessInstanceAssert for ProcessInstanceEvent")
  void testAssertThatProcessInstanceEventReturnsProcessInstanceAssert() {
    // given
    ProcessInstanceEvent event = mock(ProcessInstanceEvent.class);

    // when
    ProcessInstanceAssert assertions = BpmnAssert.assertThat(event);

    // then
    assertThat(assertions).isInstanceOf(ProcessInstanceAssert.class);
  }

  @Test
  @DisplayName("Should return ProcessInstanceAssert for ProcessInstanceResult")
  void testAssertThatProcessInstanceResultReturnsProcessInstanceAssert() {
    // given
    ProcessInstanceResult result = mock(ProcessInstanceResult.class);

    // when
    ProcessInstanceAssert assertions = BpmnAssert.assertThat(result);

    // then
    assertThat(assertions).isInstanceOf(ProcessInstanceAssert.class);
  }

  @Test
  @DisplayName("Should return ProcessInstanceAssert for InspectedProcessInstance")
  void testAssertThatInspectedProcessInstanceReturnsProcessInstanceAssert() {
    // given
    InspectedProcessInstance inspected = mock(InspectedProcessInstance.class);

    // when
    ProcessInstanceAssert assertions = BpmnAssert.assertThat(inspected);

    // then
    assertThat(assertions).isInstanceOf(ProcessInstanceAssert.class);
  }

  @Test
  @DisplayName("Should return JobAssert for ActivatedJob")
  void testAssertThatActivatedJobReturnsJobAssert() {
    // given
    ActivatedJob job = mock(ActivatedJob.class);

    // when
    JobAssert assertions = BpmnAssert.assertThat(job);

    // then
    assertThat(assertions).isInstanceOf(JobAssert.class);
  }

  @Test
  @DisplayName("Should return DeploymentAssert for DeploymentEvent")
  void testAssertThatDeploymentEventReturnsDeploymentAssert() {
    // given
    DeploymentEvent event = mock(DeploymentEvent.class);

    // when
    DeploymentAssert assertions = BpmnAssert.assertThat(event);

    // then
    assertThat(assertions).isInstanceOf(DeploymentAssert.class);
  }

  @Test
  @DisplayName("Should return MessageAssert for PublishMessageResponse")
  void testAssertThatPublishMessageResponseReturnsMessageAssert() {
    // given
    PublishMessageResponse event = mock(PublishMessageResponse.class);

    // when
    MessageAssert assertions = BpmnAssert.assertThat(event);

    // then
    assertThat(assertions).isInstanceOf(MessageAssert.class);
  }
}
