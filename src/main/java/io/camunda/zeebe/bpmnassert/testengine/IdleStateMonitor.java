package io.camunda.zeebe.bpmnassert.testengine;

import io.camunda.zeebe.engine.processing.streamprocessor.StreamProcessorListener;
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecord;
import io.camunda.zeebe.logstreams.log.LogStreamReader;
import io.camunda.zeebe.logstreams.log.LoggedEvent;
import io.camunda.zeebe.logstreams.storage.LogStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

final class IdleStateMonitor implements LogStorage.CommitListener, StreamProcessorListener {

  private static final Timer TIMER = new Timer();
  private final List<Runnable> callbacks = new ArrayList<>();
  private final LogStreamReader reader;
  private volatile long lastEventPosition = -1L;
  private volatile long lastProcessedPosition = -1L;

  private volatile TimerTask task =
      createTask(); // must never be null for the synchronization to work

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
    if (isInIdleState()) {
      synchronized (task) {
        task = createTask();
        try {
          TIMER.schedule(task, 10);
        } catch (IllegalStateException e) {
          // thrown - among others - if task was cancelled before it could be scheduled
          // do nothing in this case
        }
      }
    } else {
      synchronized (task) {
        task.cancel();
      }
    }
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

  private TimerTask createTask() {
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
