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
package io.camunda.zeebe.process.test.inspections.model;

/**
 * Helper object to gain access to {@link
 * io.camunda.zeebe.process.test.assertions.ProcessInstanceAssert}. All this object does is wrap a
 * process instance key. This object is required to use {@link
 * io.camunda.zeebe.process.test.assertions.BpmnAssert#assertThat(InspectedProcessInstance)}.
 *
 * <p>The helper object enabled asserting process instances which were not started with a command
 * send by the client (e.g. by a timer or a call activity).
 */
public class InspectedProcessInstance {

  private final long processInstanceKey;

  public InspectedProcessInstance(final long processInstanceKey) {
    this.processInstanceKey = processInstanceKey;
  }

  /**
   * Get the process instance key.
   *
   * @return the wrapped process instance key
   */
  public long getProcessInstanceKey() {
    return processInstanceKey;
  }
}
