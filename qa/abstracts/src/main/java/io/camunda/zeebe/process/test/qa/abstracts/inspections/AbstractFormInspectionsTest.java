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

package io.camunda.zeebe.process.test.qa.abstracts.inspections;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.inspections.FormInspectionsUtility;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.FormPack;
import io.camunda.zeebe.protocol.record.value.deployment.FormMetadataValue;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public abstract class AbstractFormInspectionsTest {

  private ZeebeClient client;
  private ZeebeTestEngine engine;

  @Test
  void shouldReturnTrueIfFormIsCreated() {
    // given
    Utilities.deployResource(client, FormPack.RESOURCE_NAME);

    // when
    final boolean isCreated = FormInspectionsUtility.isFormCreated(FormPack.FORM_ID);

    // then
    assertThat(isCreated).isTrue();
  }

  @Test
  void shouldReturnFalseIfFormIsNotCreated() {
    // when
    final boolean isCreated = FormInspectionsUtility.isFormCreated("wrongFormId");

    // then
    assertThat(isCreated).isFalse();
  }

  @Test
  void shouldFindTheLatestVersion() {
    // given
    Utilities.deployResource(client, FormPack.RESOURCE_NAME);
    Utilities.deployResources(client, FormPack.FORM_V2_RESOURCE_NAME);

    // when
    final Optional<FormMetadataValue> latestForm =
        FormInspectionsUtility.findLatestFormById(FormPack.FORM_ID);

    // then
    assertThat(latestForm.isPresent()).isTrue();

    final FormMetadataValue form = latestForm.get();
    assertThat(form.getFormId()).isEqualTo(FormPack.FORM_ID);
    assertThat(form.getVersion()).isEqualTo(2);
  }
}
