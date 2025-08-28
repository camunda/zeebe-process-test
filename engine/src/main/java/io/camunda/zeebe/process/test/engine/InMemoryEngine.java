/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import io.camunda.client.impl.CamundaObjectMapper;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.logstreams.log.LogStream;
import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.scheduler.ActorScheduler;
import io.camunda.zeebe.scheduler.clock.ControlledActorClock;
import io.camunda.zeebe.stream.impl.StreamProcessor;
import io.grpc.Server;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryEngine implements ZeebeTestEngine {

  private static final Logger LOG = LoggerFactory.getLogger(InMemoryEngine.class);

  private final Server grpcServer;
  private final StreamProcessor streamProcessor;
  private final GrpcToLogStreamGateway gateway;
  private final ZeebeDb<ZbColumnFamilies> database;
  private final LogStream logStream;
  private final ActorScheduler scheduler;
  private final RecordStreamSource recordStream;
  private final ControlledActorClock clock;
  private final EngineStateMonitor engineStateMonitor;

  public InMemoryEngine(
      final Server grpcServer,
      final StreamProcessor streamProcessor,
      final GrpcToLogStreamGateway gateway,
      final ZeebeDb<ZbColumnFamilies> database,
      final LogStream logStream,
      final ActorScheduler scheduler,
      final RecordStreamSource recordStream,
      final ControlledActorClock clock,
      final EngineStateMonitor engineStateMonitor) {
    this.grpcServer = grpcServer;
    this.streamProcessor = streamProcessor;
    this.gateway = gateway;
    this.database = database;
    this.logStream = logStream;
    this.scheduler = scheduler;
    this.recordStream = recordStream;
    this.clock = clock;
    this.engineStateMonitor = engineStateMonitor;
  }

  @Override
  public void start() {
    try {
      grpcServer.start();
      streamProcessor.openAsync(false).join();
    } catch (final IOException e) {
      LOG.error("Failed starting in memory engine", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stop() {
    try {
      grpcServer.shutdownNow();
      grpcServer.awaitTermination();
      streamProcessor.close();
      database.close();
      logStream.close();
      scheduler.stop();
    } catch (final Exception e) {
      LOG.error("Failed stopping in memory engine", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public RecordStreamSource getRecordStreamSource() {
    return recordStream;
  }

  @Override
  public CamundaClient createClient() {
    return CamundaClient.newClientBuilder()
        .applyEnvironmentVariableOverrides(false)
        .preferRestOverGrpc(false)
        .gatewayAddress(getGatewayAddress())
        .usePlaintext()
        .build();
  }

  @Override
  public CamundaClient createClient(final ObjectMapper objectMapper) {
    return CamundaClient.newClientBuilder()
        .withJsonMapper(new CamundaObjectMapper(objectMapper))
        .applyEnvironmentVariableOverrides(false)
        .preferRestOverGrpc(false)
        .gatewayAddress(getGatewayAddress())
        .usePlaintext()
        .build();
  }

  @Override
  public String getGatewayAddress() {
    return gateway.getAddress();
  }

  @Override
  public void increaseTime(final Duration timeToAdd) {
    clock.addTime(timeToAdd);
  }

  @Override
  public void waitForIdleState(final Duration timeout)
      throws InterruptedException, TimeoutException {
    final CompletableFuture<Void> idleState = new CompletableFuture<>();

    engineStateMonitor.addOnIdleCallback(() -> idleState.complete(null));

    try {
      idleState.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (final ExecutionException e) {
      // Do nothing. ExecutionExceptions won't appear. The function only completes the future, which
      // in itself does not throw any exceptions.
    }
  }

  @Override
  public void waitForBusyState(final Duration timeout)
      throws InterruptedException, TimeoutException {
    final CompletableFuture<Void> processingState = new CompletableFuture<>();

    engineStateMonitor.addOnProcessingCallback(() -> processingState.complete(null));

    try {
      processingState.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (final ExecutionException e) {
      // Do nothing. ExecutionExceptions won't appear. The function only completes the future, which
      // in itself does not throw any exceptions.
    }
  }
}
