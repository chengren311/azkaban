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

import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * Represents status for trigger/dependency
 */
public enum Status {
  RUNNING, // dependency instance is running
  SUCCEEDED, // dependency instance succeeds
  TIMEOUT, // dependency instance exceeds the max wait time
  KILLED, // dependency instance is killed by user
  KILLING, // dependency instance is being killed by timeout or user
  FAILED; // dependency instance fails due to internal/external failure

  public static boolean isDone(final Status status) {
    final Set<Status> terminalStatus = ImmutableSet.of(SUCCEEDED, TIMEOUT, KILLED, FAILED);
    return terminalStatus.contains(status);
  }
}
