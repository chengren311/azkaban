/*
 * Copyright 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package azkaban.flowtrigger;

import java.util.Date;

public class DependencyInstance {

  private final Date startTime;
  private final TriggerInstance triggerInstance;
  // dependency name as defined by user
  private final String depName;
  private DependencyInstanceContext context;
  private volatile Date endTime;
  private volatile Status status;
  private boolean timeoutKilling;

  //todo chengren311: use builder pattern to construct the object
  public DependencyInstance(final String depName, final DependencyInstanceContext context,
      final TriggerInstance triggerInst) {
    this.status = Status.RUNNING;
    this.depName = depName;
    this.startTime = new Date();
    this.endTime = null;
    this.context = context;
    this.timeoutKilling = false;
    this.triggerInstance = triggerInst;
  }

  public DependencyInstance(final String depName, final Date startTime, final Date endTime,
      final Status status, final boolean timeoutKilling, final TriggerInstance triggerInst) {
    this.depName = depName;
    this.timeoutKilling = timeoutKilling;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.context = null;
    this.timeoutKilling = false;
    this.triggerInstance = triggerInst;
  }

  public TriggerInstance getTriggerInstance() {
    return this.triggerInstance;
  }

  public Date getStartTime() {
    return this.startTime;
  }

  public Date getEndTime() {
    return this.endTime;
  }

  public String getDepName() {
    return this.depName;
  }

  public boolean isTimeoutKilling() {
    return this.timeoutKilling;
  }

  public void setTimeoutKilling(final boolean timeoutKilling) {
    this.timeoutKilling = timeoutKilling;
  }

  public void updateEndTime(final Date endTime) {
    this.endTime = endTime;
  }

  public void updateStatus(final Status status) {
    this.status = status;
  }

  public DependencyInstanceContext getContext() {
    return this.context;
  }

  public void setContext(DependencyInstanceContext context) {
    this.context = context;
  }

  public Status getStatus() {
    return this.status;
  }

}