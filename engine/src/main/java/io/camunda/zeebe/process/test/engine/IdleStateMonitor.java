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
 * Monitor that waits for an idle state. Idle state is a state in which the process engine makes no
 * progress and is waiting for new commands or events to trigger</br>
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
final class IdleStateMonitor implements LogStorage.CommitListener, StreamProcessorListener {

  private static final Timer TIMER = new Timer();
  public static final int GRACE_PERIOD = 10;
  private final List<Runnable> callbacks = new ArrayList<>();
  private final LogStreamReader reader;
  private volatile long lastEventPosition = -1L;
  private volatile long lastProcessedPosition = -1L;

  private volatile TimerTask idleStateNotifier =
      createIdleStateNotifier(); // must never be null for the synchronization to work

  IdleStateMonitor(final InMemoryLogStorage logStorage, final LogStreamReader logStreamReader) {
    logStorage.addCommitListener(this);

    reader = logStreamReader;
  }

  public void addCallback(final Runnable callback) {
    synchronized (callbacks) {
      callbacks.add(callback);
    }
    checkIdleState();
  }

  private void checkIdleState() {
    synchronized (idleStateNotifier) {
      if (isInIdleState()) {
        scheduleNotification();
      } else {
        cancelNotification();
      }
    }
  }

  private void scheduleNotification() {
    idleStateNotifier = createIdleStateNotifier();
    try {
      TIMER.schedule(idleStateNotifier, GRACE_PERIOD);
    } catch (IllegalStateException e) {
      // thrown - among others - if task was cancelled before it could be scheduled
      // do nothing in this case
    }
  }

  private void cancelNotification() {
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
    checkIdleState();
  }

  @Override
  public void onProcessed(final TypedRecord<?> processedCommand) {
    lastProcessedPosition = Math.max(lastProcessedPosition, processedCommand.getPosition());
    checkIdleState();
  }

  @Override
  public void onSkipped(final LoggedEvent skippedRecord) {
    lastProcessedPosition = Math.max(lastProcessedPosition, skippedRecord.getPosition());
    checkIdleState();
  }

  @Override
  public void onReplayed(final long lastReplayedEventPosition, final long lastReadRecordPosition) {
    lastProcessedPosition = Math.max(lastProcessedPosition, lastReplayedEventPosition);
    checkIdleState();
  }

  private TimerTask createIdleStateNotifier() {
    return new TimerTask() {
      @Override
      public void run() {
        if (isInIdleState()) {
          synchronized (callbacks) {
            callbacks.forEach(Runnable::run);
            callbacks.clear();
          }
        }
      }
    };
  }
}
