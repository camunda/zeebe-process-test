package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.engine.processing.EngineProcessors;
import io.camunda.zeebe.engine.processing.streamprocessor.StreamProcessor;
import io.camunda.zeebe.engine.state.ZbColumnFamilies;
import io.camunda.zeebe.engine.state.appliers.EventAppliers;
import io.camunda.zeebe.logstreams.log.LogStream;
import io.camunda.zeebe.logstreams.log.LogStreamBuilder;
import io.camunda.zeebe.logstreams.log.LogStreamReader;
import io.camunda.zeebe.logstreams.storage.LogStorage;
import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.engine.db.InMemoryDbFactory;
import io.camunda.zeebe.util.sched.Actor;
import io.camunda.zeebe.util.sched.ActorScheduler;
import io.camunda.zeebe.util.sched.ActorSchedulingService;
import io.camunda.zeebe.util.sched.clock.ActorClock;
import io.camunda.zeebe.util.sched.clock.ControlledActorClock;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.util.concurrent.CompletableFuture;

public class EngineFactory {

  public static InMemoryEngine create() {
    final int partitionId = 1;
    final int partitionCount = 1;
    final int port = 26500;

    final ControlledActorClock clock = createActorClock();
    final ActorScheduler scheduler = createAndStartActorScheduler(clock);

    final InMemoryLogStorage logStorage = new InMemoryLogStorage();
    final LogStream logStream = createLogStream(logStorage, scheduler, partitionId);

    final SubscriptionCommandSenderFactory subscriptionCommandSenderFactory =
        new SubscriptionCommandSenderFactory(
            logStream.newLogStreamRecordWriter().join(), partitionId);

    final GrpcToLogStreamGateway gateway =
        new GrpcToLogStreamGateway(
            logStream.newLogStreamRecordWriter().join(), partitionId, partitionCount, port);
    final Server grpcServer = ServerBuilder.forPort(port).addService(gateway).build();
    final GrpcResponseWriter grpcResponseWriter = new GrpcResponseWriter(gateway);

    final ZeebeDb<ZbColumnFamilies> zeebeDb = createDatabase();

    final IdleStateMonitor idleStateMonitor =
        new IdleStateMonitor(logStorage, logStream.newLogStreamReader().join());

    final StreamProcessor streamProcessor =
        createStreamProcessor(
            logStream,
            zeebeDb,
            scheduler,
            grpcResponseWriter,
            idleStateMonitor,
            partitionCount,
            subscriptionCommandSenderFactory);

    final LogStreamReader reader = logStream.newLogStreamReader().join();
    final RecordStreamSourceImpl recordStream = new RecordStreamSourceImpl(reader, partitionId);

    return new InMemoryEngineImpl(
        grpcServer,
        streamProcessor,
        gateway,
        zeebeDb,
        logStream,
        scheduler,
        recordStream,
        clock,
        idleStateMonitor);
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
    final LogStreamBuilder builder =
        LogStream.builder()
            .withPartitionId(partitionId)
            .withLogStorage(logStorage)
            .withActorSchedulingService(scheduler);

    final CompletableFuture<LogStream> theFuture = new CompletableFuture<>();

    scheduler.submitActor(
        Actor.wrap(
            (control) ->
                builder
                    .buildAsync()
                    .onComplete(
                        (logStream, failure) -> {
                          if (failure != null) {
                            theFuture.completeExceptionally(failure);
                          } else {
                            theFuture.complete(logStream);
                          }
                        })));

    return theFuture.join();
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
      final IdleStateMonitor idleStateMonitor,
      final int partitionCount,
      final SubscriptionCommandSenderFactory subscriptionCommandSenderFactory) {
    return StreamProcessor.builder()
        .logStream(logStream)
        .zeebeDb(database)
        .eventApplierFactory(EventAppliers::new)
        .commandResponseWriter(grpcResponseWriter)
        .streamProcessorFactory(
            context ->
                EngineProcessors.createEngineProcessors(
                    context.listener(idleStateMonitor),
                    partitionCount,
                    subscriptionCommandSenderFactory.createSender(),
                    new SinglePartitionDeploymentDistributor(),
                    new SinglePartitionDeploymentResponder(),
                    jobType -> {}))
        .actorSchedulingService(scheduler)
        .build();
  }
}
