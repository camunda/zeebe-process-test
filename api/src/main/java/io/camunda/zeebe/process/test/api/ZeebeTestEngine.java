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
package io.camunda.zeebe.process.test.api;

import io.camunda.zeebe.client.ZeebeClient;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * The engine used to execute processes. This engine is a stripped down version of the actual Zeebe
 * Engine. It's intended for testing purposes only.
 */
public interface ZeebeTestEngine {

  /** Starts the test engine */
  void start();

  /** Stops the test engine */
  void stop();

  /**
   * @return the {@link RecordStreamSource} of this test engine
   */
  RecordStreamSource getRecordStreamSource();

  /**
   * @return a newly created {@link ZeebeClient}
   */
  ZeebeClient createClient();

  /**
   * @return the address at which the gateway is reachable
   */
  String getGatewayAddress();

  /**
   * Increases the time of the test engine. Increasing the time can be useful for triggering timers
   * with a date in the future.
   *
   * @param timeToAdd the amount of time to increase the engine with
   */
  void increaseTime(Duration timeToAdd);

  /**
   * The engine is in an idle state when there is nothing left to do for it. In this state, the
   * engine is waiting for an external command sent by a client, or for an event to trigger. For
   * example, when a timer expires the engine may need to continue orchestrating some process
   * instance(s).</br>
   *
   * <p>On a technical level, idle state is defined by
   *
   * <ul>
   *   <li>The point in time when all current records in the commit log have been processed by the
   *       engine
   *   <li>This is insufficient, however, because the engine might still be in the process of
   *       writing follow-up records
   *   <li>Therefore, when the first idle state is detected, a grace period (30ms) starts. If no new
   *       records come in during that grace period, then at the end of the grace period callbacks
   *       are notified
   * </ul>
   *
   * @param timeout the maximum amount of time to wait before idle state has been reached
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @throws TimeoutException if the engine has not reached an idle state before the timeout
   */
  void waitForIdleState(Duration timeout) throws InterruptedException, TimeoutException;

  /**
   * Waits for the engine to reach a busy state. We consider the engine to be in a busy state when
   * new records are being processed since we started waiting.
   *
   * @param timeout the maximum amount of time to wait before busy state has been reached
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @throws TimeoutException if the engine has not reached a busy state before the timeout
   */
  void waitForBusyState(Duration timeout) throws InterruptedException, TimeoutException;

  /**
   * Gets the current engine time. Can be used in conjunction with increaseTime to set a specific
   * time in the future.
   *
   * @return the current time of the engine in milliseconds since the epoch.
   */
  long getTime();
}
