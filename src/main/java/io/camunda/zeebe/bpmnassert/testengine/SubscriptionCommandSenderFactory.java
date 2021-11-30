package io.camunda.zeebe.bpmnassert.testengine;

import io.camunda.zeebe.engine.processing.message.command.SubscriptionCommandMessageHandler;
import io.camunda.zeebe.engine.processing.message.command.SubscriptionCommandSender;
import io.camunda.zeebe.logstreams.log.LogStreamRecordWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubscriptionCommandSenderFactory {

  final LogStreamRecordWriter streamWriter;
  private final ExecutorService subscriptionHandlerExecutor = Executors.newSingleThreadExecutor();
  private final int partitionId;

  public SubscriptionCommandSenderFactory(
      final LogStreamRecordWriter streamWriter, final int partitionId) {
    this.streamWriter = streamWriter;
    this.partitionId = partitionId;
  }

  public SubscriptionCommandSender createSender() {
    final SubscriptionCommandMessageHandler handler =
        new SubscriptionCommandMessageHandler(
            subscriptionHandlerExecutor::submit, this::getStreamWriter);
    return new SubscriptionCommandSender(
        partitionId, new PartitionCommandSenderImpl(handler, partitionId));
  }

  private LogStreamRecordWriter getStreamWriter(final int receivedPartitionId) {
    if (receivedPartitionId != partitionId) {
      throw new RuntimeException(
          String.format(
              "Expected receivedPartitionId to be %d, but was %d",
              partitionId, receivedPartitionId));
    }
    return streamWriter;
  }
}
