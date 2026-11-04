/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import io.camunda.search.clients.SearchClientsProxy;
import io.camunda.security.auth.BrokerRequestAuthorizationConverter;
import io.camunda.security.configuration.SecurityConfiguration;
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
import io.camunda.zeebe.stream.impl.ControllableStreamClockImpl;
import io.camunda.zeebe.stream.impl.StreamProcessor;
import io.camunda.zeebe.util.FeatureFlags;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class EngineFactory {
  private static final int EPHEMERAL_PORT = 0;

  public static ZeebeTestEngine create() {
    return create(EPHEMERAL_PORT);
  }

  public static ZeebeTestEngine create(final Consumer<Intent> requestListener) {
    return create(EPHEMERAL_PORT, requestListener);
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
    final LogStream logStream = createLogStream(logStorage, partitionId, clock);

    final CommandWriter commandWriter = new CommandWriter(logStream.newLogStreamWriter());
    final CommandSender commandSender = new CommandSender(commandWriter);
    final GatewayRequestStore gatewayRequestStore = new GatewayRequestStore();
    final InMemoryJobStreamer jobStreamer = new InMemoryJobStreamer(commandWriter);
    final AtomicReference<Server> grpcServerReference = new AtomicReference<>();
    final IntSupplier boundPort = () -> getBoundPort(grpcServerReference.get());
    final GrpcToLogStreamGateway gateway =
        new GrpcToLogStreamGateway(
            commandWriter,
            partitionId,
            partitionCount,
            boundPort,
            gatewayRequestStore,
            jobStreamer);
    final Server grpcServer = ServerBuilder.forPort(port).addService(gateway).build();
    grpcServerReference.set(grpcServer);

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
            jobStreamer,
            clock);

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

  private static int getBoundPort(final Server grpcServer) {
    try {
      final List<? extends SocketAddress> listenSockets = grpcServer.getListenSockets();
      if (listenSockets.isEmpty() || !(listenSockets.get(0) instanceof InetSocketAddress address)) {
        throw new IllegalStateException("The gRPC server has no Internet socket");
      }
      final int port = address.getPort();
      if (port == 0) {
        throw new IllegalStateException("The gRPC server is not bound to a port");
      }
      return port;
    } catch (final RuntimeException e) {
      throw new IllegalStateException("Could not retrieve the bound port from the gRPC server", e);
    }
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
      final LogStorage logStorage, final int partitionId, final ActorClock clock) {
    return LogStream.builder()
        .withPartitionId(partitionId)
        .withLogStorage(logStorage)
        .withClock(clock)
        .withMeterRegistry(new SimpleMeterRegistry())
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
      final JobStreamer jobStreamer,
      final ActorClock clock) {
    final SecurityConfiguration securityConfiguration = new SecurityConfiguration();
    securityConfiguration.getAuthorizations().setEnabled(false);
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
                            jobStreamer,
                            SearchClientsProxy.noop(),
                            new BrokerRequestAuthorizationConverter(securityConfiguration)),
                    new EngineConfiguration(),
                    securityConfiguration)))
        .actorSchedulingService(scheduler)
        .clock(new ControllableStreamClockImpl(clock))
        .meterRegistry(new SimpleMeterRegistry())
        .build();
  }
}
