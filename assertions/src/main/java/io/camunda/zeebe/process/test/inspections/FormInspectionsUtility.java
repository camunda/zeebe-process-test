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

package io.camunda.zeebe.process.test.inspections;

import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.deployment.FormMetadataValue;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class FormInspectionsUtility {

  /**
   * Finds if a form is created.
   *
   * @param formId the id of the form
   * @return true if the id of the form is found in the stream, false otherwise
   */
  public static boolean isFormCreated(final String formId) {
    return getForms().anyMatch(form -> formId.equals(form.getFormId()));
  }

  /**
   * Find the latest version of a form
   *
   * @param formId the id of the form
   * @return the {@link Optional} latest version of the form if available
   */
  public static Optional<FormMetadataValue> findLatestFormById(final String formId) {
    return getForms()
        .filter(form -> formId.equals(form.getFormId()))
        .max(Comparator.comparingInt(FormMetadataValue::getVersion));
  }

  private static Stream<FormMetadataValue> getForms() {
    return StreamFilter.forms(BpmnAssert.getRecordStream()).stream().map(Record::getValue);
  }
}
