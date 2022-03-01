package io.camunda.zeebe.process.test.api;

import io.camunda.zeebe.client.ZeebeClient;
import io.netty.util.Timeout;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/** The engine used for running processes. This engine runs fully in memory. */
public interface InMemoryEngine {

  /** Starts the engine */
  void start();

  /** Stops the engine */
  void stop();

  /** @return the {@link RecordStreamSource} of this engine */
  RecordStreamSource getRecordStreamSource();

  /** @return a newly created {@link ZeebeClient} */
  ZeebeClient createClient();

  /** @return the address at which the gateway is reachable */
  String getGatewayAddress();

  /**
   * Increases the time of the engine. Increasing the time can be useful for triggering timers with
   * a date in the future.
   *
   * @param timeToAdd the amount of time to increase the engine with
   */
  void increaseTime(Duration timeToAdd);

  /** Waits for the engine to reach an idle state.
   *
   * @param timeout the maximum amount of time to wait before idle state has been reached
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @throws TimeoutException if the engine has not reached an idle state before the timeout
   */
  void waitForIdleState(Duration timeout) throws InterruptedException, TimeoutException;

  /**
   * Waits for the engine to reach a busy state.
   *
   * @param timeout the maximum amount of time to wait before busy state has been reached
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @throws TimeoutException if the engine has not reached a busy state before the timeout
   */
  void waitForBusyState(Duration timeout) throws InterruptedException, TimeoutException;
}
