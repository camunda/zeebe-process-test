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
package io.camunda.zeebe.process.test.qa.embedded;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExtendAbstractsTest {

  public static final Class<ZeebeProcessTest> ANNOTATION = ZeebeProcessTest.class;
  private static final String ABSTRACT_PACKAGE = "io.camunda.zeebe.process.test.qa.abstracts";
  private static final String TEST_PACKAGE = "io.camunda.zeebe.process.test.qa.embedded";
  private static final ClassInfoList CLASSES =
      new ClassGraph()
          .acceptPackages(TEST_PACKAGE)
          .ignoreClassVisibility()
          .enableAnnotationInfo()
          .scan()
          .getAllStandardClasses()
          .filter(info -> !info.isInnerClass());

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideAbstractClasses")
  void testAbstractClassIsExtendedWithEmbeddedExtension(
      final String className, final Class<?> abstractClass) {
    final ClassInfoList embeddedClass =
        CLASSES
            .filter(info -> info.getPackageName().contains("embedded"))
            .filter(info -> info.extendsSuperclass(abstractClass))
            .filter(info -> info.hasAnnotation(ANNOTATION));

    assertThat(embeddedClass)
        .withFailMessage(
            "Expected 1 embedded implementation of %s, but found %d: %s",
            className, embeddedClass.size(), embeddedClass)
        .hasSize(1);
  }

  private static Stream<Arguments> provideAbstractClasses() {
    return new ClassGraph()
            .acceptPackages(ABSTRACT_PACKAGE)
            .scan()
            .getAllStandardClasses()
            .filter(ClassInfo::isAbstract)
            .filter(info -> !info.isInnerClass())
            .stream()
            .map(info -> Arguments.of(info.getSimpleName(), info.loadClass()));
  }
}
