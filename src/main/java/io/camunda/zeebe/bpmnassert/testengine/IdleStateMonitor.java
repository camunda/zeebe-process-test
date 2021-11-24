package io.camunda.zeebe.bpmnassert.testengine;

import io.camunda.zeebe.engine.processing.streamprocessor.StreamProcessorListener;
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecord;
import io.camunda.zeebe.logstreams.log.LogStreamReader;
import io.camunda.zeebe.logstreams.log.LoggedEvent;
import io.camunda.zeebe.logstreams.storage.LogStorage;
import java.util.ArrayList;
import java.util.List;

final class IdleStateMonitor implements LogStorage.CommitListener, StreamProcessorListener {

  private final List<Runnable> callbacks = new ArrayList<>();
  private final LogStreamReader reader;
  private long lastEventPosition = -1L;
  private long lastProcessedPosition = -1;

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
      synchronized (callbacks) {
        callbacks.forEach(Runnable::run);
        callbacks.clear();
      }
    }
  }

  private boolean isInIdleState() {
    forwardToLastEvent();
    return lastEventPosition == lastProcessedPosition;
  }

  private void forwardToLastEvent() {
    while (reader.hasNext()) {
      lastEventPosition = reader.next().getPosition();
    }
  }

  @Override
  public void onCommit() {
    checkIdleState();
  }

  @Override
  public void onProcessed(final TypedRecord<?> processedCommand) {
    lastProcessedPosition = processedCommand.getPosition();
    checkIdleState();
  }

  @Override
  public void onSkipped(final LoggedEvent skippedRecord) {
    lastProcessedPosition = skippedRecord.getPosition();
    checkIdleState();
  }

  @Override
  public void onReplayed(final long lastReplayedEventPosition, final long lastReadRecordPosition) {
    lastProcessedPosition = lastReplayedEventPosition;
    checkIdleState();
  }
}
