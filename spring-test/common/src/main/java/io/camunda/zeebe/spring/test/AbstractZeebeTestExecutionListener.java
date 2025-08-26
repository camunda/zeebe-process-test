/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.spring.test;

import io.camunda.client.CamundaClient;
import io.camunda.client.CamundaClientBuilder;
import io.camunda.client.api.JsonMapper;
import io.camunda.spring.client.event.CamundaClientClosingEvent;
import io.camunda.spring.client.event.CamundaClientCreatedEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.spring.test.proxy.CamundaClientProxy;
import io.camunda.zeebe.spring.test.proxy.ZeebeTestEngineProxy;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;

/**
 * Base class for the two different ZeebeTestExecutionListener classes provided for in-memory vs
 * Testcontainer tests
 */
public class AbstractZeebeTestExecutionListener {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CamundaClient camundaClient;

  /** Registers the ZeebeEngine for test case in relevant places and creates the CamundaClient */
  public void setupWithZeebeEngine(
      final TestContext testContext, final ZeebeTestEngine zeebeEngine) {

    testContext
        .getApplicationContext()
        .getBean(ZeebeTestEngineProxy.class)
        .swapZeebeEngine(zeebeEngine);

    BpmnAssert.initRecordStream(RecordStream.of(zeebeEngine.getRecordStreamSource()));

    ZeebeTestThreadSupport.setEngineForCurrentThread(zeebeEngine);

    LOGGER.info("Test engine setup. Now starting deployments and workers...");

    // Not using zeebeEngine.createClient(); to be able to set JsonMapper
    camundaClient = createClient(testContext, zeebeEngine);

    testContext
        .getApplicationContext()
        .getBean(CamundaClientProxy.class)
        .swapZeebeClient(camundaClient);
    testContext
        .getApplicationContext()
        .publishEvent(new CamundaClientCreatedEvent(this, camundaClient));

    LOGGER.info("...deployments and workers started.");
  }

  public CamundaClient createClient(
      final TestContext testContext, final ZeebeTestEngine zeebeEngine) {
    // Maybe use more of the normal config properties
    // (https://github.com/camunda-community-hub/spring-zeebe/blob/11966be454cc76f3966fb2c0e4114a35487946fc/client/spring-zeebe-starter/src/main/java/io/camunda/zeebe/spring/client/config/ZeebeClientStarterAutoConfiguration.java#L30)?
    final CamundaClientBuilder builder =
        CamundaClient.newClientBuilder()
            .preferRestOverGrpc(false)
            .gatewayAddress(zeebeEngine.getGatewayAddress())
            .usePlaintext();
    if (testContext.getApplicationContext().getBeanNamesForType(JsonMapper.class).length > 0) {
      final JsonMapper jsonMapper = testContext.getApplicationContext().getBean(JsonMapper.class);
      builder.withJsonMapper(jsonMapper);
    }
    builder.applyEnvironmentVariableOverrides(false);
    return builder.build();
  }

  public void cleanup(final TestContext testContext, final ZeebeTestEngine zeebeEngine) {

    if (testContext.getTestException() != null) {
      LOGGER.warn(
          "Test failure on '"
              + testContext.getTestMethod()
              + "'. Tracing workflow engine internals on INFO for debugging purposes:");
      final RecordStream recordStream = RecordStream.of(zeebeEngine.getRecordStreamSource());
      recordStream.print(true);

      if (recordStream.incidentRecords().iterator().hasNext()) {
        LOGGER.warn(
            "There were incidents in Zeebe during '"
                + testContext.getTestMethod()
                + "', maybe they caused some unexpected behavior for you? Please check below:");
        recordStream
            .incidentRecords()
            .forEach(
                record -> {
                  LOGGER.warn(". " + record.getValue());
                });
      }
    }

    BpmnAssert.resetRecordStream();
    ZeebeTestThreadSupport.cleanupEngineForCurrentThread();

    testContext
        .getApplicationContext()
        .publishEvent(new CamundaClientClosingEvent(this, camundaClient));
    testContext.getApplicationContext().getBean(CamundaClientProxy.class).removeZeebeClient();
    camundaClient.close();
    testContext.getApplicationContext().getBean(ZeebeTestEngineProxy.class).removeZeebeEngine();
  }
}
