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

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.assertions.FormAssert;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.FormPack;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AbstractFormAssertTest {

  public static final String WRONG_VALUE = "wrong value";

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private ZeebeClient client;
    private ZeebeTestEngine engine;

    @Test
    void shouldHaveFormId() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployResource(client, FormPack.RESOURCE_NAME);

      // when
      final FormAssert formAssert =
          BpmnAssert.assertThat(deploymentEvent).extractingFormByFormId(FormPack.FORM_ID);

      // then
      formAssert.hasFormId(FormPack.FORM_ID);
    }

    @Test
    void shouldHaveVersion() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployResource(client, FormPack.RESOURCE_NAME);

      // when
      final FormAssert formAssert =
          BpmnAssert.assertThat(deploymentEvent).extractingFormByFormId(FormPack.FORM_ID);

      // then
      formAssert.hasVersion(1);
    }

    @Test
    void shouldHaveResourceName() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployResource(client, FormPack.RESOURCE_NAME);

      // when
      final FormAssert formAssert =
          BpmnAssert.assertThat(deploymentEvent).extractingFormByFormId(FormPack.FORM_ID);

      // then
      formAssert.hasResourceName(FormPack.RESOURCE_NAME);
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private ZeebeClient client;
    private ZeebeTestEngine engine;

    @Test
    void shouldHaveFormIdFailure() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployResource(client, FormPack.RESOURCE_NAME);

      // when
      final FormAssert formAssert =
          BpmnAssert.assertThat(deploymentEvent).extractingFormByFormId(FormPack.FORM_ID);

      // then
      assertThatThrownBy(() -> formAssert.hasFormId(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected form id to be '%s' but was '%s' instead.", WRONG_VALUE, FormPack.FORM_ID);
    }

    @Test
    void shouldHaveVersionFailure() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployResource(client, FormPack.RESOURCE_NAME);

      // when
      final FormAssert formAssert =
          BpmnAssert.assertThat(deploymentEvent).extractingFormByFormId(FormPack.FORM_ID);

      // then
      assertThatThrownBy(() -> formAssert.hasVersion(2))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected version to be 2 but was 1 instead");
    }

    @Test
    void shouldHaveResourceName() {
      // given
      final DeploymentEvent deploymentEvent =
          Utilities.deployResource(client, FormPack.RESOURCE_NAME);

      // when
      final FormAssert formAssert =
          BpmnAssert.assertThat(deploymentEvent)
              .extractingFormByResourceName(FormPack.RESOURCE_NAME);

      // then
      assertThatThrownBy(() -> formAssert.hasResourceName(WRONG_VALUE))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected resource name to be '%s' but was '%s' instead.",
              WRONG_VALUE, FormPack.RESOURCE_NAME);
    }
  }
}
