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
package io.camunda.zeebe.process.test.qa.abstracts.multithread;

import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.deployResource;
import static io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.startProcessInstance;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackStartEndEvent;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractMultiThreadTest {

  private ExecutorService executorService;

  @BeforeEach
  void beforeEach() {
    executorService = Executors.newFixedThreadPool(5);
  }

  @AfterEach
  void afterEach() {
    executorService.shutdown();
  }

  @Test
  void testMultiThreadingThrowsNoExceptions() throws InterruptedException {
    final List<Future<Boolean>> futures =
        executorService.invokeAll(
            Arrays.asList(
                new ProcessRunner(),
                new ProcessRunner(),
                new ProcessRunner(),
                new ProcessRunner(),
                new ProcessRunner()));

    for (final Future<Boolean> future : futures) {
      try {
        Assertions.assertThat(future.get()).isTrue();
      } catch (final ExecutionException ex) {
        Assertions.fail("Future completed exceptionally: %s", ExceptionUtils.getStackTrace(ex));
      }
    }
  }

  public abstract ZeebeClient getClient();

  public abstract ZeebeTestEngine getEngine();

  public abstract RecordStream getRecordStream();

  private class ProcessRunner implements Callable<Boolean> {

    @Override
    public Boolean call() throws InterruptedException, TimeoutException {
      BpmnAssert.initRecordStream(getRecordStream());

      deployResource(getClient(), ProcessPackStartEndEvent.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(getEngine(), getClient(), ProcessPackStartEndEvent.PROCESS_ID);
      Utilities.waitForIdleState(getEngine(), Duration.ofSeconds(1));

      assertThat(instanceEvent).isCompleted();
      BpmnAssert.resetRecordStream();
      return true;
    }
  }
}
