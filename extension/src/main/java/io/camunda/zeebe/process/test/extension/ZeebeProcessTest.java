/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This annotation can be used to test BPMN processes. It will run an in memory Zeebe engine. To use
 * this annotation Java 17 or higher is required.
 *
 * <p>Annotating test classes with this annotation will do a couple of things:
 *
 * <ul>
 *   <li>It will manage the lifecycle of the embedded test engine
 *   <li>It will create a client which can be used to interact with the engine.
 *   <li>It will (optionally) inject 3 fields in your test class:
 *       <ul>
 *         <li>ZeebeTestEngine - This is the engine that will run your process. It will provide some
 *             basic functionality to help you write your tests, such as waiting for an idle state
 *             and increasing the time.
 *         <li>ZeebeClient - This is the client that allows you to send commands to the engine, such
 *             as starting a process instance. The interface of this client is identical to the
 *             interface you use to connect to a real Zeebe engine.
 *         <li>RecordStream - This gives you access to all the records that are processed by the
 *             engine. Assertions use the records for verifying expectations. This grants you the
 *             freedom to create your own assertions.
 *       </ul>
 * </ul>
 *
 * <p>Lifecycle:
 *
 * <ul>
 *   <li>Before each test create a new Zeebe test engine
 *   <li>Run the test
 *   <li>After each test stop the Zeebe test engine
 * </ul>
 *
 * @since Java 17
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(ZeebeProcessTestExtension.class)
public @interface ZeebeProcessTest {}
