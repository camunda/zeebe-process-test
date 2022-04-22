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

import io.camunda.zeebe.engine.processing.streamprocessor.StreamProcessor;
import io.camunda.zeebe.util.sched.future.CompletableActorFuture;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EngineStateMonitorTest {

  private StreamProcessor mockStreamProcessor;
  private EngineStateMonitor monitor;

  @BeforeEach
  void beforeEach() {
    final InMemoryLogStorage mockLogStorage = mock(InMemoryLogStorage.class);
    mockStreamProcessor = mock(StreamProcessor.class);
    monitor = new EngineStateMonitor(mockLogStorage, mockStreamProcessor);
  }

  @Test
  void testOnIdleCallbackIsCalledWhenEngineIsAlreadyIdle()
      throws ExecutionException, InterruptedException, TimeoutException {
    // given
    final CompletableFuture<Void> callbackFuture = new CompletableFuture<>();
    final Runnable callback = () -> callbackFuture.complete(null);

    // when
    changeToIdleState(mockStreamProcessor);
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
    changeToBusyState(monitor, mockStreamProcessor);
    monitor.addOnIdleCallback(callback);
    changeToIdleState(mockStreamProcessor);

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
    changeToBusyState(monitor, mockStreamProcessor);
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
    changeToIdleState(mockStreamProcessor);
    monitor.addOnProcessingCallback(callback);
    changeToBusyState(monitor, mockStreamProcessor);

    // then
    callbackFuture.get(1L, TimeUnit.SECONDS);
    assertThat(callbackFuture).isCompleted();
  }

  private void changeToIdleState(final StreamProcessor streamProcessor) {
    when(streamProcessor.hasProcessingReachedTheEnd())
        .thenReturn(CompletableActorFuture.completed(true));
  }

  private void changeToBusyState(
      final EngineStateMonitor monitor, final StreamProcessor streamProcessor) {
    when(streamProcessor.hasProcessingReachedTheEnd())
        .thenReturn(CompletableActorFuture.completed(false));
    monitor.onCommit();
  }
}
