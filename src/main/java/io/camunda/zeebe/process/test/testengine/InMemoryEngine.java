package io.camunda.zeebe.process.test.testengine;

import io.camunda.zeebe.client.ZeebeClient;
import java.time.Duration;

/** The engine used for running processes. This engine runs fully in memory. */
public interface InMemoryEngine {

  /** Starts the engine */
  void start();

  /** Stops the engine */
  void stop();

  /** @return the {@link RecordStreamSource} of this engine */
  RecordStreamSource getRecordStream();

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

  /**
   * Runs the given function once the engine has reached an idle state.
   *
   * <p>For more info on the idle state refer to {@link IdleStateMonitor}
   *
   * @param callback the function that should be executed once an idle state has been reached
   */
  void runOnIdleState(Runnable callback);

  /**
   * Waits for the engine to reach an idle state.
   *
   * <p>For more info on the idle state refer to {@link IdleStateMonitor}
   */
  void waitForIdleState();
}
