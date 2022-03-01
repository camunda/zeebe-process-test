package io.camunda.zeebe.process.test.engine;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.logstreams.log.LogStreamReader;
import io.camunda.zeebe.logstreams.log.LoggedEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class EngineStateMonitorTest {

  @Test
  void testCallbackIsCalledWhenEngineIsAlreadyIdle()
      throws ExecutionException, InterruptedException, TimeoutException {
    // given
    final InMemoryLogStorage logStorage = mock(InMemoryLogStorage.class);
    final TestLogStreamReader logStreamReader = new TestLogStreamReader();
    final EngineStateMonitor monitor = new EngineStateMonitor(logStorage, logStreamReader);

    // when
    final CompletableFuture<Void> callbackFuture = new CompletableFuture<>();
    final Runnable callback = () -> callbackFuture.complete(null);
    monitor.addOnIdleCallback(callback);

    // then
    callbackFuture.get(50L, TimeUnit.MILLISECONDS);
    assertThat(callbackFuture).isCompleted();
  }

  @Test
  void testCallbackIsCalledWhenIdleStateIsReached()
      throws ExecutionException, InterruptedException, TimeoutException {
    // given
    final InMemoryLogStorage logStorage = mock(InMemoryLogStorage.class);
    final TestLogStreamReader logStreamReader = new TestLogStreamReader();
    final EngineStateMonitor monitor = new EngineStateMonitor(logStorage, logStreamReader);
    changeToBusyState(monitor, logStreamReader);

    // when
    final CompletableFuture<Void> callbackFuture = new CompletableFuture<>();
    final Runnable callback = () -> callbackFuture.complete(null);
    monitor.addOnIdleCallback(callback);

    // then
    callbackFuture.get(1L, TimeUnit.SECONDS);
    assertThat(callbackFuture).isCompleted();
  }

  @Test
  void testCallbackIsCalledWhenEngineIsAlreadyBusy()
      throws ExecutionException, InterruptedException, TimeoutException {
    // given
    final InMemoryLogStorage logStorage = mock(InMemoryLogStorage.class);
    final TestLogStreamReader logStreamReader = new TestLogStreamReader();
    final EngineStateMonitor monitor = new EngineStateMonitor(logStorage, logStreamReader);
    changeToBusyState(monitor, logStreamReader);
    // We need to set the position of the reader high enough the so hasNext() resolves to false. If
    // we don't do this adding the callback will make the engine idle because of the
    // forwardToLastEvent() method in the EngineStateMonitor, and the callback will never get called
    logStreamReader.setPosition(100L);

    // when
    final CompletableFuture<Void> callbackFuture = new CompletableFuture<>();
    final Runnable callback = () -> callbackFuture.complete(null);
    monitor.addOnProcessingCallback(callback);

    // then
    callbackFuture.get(1L, TimeUnit.SECONDS);
    assertThat(callbackFuture).isCompleted();
  }

  @Test
  void testCallbackIsCalledWhenBusyStateIsReached()
      throws ExecutionException, InterruptedException, TimeoutException {
    // given
    final InMemoryLogStorage logStorage = mock(InMemoryLogStorage.class);
    final TestLogStreamReader logStreamReader = new TestLogStreamReader();
    final EngineStateMonitor monitor = new EngineStateMonitor(logStorage, logStreamReader);

    final CompletableFuture<Void> callbackFuture = new CompletableFuture<>();
    final Runnable callback = () -> callbackFuture.complete(null);
    monitor.addOnProcessingCallback(callback);

    // when
    changeToBusyState(monitor, logStreamReader);

    // then
    callbackFuture.get(1L, TimeUnit.SECONDS);
    assertThat(callbackFuture).isCompleted();
  }

  private void changeToIdleState(
      final EngineStateMonitor monitor, final TestLogStreamReader reader) {
    // We use onCommit here because it is an easy way to trigger the EngineStateMonitor to check the
    // engine state and trigger the callbacks
    reader.setPosition(reader.getLastEventPosition());
    monitor.onCommit();
  }

  private void changeToBusyState(
      final EngineStateMonitor monitor, final TestLogStreamReader reader) {
    // We use onReplayed here because it is an easy way to update the lastProcessedEventPosition of
    // the EngineStateMonitor
    final long position = reader.getLastEventPosition() + 1;
    monitor.onReplayed(position, -1L);
    reader.setLastEventPosition(position);
  }

  private class TestLogStreamReader implements LogStreamReader {

    private long position = 0L;
    private long lastEventPosition = 0L;

    long getLastEventPosition() {
      return lastEventPosition;
    }

    void setLastEventPosition(final long position) {
      lastEventPosition = position;
    }

    void setPosition(final long position) {
      this.position = position;
    }

    @Override
    public boolean seekToNextEvent(final long position) {
      return false;
    }

    @Override
    public boolean seek(final long position) {
      return false;
    }

    @Override
    public void seekToFirstEvent() {}

    @Override
    public long seekToEnd() {
      return 0;
    }

    @Override
    public long getPosition() {
      return position;
    }

    @Override
    public LoggedEvent peekNext() {
      return null;
    }

    @Override
    public void close() {}

    @Override
    public boolean hasNext() {
      return position < lastEventPosition;
    }

    @Override
    public LoggedEvent next() {
      position++;
      final LoggedEvent event = mock(LoggedEvent.class);
      when(event.getPosition()).thenReturn(position);
      return event;
    }
  }
}
