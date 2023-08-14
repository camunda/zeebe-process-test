/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */

package io.camunda.zeebe.process.test.engine;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperConfig extends io.camunda.zeebe.process.test.api.ObjectMapperConfig {

  private ObjectMapperConfig(final ObjectMapper mapper) {
    super(mapper);
  }

  public static void initialize(final ObjectMapper objectMapper) {
    new ObjectMapperConfig(objectMapper);
  }
}
