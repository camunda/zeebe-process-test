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
import io.camunda.client.spring.event.CamundaClientClosingSpringEvent;
import io.camunda.client.spring.event.CamundaClientCreatedSpringEvent;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.JsonMapper;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.spring.client.event.ZeebeClientClosingEvent;
import io.camunda.zeebe.spring.client.event.ZeebeClientCreatedEvent;
import io.camunda.zeebe.spring.test.proxy.CamundaClientProxy;
import io.camunda.zeebe.spring.test.proxy.ZeebeClientProxy;
import io.camunda.zeebe.spring.test.proxy.ZeebeTestEngineProxy;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestContext;

/**
 * Base class for the two different ZeebeTestExecutionListener classes provided for in-memory vs
 * Testcontainer tests
 */
public class AbstractZeebeTestExecutionListener {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ZeebeClient zeebeClient;
  private CamundaClient camundaClient;

  /** Registers the ZeebeEngine for test case in relevant places and creates the ZeebeClient */
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
    zeebeClient = createZeebeClient(testContext, zeebeEngine);

    testContext
        .getApplicationContext()
        .getBean(ZeebeClientProxy.class)
        .swapZeebeClient(zeebeClient);
    testContext
        .getApplicationContext()
        .publishEvent(new ZeebeClientCreatedEvent(this, zeebeClient));

    // Camunda Client
    camundaClient = createCamundaClient(testContext, zeebeEngine);

    testContext
        .getApplicationContext()
        .getBean(CamundaClientProxy.class)
        .swapZeebeClient(camundaClient);

    // Register the test class as a synthetic bean definition so that annotation processors
    // (e.g. DeploymentAnnotationProcessor, JobWorkerAnnotationProcessor) discover it when
    // iterating getBeanDefinitionNames() during onStart(). Test classes are not normally
    // registered as Spring bean definitions.
    registerTestClassAsBeanDefinition(testContext);

    testContext
        .getApplicationContext()
        .publishEvent(new CamundaClientCreatedSpringEvent(this, camundaClient) {});

    LOGGER.info("...deployments and workers started.");
  }

  public ZeebeClient createZeebeClient(
      final TestContext testContext, final ZeebeTestEngine zeebeEngine) {
    final ZeebeClientBuilder builder =
        ZeebeClient.newClientBuilder()
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

  public CamundaClient createCamundaClient(
      final TestContext testContext, final ZeebeTestEngine zeebeEngine) {
    final CamundaClientBuilder builder =
        CamundaClient.newClientBuilder()
            .preferRestOverGrpc(false)
            .grpcAddress(URI.create("http://" + zeebeEngine.getGatewayAddress()));
    if (testContext
            .getApplicationContext()
            .getBeanNamesForType(io.camunda.client.api.JsonMapper.class)
            .length
        > 0) {
      final io.camunda.client.api.JsonMapper jsonMapper =
          testContext.getApplicationContext().getBean(io.camunda.client.api.JsonMapper.class);
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
        .publishEvent(new ZeebeClientClosingEvent(this, zeebeClient));
    testContext.getApplicationContext().getBean(ZeebeClientProxy.class).removeZeebeClient();
    zeebeClient.close();

    testContext
        .getApplicationContext()
        .publishEvent(new CamundaClientClosingSpringEvent(this, camundaClient));
    testContext.getApplicationContext().getBean(CamundaClientProxy.class).removeCamundaClient();
    camundaClient.close();

    unregisterTestClassBeanDefinition(testContext);

    testContext.getApplicationContext().getBean(ZeebeTestEngineProxy.class).removeZeebeEngine();
  }

  /**
   * Registers the test class as a synthetic bean definition in the {@link
   * DefaultListableBeanFactory} so that {@link
   * io.camunda.client.spring.annotation.processor.AbstractCamundaAnnotationProcessor#onStart}
   * discovers it when iterating {@code getBeanDefinitionNames()}. The bean definition uses an
   * instance supplier that returns the existing test instance managed by JUnit, avoiding any Spring
   * lifecycle interference.
   */
  private void registerTestClassAsBeanDefinition(final TestContext testContext) {
    try {
      if (testContext.getApplicationContext()
          instanceof ConfigurableApplicationContext configurableContext) {
        final ConfigurableListableBeanFactory beanFactory = configurableContext.getBeanFactory();
        if (beanFactory instanceof DefaultListableBeanFactory defaultBeanFactory) {
          final Class<?> testClass = testContext.getTestClass();
          final String beanName = testClass.getName();
          if (!defaultBeanFactory.containsBeanDefinition(beanName)) {
            final GenericBeanDefinition bd = new GenericBeanDefinition();
            bd.setBeanClass(testClass);
            bd.setInstanceSupplier(testContext::getTestInstance);
            defaultBeanFactory.registerBeanDefinition(beanName, bd);
            LOGGER.debug("Registered test class {} as bean definition", beanName);
          }
        }
      }
    } catch (final Exception e) {
      LOGGER.debug("Could not register test class as bean definition: {}", e.getMessage());
    }
  }

  /**
   * Removes the synthetic bean definition for the test class that was registered by {@link
   * #registerTestClassAsBeanDefinition}. Called during cleanup to avoid stale definitions across
   * test methods.
   */
  private void unregisterTestClassBeanDefinition(final TestContext testContext) {
    try {
      if (testContext.getApplicationContext()
          instanceof ConfigurableApplicationContext configurableContext) {
        final ConfigurableListableBeanFactory beanFactory = configurableContext.getBeanFactory();
        if (beanFactory instanceof DefaultListableBeanFactory defaultBeanFactory) {
          final String beanName = testContext.getTestClass().getName();
          if (defaultBeanFactory.containsBeanDefinition(beanName)) {
            defaultBeanFactory.removeBeanDefinition(beanName);
            LOGGER.debug("Unregistered test class bean definition {}", beanName);
          }
        }
      }
    } catch (final Exception e) {
      LOGGER.debug("Could not unregister test class bean definition: {}", e.getMessage());
    }
  }
}
