package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.engine.processing.streamprocessor.StreamProcessor;
import io.camunda.zeebe.engine.state.ZbColumnFamilies;
import io.camunda.zeebe.logstreams.log.LogStream;
import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.util.sched.ActorScheduler;
import io.camunda.zeebe.util.sched.clock.ControlledActorClock;
import io.grpc.Server;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryEngineImpl implements InMemoryEngine {

  private static final Logger LOG = LoggerFactory.getLogger(InMemoryEngineImpl.class);

  private final Server grpcServer;
  private final StreamProcessor streamProcessor;
  private final GrpcToLogStreamGateway gateway;
  private final ZeebeDb<ZbColumnFamilies> database;
  private final LogStream logStream;
  private final ActorScheduler scheduler;
  private final RecordStreamSource recordStream;
  private final ControlledActorClock clock;
  private final IdleStateMonitor idleStateMonitor;

  public InMemoryEngineImpl(
      final Server grpcServer,
      final StreamProcessor streamProcessor,
      final GrpcToLogStreamGateway gateway,
      final ZeebeDb<ZbColumnFamilies> database,
      final LogStream logStream,
      final ActorScheduler scheduler,
      final RecordStreamSource recordStream,
      final ControlledActorClock clock,
      final IdleStateMonitor idleStateMonitor) {
    this.grpcServer = grpcServer;
    this.streamProcessor = streamProcessor;
    this.gateway = gateway;
    this.database = database;
    this.logStream = logStream;
    this.scheduler = scheduler;
    this.recordStream = recordStream;
    this.clock = clock;
    this.idleStateMonitor = idleStateMonitor;
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
      gateway.close();
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
  public RecordStreamSource getRecordStream() {
    return recordStream;
  }

  @Override
  public ZeebeClient createClient() {
    return ZeebeClient.newClientBuilder()
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
  public void runOnIdleState(final Runnable callback) {
    idleStateMonitor.addCallback(callback);
  }

  @Override
  public void waitForIdleState() {
    final CompletableFuture<Void> idleState = new CompletableFuture<>();

    runOnIdleState(() -> idleState.complete(null));

    idleState.join();
  }
}
