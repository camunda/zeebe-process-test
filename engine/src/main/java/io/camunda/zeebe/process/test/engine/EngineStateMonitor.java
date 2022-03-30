/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.engine.processing.streamprocessor.StreamProcessorListener;
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecord;
import io.camunda.zeebe.logstreams.log.LogStreamReader;
import io.camunda.zeebe.logstreams.log.LoggedEvent;
import io.camunda.zeebe.logstreams.storage.LogStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Monitor that monitors whether the engine is busy or in idle state. Busy state is a state in which
 * the engine is actively writing new events to the logstream. Idle state is a state in which the
 * process engine makes no progress and is waiting for new commands or events to trigger</br>
 *
 * <p>On a technical level, idle state is defined by
 *
 * <ul>
 *   <li>The point in time when all current records in the commit log have been processed by the
 *       engine
 *   <li>This is insufficient, however, because the engine might still be in the process of writing
 *       follow-up records
 *   <li>Therefore, when the first idle state is detected, a grace period starts. If no new records
 *       come in during that grace period, then at the end onf the grace period callbacks are
 *       notified
 * </ul>
 */
final class EngineStateMonitor implements LogStorage.CommitListener, StreamProcessorListener {

  public static final int GRACE_PERIOD_MS = 50;
  private static final Timer TIMER = new Timer();
  private final List<Runnable> idleCallbacks = new ArrayList<>();
  private final List<Runnable> processingCallbacks = new ArrayList<>();
  private final LogStreamReader reader;
  private volatile long lastEventPosition = -1L;
  private volatile long lastProcessedPosition = -1L;

  private volatile TimerTask idleStateNotifier =
      createIdleStateNotifier(); // must never be null for the synchronization to work

  EngineStateMonitor(final InMemoryLogStorage logStorage, final LogStreamReader logStreamReader) {
    logStorage.addCommitListener(this);

    reader = logStreamReader;
  }

  public void addOnIdleCallback(final Runnable callback) {
    synchronized (idleCallbacks) {
      idleCallbacks.add(callback);
    }
    checkEngineStateAndNotifyCallbacks();
  }

  public void addOnProcessingCallback(final Runnable callback) {
    synchronized (processingCallbacks) {
      processingCallbacks.add(callback);
    }
    checkEngineStateAndNotifyCallbacks();
  }

  private void checkEngineStateAndNotifyCallbacks() {
    synchronized (idleStateNotifier) {
      if (isInIdleState()) {
        scheduleIdleStateNotification();
      } else {
        cancelIdleStateNotification();
        notifyProcessingCallbacks();
      }
    }
  }

  private void notifyProcessingCallbacks() {
    synchronized (processingCallbacks) {
      processingCallbacks.forEach(Runnable::run);
      processingCallbacks.clear();
    }
  }

  private void scheduleIdleStateNotification() {
    idleStateNotifier = createIdleStateNotifier();
    try {
      TIMER.schedule(idleStateNotifier, GRACE_PERIOD_MS);
    } catch (final IllegalStateException e) {
      // thrown - among others - if task was cancelled before it could be scheduled
      // do nothing in this case
    }
  }

  private void cancelIdleStateNotification() {
    idleStateNotifier.cancel();
  }

  private boolean isInIdleState() {
    forwardToLastEvent();
    return lastEventPosition == lastProcessedPosition;
  }

  private void forwardToLastEvent() {
    synchronized (reader) {
      while (reader.hasNext()) {
        lastEventPosition = reader.next().getPosition();
      }
    }
  }

  @Override
  public void onCommit() {
    checkEngineStateAndNotifyCallbacks();
  }

  @Override
  public void onProcessed(final TypedRecord<?> processedCommand) {
    lastProcessedPosition = Math.max(lastProcessedPosition, processedCommand.getPosition());
    checkEngineStateAndNotifyCallbacks();
  }

  @Override
  public void onSkipped(final LoggedEvent skippedRecord) {
    lastProcessedPosition = Math.max(lastProcessedPosition, skippedRecord.getPosition());
    checkEngineStateAndNotifyCallbacks();
  }

  @Override
  public void onReplayed(final long lastReplayedEventPosition, final long lastReadRecordPosition) {
    lastProcessedPosition = Math.max(lastProcessedPosition, lastReplayedEventPosition);
    checkEngineStateAndNotifyCallbacks();
  }

  private TimerTask createIdleStateNotifier() {
    return new TimerTask() {
      @Override
      public void run() {
        if (isInIdleState()) {
          synchronized (idleCallbacks) {
            idleCallbacks.forEach(Runnable::run);
            idleCallbacks.clear();
          }
        }
      }
    };
  }
}
