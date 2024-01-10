/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */

package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.engine.processing.streamprocessor.JobStreamer;
import io.camunda.zeebe.protocol.impl.record.RecordMetadata;
import io.camunda.zeebe.protocol.impl.stream.job.ActivatedJob;
import io.camunda.zeebe.protocol.impl.stream.job.JobActivationProperties;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InMemoryJobStreamer implements JobStreamer {
  private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryJobStreamer.class);

  private final ConcurrentMap<DirectBuffer, InMemoryJobStream> streams = new ConcurrentHashMap<>();
  private final CommandWriter yieldWriter;

  InMemoryJobStreamer(final CommandWriter yieldWriter) {
    this.yieldWriter = yieldWriter;
  }

  @Override
  public Optional<JobStream> streamFor(
      final DirectBuffer jobType, final Predicate<JobActivationProperties> filter) {
    return Optional.ofNullable(streams.get(jobType))
        .flatMap(s -> filter.test(s.properties()) ? Optional.of(s) : Optional.empty());
  }

  void addStream(
      final DirectBuffer jobType,
      final JobActivationProperties properties,
      final JobConsumer consumer) {
    streams.compute(
        jobType,
        (ignored, s) -> {
          final var stream =
              s == null ? new InMemoryJobStream(properties, new CopyOnWriteArraySet<>()) : s;
          stream.consumers.add(consumer);
          return stream;
        });
  }

  void removeStream(final DirectBuffer jobType, final JobConsumer consumer) {
    streams.compute(
        jobType,
        (ignored, stream) -> {
          if (stream == null) {
            return null;
          }

          stream.consumers.remove(consumer);
          if (stream.consumers.isEmpty()) {
            return null;
          }

          return stream;
        });
  }

  private void yieldJob(final ActivatedJob job) {
    final var metadata =
        new RecordMetadata()
            .intent(JobIntent.YIELD)
            .recordType(RecordType.COMMAND)
            .valueType(ValueType.JOB);
    yieldWriter.writeCommandWithKey(job.jobKey(), job.jobRecord(), metadata);
  }

  interface JobConsumer {
    CompletionStage<PushStatus> consumeJob(final ActivatedJob job);
  }

  enum PushStatus {
    PUSHED,
    BLOCKED;
  }

  private final class InMemoryJobStream implements JobStream {
    private final JobActivationProperties properties;
    private final Set<JobConsumer> consumers;

    InMemoryJobStream(final JobActivationProperties properties, final Set<JobConsumer> consumers) {
      this.properties = properties;
      this.consumers = consumers;
    }

    @Override
    public JobActivationProperties properties() {
      return properties;
    }

    @Override
    public void push(final ActivatedJob payload) {
      final var shuffled = new LinkedList<>(consumers);
      Collections.shuffle(shuffled);
      push(shuffled, payload);
    }

    private void push(final Queue<JobConsumer> consumers, final ActivatedJob job) {
      final var consumer = consumers.poll();
      if (consumer == null) {
        LOGGER.debug("Failed to push job to clients, exhausted all known clients");
        yieldJob(job);
        return;
      }

      try {
        consumer
            .consumeJob(job)
            .whenCompleteAsync(
                (status, error) -> {
                  if (error != null) {
                    onPushError(consumers, job, error);
                    return;
                  }

                  if (status == PushStatus.BLOCKED) {
                    LOGGER.trace(
                        "Underlying stream or client is blocked, retrying with next consumer");
                    CompletableFuture.runAsync(() -> push(consumers, job));
                  }
                });
      } catch (final Exception e) {
        onPushError(consumers, job, e);
      }
    }

    private void onPushError(
        final Queue<JobConsumer> consumers, final ActivatedJob job, final Throwable error) {
      LOGGER.debug("Failed to push job to client, retrying with next consumer", error);
      CompletableFuture.runAsync(() -> push(consumers, job));
    }
  }
}
