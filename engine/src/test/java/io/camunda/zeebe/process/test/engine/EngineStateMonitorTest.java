/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EngineStateMonitorTest {

  private InMemoryLogStorage logStorage;
  private TestLogStreamReader logStreamReader;
  private EngineStateMonitor monitor;

  @BeforeEach
  void beforeEach() {
    logStorage = mock(InMemoryLogStorage.class);
    logStreamReader = new TestLogStreamReader();
    monitor = new EngineStateMonitor(logStorage, logStreamReader);
  }

  @Test
  void testOnIdleCallbackIsCalledWhenEngineIsAlreadyIdle()
      throws ExecutionException, InterruptedException, TimeoutException {
    // given
    final CompletableFuture<Void> callbackFuture = new CompletableFuture<>();
    final Runnable callback = () -> callbackFuture.complete(null);

    // when
    changeToIdleState(monitor, logStreamReader, false);
    monitor.addOnIdleCallback(callback);

    // then
    callbackFuture.get(1L, TimeUnit.SECONDS);
    assertThat(callbackFuture).isCompleted();
  }

  @Test
  void testOnIdleCallbackIsCalledWhenIdleStateIsReached()
      throws ExecutionException, InterruptedException, TimeoutException {
    // given
    final CompletableFuture<Void> callbackFuture = new CompletableFuture<>();
    final Runnable callback = () -> callbackFuture.complete(null);

    // when
    changeToBusyState(monitor, logStreamReader, true);
    monitor.addOnIdleCallback(callback);
    changeToIdleState(monitor, logStreamReader, false);

    // then
    callbackFuture.get(1L, TimeUnit.SECONDS);
    assertThat(callbackFuture).isCompleted();
  }

  @Test
  void testOnProcessingCallbackIsCalledWhenEngineIsAlreadyBusy()
      throws ExecutionException, InterruptedException, TimeoutException {
    // given
    final CompletableFuture<Void> callbackFuture = new CompletableFuture<>();
    final Runnable callback = () -> callbackFuture.complete(null);

    // when
    changeToBusyState(monitor, logStreamReader, true);
    monitor.addOnProcessingCallback(callback);

    // then
    callbackFuture.get(1L, TimeUnit.SECONDS);
    assertThat(callbackFuture).isCompleted();
  }

  @Test
  void testOnProcessingCallbackIsCalledWhenBusyStateIsReached()
      throws ExecutionException, InterruptedException, TimeoutException {
    // given
    final CompletableFuture<Void> callbackFuture = new CompletableFuture<>();
    final Runnable callback = () -> callbackFuture.complete(null);

    // when
    changeToIdleState(monitor, logStreamReader, true);
    monitor.addOnProcessingCallback(callback);
    changeToBusyState(monitor, logStreamReader, false);

    // then
    callbackFuture.get(1L, TimeUnit.SECONDS);
    assertThat(callbackFuture).isCompleted();
  }

  private void changeToIdleState(
      final EngineStateMonitor monitor, final TestLogStreamReader reader, final boolean lockState) {
    // We use onCommit here because it is an easy way to synchronize the EngineStateMonitor with the
    // TestLogStreamReader. For this synchronize to happen the state cannot be locked!
    reader.setStateLocked(false);
    monitor.onCommit();
    reader.setStateLocked(lockState);
  }

  private void changeToBusyState(
      final EngineStateMonitor monitor, final TestLogStreamReader reader, final boolean lockState) {
    // We use onReplayed here because it is an easy way to update the lastProcessedEventPosition of
    // the EngineStateMonitor
    reader.setStateLocked(lockState);
    final long position = reader.getLastEventPosition() + 1;
    monitor.onReplayed(position, -1L);
    reader.setLastEventPosition(position);
  }

  private class TestLogStreamReader implements LogStreamReader {

    private boolean stateLocked = false;
    private long position = -1L;
    private long lastEventPosition = -1L;

    void setStateLocked(final boolean stateLocked) {
      this.stateLocked = stateLocked;
    }

    long getLastEventPosition() {
      return lastEventPosition;
    }

    void setLastEventPosition(final long position) {
      lastEventPosition = position;
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
      return !stateLocked && position < lastEventPosition;
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
