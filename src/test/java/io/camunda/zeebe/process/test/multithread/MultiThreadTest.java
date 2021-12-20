package io.camunda.zeebe.process.test.multithread;

import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.process.test.util.Utilities.deployProcess;
import static io.camunda.zeebe.process.test.util.Utilities.startProcessInstance;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.RecordStreamSourceStore;
import io.camunda.zeebe.process.test.extensions.ZeebeProcessTest;
import io.camunda.zeebe.process.test.testengine.InMemoryEngine;
import io.camunda.zeebe.process.test.testengine.RecordStreamSource;
import io.camunda.zeebe.process.test.util.Utilities.ProcessPackStartEndEvent;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
public class MultiThreadTest {

  private InMemoryEngine engine;
  private ZeebeClient client;
  private RecordStreamSource recordStreamSource;

  @Test
  public void testMultiThreadingThrowsNoExceptions() throws InterruptedException {
    final ExecutorService executorService = Executors.newFixedThreadPool(5);

    final List<Future<Boolean>> futures =
        executorService.invokeAll(
            List.of(
                new ProcessRunner(),
                new ProcessRunner(),
                new ProcessRunner(),
                new ProcessRunner(),
                new ProcessRunner()));

    for (final Future<Boolean> future : futures) {
      try {
        Assertions.assertThat(future.get()).isTrue();
      } catch (ExecutionException ex) {
        Assertions.fail("Future completed exceptionally: %s", ExceptionUtils.getStackTrace(ex));
      }
    }
  }

  private class ProcessRunner implements Callable<Boolean> {

    @Override
    public Boolean call() {
      RecordStreamSourceStore.init(recordStreamSource);

      deployProcess(client, ProcessPackStartEndEvent.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(engine, client, ProcessPackStartEndEvent.PROCESS_ID);
      engine.waitForIdleState();

      assertThat(instanceEvent).isCompleted();
      RecordStreamSourceStore.reset();
      return true;
    }
  }
}
