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

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VariablesMapAssertTest {

  private static final String KEY = "key";
  private static final String UNKNOWN_KEY = "unknownKey";

  private static final Map<String, String> VARIABLES = new HashMap<>();

  static {
    VARIABLES.put(KEY, "\"value\"");
  }

  @Nested
  class ContainsVariable {

    @Test
    void shouldNotThrowAssertionErrorWhenVariableIsKnown() {
      // given
      final VariablesMapAssert sut = new VariablesMapAssert(VARIABLES);

      // when + then
      assertThatNoException().isThrownBy(() -> sut.containsVariable(KEY));
    }

    @Test
    void shouldThrowAssertionErrorWhenVariableIsUnknown() {
      // given
      final VariablesMapAssert sut = new VariablesMapAssert(VARIABLES);

      // when + then
      assertThatThrownBy(() -> sut.containsVariable(UNKNOWN_KEY))
          .isExactlyInstanceOf(AssertionError.class)
          .hasMessage(
              "Unable to find variable with name `%s`. Available variables are: %s",
              UNKNOWN_KEY, VARIABLES.keySet());
    }
  }

  @Nested
  class HasVariableWithValue {

    @Test
    void shouldNotThrowAssertionErrorWhenVariableIsKnownAndValueIsCorrect() {
      // given
      final VariablesMapAssert sut = new VariablesMapAssert(VARIABLES);

      // when + then
      assertThatNoException().isThrownBy(() -> sut.hasVariableWithValue(KEY, "value"));
    }

    @Test
    void shouldThrowAssertionErrorWhenVariableIsUnknown() {
      // given
      final VariablesMapAssert sut = new VariablesMapAssert(VARIABLES);

      // when + then
      assertThatThrownBy(() -> sut.hasVariableWithValue(UNKNOWN_KEY, null))
          .isExactlyInstanceOf(AssertionError.class)
          .hasMessage(
              "Unable to find variable with name `%s`. Available variables are: %s",
              UNKNOWN_KEY, VARIABLES.keySet());
    }

    @Test
    void shouldThrowAssertionErrorWhenValueIsDifferent() {
      // given
      final VariablesMapAssert sut = new VariablesMapAssert(VARIABLES);

      // when + then
      assertThatThrownBy(() -> sut.hasVariableWithValue(KEY, "someOtherValue"))
          .isExactlyInstanceOf(AssertionError.class)
          .hasMessage(
              "The variable 'key' does not have the expected value. The value passed in ('someOtherValue') is internally mapped to a JSON String that yields '\"someOtherValue\"'. However, the actual value (as JSON String) is '\"value\"'.");
    }

    /**
     * Verifies that the logic to compare the actual and expected value is lenient with respect to
     * the order of fields in a JSON object
     */
    @Test
    void shouldBeLenientWithRespectToFieldOrder() {
      // given
      final Map<String, Boolean> value = new HashMap<>();
      value.put("a", true);
      value.put("b", false);

      final String jsonRepresentation1 = "{ \"a\" : true, \"b\": false }";
      final String jsonRepresentation2 = "{ \"b\" : false, \"a\": true }";

      final Map<String, String> map1 = new HashMap<>();
      map1.put(KEY, jsonRepresentation1);

      final Map<String, String> map2 = new HashMap<>();
      map2.put(KEY, jsonRepresentation2);

      final VariablesMapAssert sut1 = new VariablesMapAssert(map1);
      final VariablesMapAssert sut2 = new VariablesMapAssert(map2);

      // when + then
      assertThatNoException().isThrownBy(() -> sut1.hasVariableWithValue(KEY, value));
      assertThatNoException().isThrownBy(() -> sut2.hasVariableWithValue(KEY, value));
    }
  }
}
