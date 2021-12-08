package io.camunda.zeebe.process.test.testengine;

import io.camunda.zeebe.logstreams.storage.LogStorage;
import io.camunda.zeebe.logstreams.storage.LogStorageReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public class InMemoryLogStorage implements LogStorage {

  private final ConcurrentSkipListMap<Long, Integer> positionIndexMapping =
      new ConcurrentSkipListMap<>();
  private final List<ByteBuffer> logEntries = new ArrayList<>();
  private final Set<CommitListener> commitListeners = new HashSet<>();

  @Override
  public LogStorageReader newReader() {
    return new ListLogStorageReader();
  }

  @Override
  public void addCommitListener(final CommitListener listener) {
    commitListeners.add(listener);
  }

  @Override
  public void removeCommitListener(final CommitListener listener) {
    commitListeners.remove(listener);
  }

  @Override
  public void append(
      final long lowestPosition,
      final long highestPosition,
      final ByteBuffer blockBuffer,
      final AppendListener listener) {
    try {
      logEntries.add(blockBuffer);
      final int index = logEntries.size();
      positionIndexMapping.put(lowestPosition, index);
      listener.onWrite(index);

      listener.onCommit(index);
      commitListeners.forEach(LogStorage.CommitListener::onCommit);
    } catch (Exception e) {
      listener.onWriteError(e);
    }
  }

  private class ListLogStorageReader implements LogStorageReader {

    private int currentIndex = 0;

    @Override
    public void seek(final long position) {
      currentIndex =
          Optional.ofNullable(positionIndexMapping.lowerEntry(position))
              .map(Entry::getValue)
              .map(index -> index - 1)
              .orElse(0);
    }

    @Override
    public void close() {}

    @Override
    public boolean hasNext() {
      return currentIndex >= 0 && currentIndex < logEntries.size();
    }

    @Override
    public DirectBuffer next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      final int index = currentIndex;
      currentIndex++;
      return new UnsafeBuffer(logEntries.get(index));
    }
  }
}
