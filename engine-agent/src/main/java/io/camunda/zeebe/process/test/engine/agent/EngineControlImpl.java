package io.camunda.zeebe.process.test.engine.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.camunda.zeebe.protocol.record.Record;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Duration;

public class EngineControlImpl extends EngineControlImplBase {

  private InMemoryEngine engine;

  public EngineControlImpl(final InMemoryEngine engine) {
    this.engine = engine;
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
    final ObjectMapper mapper = new ObjectMapper();
    final Iterable<Record<?>> records = engine.getRecordStream().records();
    for (final Record<?> record : records) {
      try {
        final String recordJson = mapper.writeValueAsString(record);
        final RecordResponse response =
            RecordResponse.newBuilder().setRecordJson(recordJson).build();
        responseObserver.onNext(response);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        responseObserver.onError(Status.INTERNAL.asException());
        return;
      }
    }
    responseObserver.onCompleted();
  }
}
