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
package io.camunda.zeebe.process.test.qa.regular.multithread;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import io.camunda.zeebe.process.test.qa.util.Utilities;
import io.camunda.zeebe.process.test.qa.util.Utilities.ProcessPackLoopingServiceTask;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
public class WorkerTest {

  private ZeebeClient client;
  private ZeebeTestEngine engine;

  @Test
  void testJobsCanBeProcessedAsynchronouslyByWorker()
      throws InterruptedException, TimeoutException {
    // given
    client
        .newWorker()
        .jobType(ProcessPackLoopingServiceTask.JOB_TYPE)
        .handler(
            (client, job) -> {
              client.newCompleteCommand(job.getKey()).send();
            })
        .open();

    Utilities.deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
    final Map<String, Object> variables =
        Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 3);

    // when
    final ProcessInstanceEvent instanceEvent =
        Utilities.startProcessInstance(
            engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

    // then
    BpmnAssert.assertThat(instanceEvent).isStarted();
    // TODO: Idle state monitor does not work in this case.
    //  Might be fixed when switching to the zeebe built-in idle state monitor
    Thread.sleep(1000);
    BpmnAssert.assertThat(instanceEvent)
        .hasPassedElement(ProcessPackLoopingServiceTask.ELEMENT_ID, 3)
        .isCompleted();
  }
}
