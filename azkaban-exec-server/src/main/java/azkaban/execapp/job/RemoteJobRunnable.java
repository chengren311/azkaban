/*
 * Copyright 2019 LinkedIn Corp.
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

package azkaban.execapp.job;

import azkaban.jobExecutor.ProcessJob;
import azkaban.storage.HdfsStorage;

class RemoteJobRunnable {

  private HdfsStorage storage;
  private ProcessJob azkabanJob;

  RemoteJobRunnable(final HdfsStorage hdfsStorage, final ProcessJob azkabanJob) {
    // to be implemented
    this.storage.get();
  }

  void run() {

  }


}
