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
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.WaitForIdleStateRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.WaitForIdleStateResponse;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

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
    engine = EngineFactory.create();
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
    engine.waitForIdleState();
    final WaitForIdleStateResponse response = WaitForIdleStateResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
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
