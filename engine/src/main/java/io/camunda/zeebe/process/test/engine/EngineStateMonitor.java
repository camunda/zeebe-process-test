/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.engine.processing.streamprocessor.StreamProcessor;
import io.camunda.zeebe.logstreams.storage.LogStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

/**
 * Monitor that monitors whether the engine is busy or in idle state. Busy state is a state in which
 * the engine is actively writing new events to the logstream. Idle state is a state in which the
 * process engine makes no progress and is waiting for new commands or events to trigger</br>
 */
final class EngineStateMonitor implements LogStorage.CommitListener {

  public static final int GRACE_PERIOD_MS = 50;
  private static final Timer TIMER = new Timer();
  private final List<Runnable> idleCallbacks = new ArrayList<>();
  private final List<Runnable> processingCallbacks = new ArrayList<>();
  private final StreamProcessor streamProcessor;
  private volatile TimerTask stateNotifier;

  EngineStateMonitor(final InMemoryLogStorage logStorage, final StreamProcessor streamProcessor) {
    logStorage.addCommitListener(this);

    this.streamProcessor = streamProcessor;
  }

  public void addOnIdleCallback(final Runnable callback) {
    synchronized (idleCallbacks) {
      idleCallbacks.add(callback);
    }
    scheduleStateNotification();
  }

  public void addOnProcessingCallback(final Runnable callback) {
    synchronized (processingCallbacks) {
      processingCallbacks.add(callback);
    }
    scheduleStateNotification();
  }

  private synchronized void scheduleStateNotification() {
    if (stateNotifier != null) {
      // cancel last task
      stateNotifier.cancel();
      TIMER.purge();
    }

    stateNotifier = createStateNotifier();

    TIMER.scheduleAtFixedRate(stateNotifier, GRACE_PERIOD_MS, GRACE_PERIOD_MS);
  }

  private boolean isInIdleState() {
    return CompletableFuture.supplyAsync(() -> streamProcessor.hasProcessingReachedTheEnd().join())
        .join();
  }

  @Override
  public void onCommit() {
    notifyProcessingCallbacks(); // notify processing callbacks immediately
    scheduleStateNotification();
  }

  private void notifyIdleCallbacks() {
    synchronized (idleCallbacks) {
      idleCallbacks.forEach(Runnable::run);
      idleCallbacks.clear();
    }
  }

  private void notifyProcessingCallbacks() {
    synchronized (processingCallbacks) {
      processingCallbacks.forEach(Runnable::run);
      processingCallbacks.clear();
    }
  }

  private TimerTask createStateNotifier() {
    return new TimerTask() {
      @Override
      public void run() {
        if (isInIdleState()) {
          notifyIdleCallbacks();
        } else {
          notifyProcessingCallbacks();
        }
        if (idleCallbacks.isEmpty() && processingCallbacks.isEmpty()) {
          cancel();
        }
      }
    };
  }
}
