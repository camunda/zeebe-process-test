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
package io.camunda.zeebe.process.test.extension.testcontainer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This annotation can be used to test BPMN processes. It will make use of testcontainers to run an
 * in memory engine. To use this annotation Java 8 or higher is required. Docker also needs to be
 * running.
 *
 * <p>Annotating test classes with this annotation will do a couple of things:
 *
 * <ul>
 *   <li>It will manage the lifecycle of the testcontainer and Zeebe test engine.
 *   <li>It will create a client which can be used to interact with the engine.
 *   <li>It will (optionally) inject 3 fields in your test class:
 *       <ul>
 *         <li>ZeebeTestEngine - This is the engine that will run your process. It will provide some
 *             basic functionality to help you write your tests, such as waiting for an idle state
 *             and increasing the time.
 *         <li>CamundaClient - This is the client that allows you to send commands to the engine,
 *             such as starting a process instance. The interface of this client is identical to the
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
 *   <li>Before the test suite start the testcontainer
 *   <li>Before each test stop the current Zeebe test engine (if applicable) and create a new Zeebe
 *       test engine for the next test
 *   <li>Run the test
 *   <li>After the test suite stop the testcontainer
 * </ul>
 *
 * @since Java 8
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(ZeebeProcessTestExtension.class)
public @interface ZeebeProcessTest {}
