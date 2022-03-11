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
 *
 */

package io.camunda.zeebe.process.test.qa;

import com.google.common.reflect.ClassPath;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class ExtendAbstractsTest {

  public static final String REGULAR = "regular";
  public static final String TESTCONTAINER = "testcontainer";
  public static final Class<ZeebeProcessTest> REGULAR_ANNOTATION = ZeebeProcessTest.class;
  public static final Class<io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest>
      TESTCONTAINER_ANNOTATION =
          io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest.class;
  private static final String BASE_PACKAGE = "io.camunda.zeebe.process.test.qa";
  private static final String ABSTRACTS = "abstracts";

  @Test
  void testAbstractClassesAreExtendedWithBothExtensions() throws IOException {
    final Map<String, List<Class<?>>> classes = new HashMap<>();
    classes.put(ABSTRACTS, new ArrayList<>());
    classes.put(REGULAR, new ArrayList<>());
    classes.put(TESTCONTAINER, new ArrayList<>());

    ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClasses().stream()
        .filter(clazz -> clazz.getPackageName().startsWith(BASE_PACKAGE))
        .forEach(
            classInfo -> {
              if (classInfo.getPackageName().contains(BASE_PACKAGE + "." + ABSTRACTS)) {
                classes.get(ABSTRACTS).add(classInfo.load());
              } else if (classInfo.getPackageName().contains(BASE_PACKAGE + "." + REGULAR)) {
                classes.get(REGULAR).add(classInfo.load());
              } else if (classInfo.getPackageName().contains(BASE_PACKAGE + "." + TESTCONTAINER)) {
                classes.get(TESTCONTAINER).add(classInfo.load());
              }
            });

    final SoftAssertions softly = new SoftAssertions();
    classes
        .get(ABSTRACTS)
        .forEach(
            abstractClass -> {
              assertExtendingClass(
                  abstractClass, classes.get(REGULAR), REGULAR, REGULAR_ANNOTATION, softly);
              assertExtendingClass(
                  abstractClass,
                  classes.get(TESTCONTAINER),
                  TESTCONTAINER,
                  TESTCONTAINER_ANNOTATION,
                  softly);
            });
    softly.assertAll();
  }

  private void assertExtendingClass(
      final Class<?> abstractClass,
      final List<Class<?>> classes,
      final String classPackage,
      final Class<? extends Annotation> annotation,
      final SoftAssertions softly) {
    final Optional<Class<?>> clazzOptional =
        classes.stream().filter(abstractClass::isAssignableFrom).findFirst();
    softly
        .assertThat(clazzOptional)
        .withFailMessage(
            "Package %s.%s does not contain a class extending %s",
            BASE_PACKAGE, classPackage, abstractClass)
        .isNotEmpty();
    clazzOptional.ifPresent(clazz -> softly.assertThat(clazz).hasAnnotation(annotation));
  }
}
