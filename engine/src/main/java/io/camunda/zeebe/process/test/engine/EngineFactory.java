/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.engine.Engine;
import io.camunda.zeebe.engine.EngineConfiguration;
import io.camunda.zeebe.engine.processing.EngineProcessors;
import io.camunda.zeebe.engine.processing.message.command.SubscriptionCommandSender;
import io.camunda.zeebe.engine.processing.streamprocessor.JobStreamer;
import io.camunda.zeebe.logstreams.log.LogStream;
import io.camunda.zeebe.logstreams.log.LogStreamReader;
import io.camunda.zeebe.logstreams.storage.LogStorage;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.engine.db.InMemoryDbFactory;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.scheduler.ActorScheduler;
import io.camunda.zeebe.scheduler.ActorSchedulingService;
import io.camunda.zeebe.scheduler.clock.ActorClock;
import io.camunda.zeebe.scheduler.clock.ControlledActorClock;
import io.camunda.zeebe.stream.impl.StreamProcessor;
import io.camunda.zeebe.util.FeatureFlags;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.function.Consumer;

public class EngineFactory {

  public static ZeebeTestEngine create() {
    return create(findFreePort());
  }

  private static int findFreePort() {
    final int freePort;
    try (final var serverSocket = new ServerSocket(0)) {
      freePort = serverSocket.getLocalPort();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return freePort;
  }

  public static ZeebeTestEngine create(final Consumer<Intent> requestListener) {
    return create(findFreePort(), requestListener);
  }

  public static ZeebeTestEngine create(final int port) {
    return create(port, null);
  }

  private static ZeebeTestEngine create(final int port, final Consumer<Intent> requestListener) {
    final int partitionId = 1;
    final int partitionCount = 1;

    final ControlledActorClock clock = createActorClock();
    final ActorScheduler scheduler = createAndStartActorScheduler(clock);

    final InMemoryLogStorage logStorage = new InMemoryLogStorage();
    final LogStream logStream = createLogStream(logStorage, scheduler, partitionId);

    final CommandWriter commandWriter = new CommandWriter(logStream.newLogStreamWriter());
    final CommandSender commandSender = new CommandSender(commandWriter);
    final GatewayRequestStore gatewayRequestStore = new GatewayRequestStore();
    final InMemoryJobStreamer jobStreamer = new InMemoryJobStreamer(commandWriter);
    final GrpcToLogStreamGateway gateway =
        new GrpcToLogStreamGateway(
            commandWriter, partitionId, partitionCount, port, gatewayRequestStore, jobStreamer);
    final Server grpcServer = ServerBuilder.forPort(port).addService(gateway).build();

    final GrpcResponseWriter grpcResponseWriter =
        new GrpcResponseWriter(gateway, gatewayRequestStore, requestListener);

    final ZeebeDb<ZbColumnFamilies> zeebeDb = createDatabase();

    final StreamProcessor streamProcessor =
        createStreamProcessor(
            logStream,
            zeebeDb,
            scheduler,
            grpcResponseWriter,
            partitionCount,
            commandSender,
            jobStreamer);

    final EngineStateMonitor engineStateMonitor =
        new EngineStateMonitor(logStorage, streamProcessor);

    final LogStreamReader reader = logStream.newLogStreamReader();
    final RecordStreamSourceImpl recordStream = new RecordStreamSourceImpl(reader, partitionId);

    return new InMemoryEngine(
        grpcServer,
        streamProcessor,
        gateway,
        zeebeDb,
        logStream,
        scheduler,
        recordStream,
        clock,
        engineStateMonitor);
  }

  private static ControlledActorClock createActorClock() {
    return new ControlledActorClock();
  }

  private static ActorScheduler createAndStartActorScheduler(final ActorClock clock) {
    final ActorScheduler scheduler =
        ActorScheduler.newActorScheduler().setActorClock(clock).build();
    scheduler.start();
    return scheduler;
  }

  private static LogStream createLogStream(
      final LogStorage logStorage, final ActorSchedulingService scheduler, final int partitionId) {
    return LogStream.builder()
        .withPartitionId(partitionId)
        .withLogStorage(logStorage)
        .withActorSchedulingService(scheduler)
        .build();
  }

  private static ZeebeDb<ZbColumnFamilies> createDatabase() {
    final InMemoryDbFactory<ZbColumnFamilies> factory = new InMemoryDbFactory<>();
    return factory.createDb();
  }

  private static StreamProcessor createStreamProcessor(
      final LogStream logStream,
      final ZeebeDb<ZbColumnFamilies> database,
      final ActorSchedulingService scheduler,
      final GrpcResponseWriter grpcResponseWriter,
      final int partitionCount,
      final CommandSender commandSender,
      final JobStreamer jobStreamer) {
    return StreamProcessor.builder()
        .logStream(logStream)
        .zeebeDb(database)
        .commandResponseWriter(grpcResponseWriter)
        .partitionCommandSender(commandSender)
        .recordProcessors(
            List.of(
                new Engine(
                    context ->
                        EngineProcessors.createEngineProcessors(
                            context,
                            partitionCount,
                            new SubscriptionCommandSender(context.getPartitionId(), commandSender),
                            commandSender,
                            FeatureFlags.createDefault(),
                            jobStreamer),
                    new EngineConfiguration())))
        .actorSchedulingService(scheduler)
        .build();
  }
}
