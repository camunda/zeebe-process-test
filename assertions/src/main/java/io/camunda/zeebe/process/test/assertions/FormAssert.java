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

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.api.response.Form;
import org.assertj.core.api.AbstractAssert;

/**
 * Assertions for {@link Form} instances.
 *
 * <p>These asserts can be obtained via:
 *
 * <pre>{@code
 * final DeploymentEvent deploymentEvent =
 *         client.newDeployCommand().addResourceFile(file).send().join();
 *
 * final FormAssert formAssert =
 *         assertThat(deploymentEvent)
 *             .extractingFormByFormId(FORM_ID);
 * }</pre>
 */
public class FormAssert extends AbstractAssert<FormAssert, Form> {

  protected FormAssert(final Form form) {
    super(form, FormAssert.class);
  }

  public FormAssert hasFormId(final String expectedFormId) {
    assertThat(expectedFormId).isNotEmpty();

    final String actualFormId = actual.getFormId();

    assertThat(actualFormId)
        .withFailMessage(
            "Expected form id to be '%s' but was '%s' instead.", expectedFormId, actualFormId)
        .isEqualTo(expectedFormId);

    return this;
  }

  public FormAssert hasFormKey(final long expectedFormKey) {
    assertThat(expectedFormKey).isPositive();

    final long actualFormKey = actual.getFormKey();

    assertThat(actualFormKey)
        .withFailMessage(
            "Expected form id to be '%d' but was '%d' instead.", expectedFormKey, actualFormKey)
        .isEqualTo(expectedFormKey);

    return this;
  }

  /**
   * Asserts that the form has the given version
   *
   * @param expectedVersion version to check
   * @return this {@link FormAssert}
   */
  public FormAssert hasVersion(final long expectedVersion) {
    final long actualVersion = actual.getVersion();

    assertThat(actualVersion)
        .withFailMessage(
            "Expected version to be %d but was %d instead", expectedVersion, actualVersion)
        .isEqualTo(expectedVersion);
    return this;
  }

  /**
   * Asserts that the form has the given resource name
   *
   * @param expectedResourceName resource name to check
   * @return this {@link FormAssert}
   */
  public FormAssert hasResourceName(final String expectedResourceName) {
    assertThat(expectedResourceName).isNotEmpty();

    final String actualResourceName = actual.getResourceName();

    assertThat(actualResourceName)
        .withFailMessage(
            "Expected resource name to be '%s' but was '%s' instead.",
            expectedResourceName, actualResourceName)
        .isEqualTo(expectedResourceName);

    return this;
  }
}
