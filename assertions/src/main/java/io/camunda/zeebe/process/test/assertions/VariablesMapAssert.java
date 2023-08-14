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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.impl.ZeebeObjectMapper;
import io.camunda.zeebe.process.test.api.ObjectMapperConfig;
import java.util.Map;
import org.assertj.core.api.AbstractAssert;

/**
 * Assertion for variable maps {@code Map<String, String>} which contain JSON strings as values
 * (e.g. variables in process instances, messages, job activations)
 */
public class VariablesMapAssert extends AbstractAssert<VariablesMapAssert, Map<String, String>> {

  private static ZeebeObjectMapper OBJECT_MAPPER = new ZeebeObjectMapper();

  static {
    final ObjectMapper customMapper = ObjectMapperConfig.getObjectMapper();
    if (customMapper != null) {
      OBJECT_MAPPER = new ZeebeObjectMapper(customMapper);
    }
  }

  public VariablesMapAssert(final Map<String, String> actual) {
    super(actual, VariablesMapAssert.class);
  }

  /**
   * Assert that the given variable name is a key in the given map of variables. *
   *
   * @param name The name of the variable
   * @return this ${@link ProcessInstanceAssert}
   */
  public VariablesMapAssert containsVariable(final String name) {
    assertThat(actual)
        .withFailMessage(
            "Unable to find variable with name `%s`. Available variables are: %s",
            name, actual.keySet())
        .containsKey(name);
    return this;
  }

  public VariablesMapAssert hasVariableWithValue(final String name, final Object value) {
    containsVariable(name);

    final String expectedValue = OBJECT_MAPPER.toJson(value);
    final String actualValue = actual.get(name);

    assertThat(isEqual(actualValue, expectedValue))
        .withFailMessage(
            "The variable '%s' does not have the expected value. The value passed in"
                + " ('%s') is internally mapped to a JSON String that yields '%s'. However, the "
                + "actual value (as JSON String) is '%s'.",
            name, value, expectedValue, actualValue)
        .isTrue();

    return this;
  }

  private static boolean isEqual(final String actualJson, final String expectedJson) {
    return asJsonNode(actualJson).equals(asJsonNode(expectedJson));
  }

  private static JsonNode asJsonNode(final String json) {
    return OBJECT_MAPPER.fromJson(json, JsonNode.class);
  }
}
