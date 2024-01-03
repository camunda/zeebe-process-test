/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */

package io.camunda.zeebe.process.test.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.gateway.protocol.GatewayGrpc.GatewayImplBase;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GrpcToLogStreamGatewayTest {

  static final List<String> UNSUPPORTED_METHODS = List.of();

  static final List<String> IGNORED_METHODS =
      List.of(
          "bindService",
          "equals",
          "getClass",
          "hashCode",
          "notify",
          "notifyAll",
          "toString",
          "wait");

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideMethods")
  void testImplementsGatewayEndpoint(final String methodName) {
    final Optional<Method> optionalMethod =
        Arrays.stream(GrpcToLogStreamGateway.class.getDeclaredMethods())
            .filter(m -> m.getName().equals(methodName))
            .findAny();

    assertThat(optionalMethod)
        .describedAs(
            """
            Expected method %s to be implemented. \
            When this test fails, it's likely a new RPC that ZPT should support. \
            Please check whether it should be supported by ZPT. \
            If it should be suported, add a test case to EngineClientTest.java""",
            methodName)
        .isPresent();
  }

  static Stream<Arguments> provideMethods() {
    return Arrays.stream(GatewayImplBase.class.getMethods())
        .map(Method::getName)
        .filter(name -> !IGNORED_METHODS.contains(name))
        .filter(name -> !UNSUPPORTED_METHODS.contains(name))
        .map(Arguments::of);
  }
}
