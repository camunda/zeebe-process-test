package io.camunda.zeebe.process.test.engine.agent;

import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.engine.EngineFactory;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlGrpc.EngineControlImplBase;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.GetRecordsRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.IncreaseTimeRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.IncreaseTimeResponse;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.RecordResponse;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.ResetEngineRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.ResetEngineResponse;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.StartEngineRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.StartEngineResponse;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.StopEngineRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.StopEngineResponse;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.WaitForBusyStateRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.WaitForBusyStateResponse;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.WaitForIdleStateRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.WaitForIdleStateResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EngineControlImpl extends EngineControlImplBase {

  private static final Logger LOG = LoggerFactory.getLogger(EngineControlImpl.class);

  private InMemoryEngine engine;
  private RecordStreamSourceWrapper recordStreamSource;

  public EngineControlImpl(final InMemoryEngine engine) {
    this.engine = engine;
    this.recordStreamSource = new RecordStreamSourceWrapper(engine.getRecordStreamSource());
  }

  @Override
  public void startEngine(
      final StartEngineRequest request,
      final StreamObserver<StartEngineResponse> responseObserver) {
    engine.start();

    final StartEngineResponse response = StartEngineResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void stopEngine(
      final StopEngineRequest request, final StreamObserver<StopEngineResponse> responseObserver) {
    engine.stop();

    final StopEngineResponse response = StopEngineResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void resetEngine(
      final ResetEngineRequest request,
      final StreamObserver<ResetEngineResponse> responseObserver) {
    engine.stop();
    engine = EngineFactory.create(AgentProperties.getGatewayPort());
    recordStreamSource = new RecordStreamSourceWrapper(engine.getRecordStreamSource());
    engine.start();

    final ResetEngineResponse response = ResetEngineResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void increaseTime(
      final IncreaseTimeRequest request,
      final StreamObserver<IncreaseTimeResponse> responseObserver) {
    engine.increaseTime(Duration.ofMillis(request.getMilliseconds()));
    final IncreaseTimeResponse response = IncreaseTimeResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void waitForIdleState(
      final WaitForIdleStateRequest request,
      final StreamObserver<WaitForIdleStateResponse> responseObserver) {
    try {
      engine.waitForIdleState(Duration.ofMillis(request.getTimeout()));
      final WaitForIdleStateResponse response = WaitForIdleStateResponse.newBuilder().build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (InterruptedException e) {
      responseObserver.onError(Status.INTERNAL.withCause(e).asException());
    } catch (TimeoutException e) {
      responseObserver.onError(
          Status.DEADLINE_EXCEEDED
              .withDescription(
                  String.format(
                      "Engine has not reached idle state within specified timeout of %d ms",
                      request.getTimeout()))
              .withCause(e)
              .asException());
    }
  }

  @Override
  public void waitForBusyState(
      final WaitForBusyStateRequest request,
      final StreamObserver<WaitForBusyStateResponse> responseObserver) {
    try {
      engine.waitForBusyState(Duration.ofMillis(request.getTimeout()));
      final WaitForBusyStateResponse response = WaitForBusyStateResponse.newBuilder().build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (InterruptedException e) {
      responseObserver.onError(Status.INTERNAL.withCause(e).asException());
    } catch (TimeoutException e) {
      responseObserver.onError(
          Status.DEADLINE_EXCEEDED
              .withDescription(
                  String.format(
                      "Engine has not started processing within specified timeout of %d ms",
                      request.getTimeout()))
              .withCause(e)
              .asException());
    }
  }

  @Override
  public void getRecords(
      final GetRecordsRequest request, final StreamObserver<RecordResponse> responseObserver) {
    final List<String> mappedRecords = recordStreamSource.getMappedRecords();

    mappedRecords.forEach(
        record ->
            responseObserver.onNext(RecordResponse.newBuilder().setRecordJson(record).build()));

    responseObserver.onCompleted();
  }
}
