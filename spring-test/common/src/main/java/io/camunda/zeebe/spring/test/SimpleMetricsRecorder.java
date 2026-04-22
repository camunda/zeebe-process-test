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

package io.camunda.zeebe.spring.test;

import io.camunda.client.metrics.MetricsRecorder;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/** Super simple class to record metrics in memory. Typically used for test cases */
public class SimpleMetricsRecorder implements MetricsRecorder {

  public static final String METRIC_NAME_JOB = "camunda.job.invocations";
  public static final String ACTION_COMPLETED = "completed";
  public static final String ACTION_FAILED = "failed";
  public static final String ACTION_ACTIVATED = "activated";
  public static final String ACTION_BPMN_ERROR = "bpmn-error";

  public HashMap<String, AtomicLong> counters = new HashMap<>();

  public HashMap<String, Long> timers = new HashMap<>();

  private void increase(
      final String metricName, final String action, final String type, final int count) {
    final String key = key(metricName, action, type);
    if (!counters.containsKey(key)) {
      counters.put(key, new AtomicLong(count));
    } else {
      counters.get(key).addAndGet(count);
    }
  }

  private String key(final String metricName, final String action, final String type) {
    final String key = metricName + "#" + action + "#" + type;
    return key;
  }

  public long getCount(final String metricName, final String action, final String type) {
    if (!counters.containsKey(key(metricName, action, type))) {
      return 0;
    }
    return counters.get(key(metricName, action, type)).get();
  }

  @Override
  public void increaseActivated(final CounterMetricsContext context) {
    increase(context.name(), "activated", context.tags().get("type"), context.count());
  }

  @Override
  public void increaseCompleted(final CounterMetricsContext context) {
    increase(context.name(), "completed", context.tags().get("type"), context.count());
  }

  @Override
  public void increaseFailed(final CounterMetricsContext context) {
    increase(context.name(), "failed", context.tags().get("type"), context.count());
  }

  @Override
  public void increaseBpmnError(final CounterMetricsContext context) {
    increase(context.name(), "bpmn-error", context.tags().get("type"), context.count());
  }

  @Override
  public <T> T executeWithTimer(
      final TimerMetricsContext context, final Callable<T> methodToExecute) throws Exception {
    final long startTime = System.currentTimeMillis();
    timers.put(
        context.name() + "#" + context.tags().get("type"), System.currentTimeMillis() - startTime);
    return methodToExecute.call();
  }
}
