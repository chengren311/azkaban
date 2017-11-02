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

import azkaban.executor.ExecutableFlow;
import azkaban.executor.ExecutorManager;
import azkaban.flow.Flow;
import azkaban.flow.FlowUtils;
import azkaban.flowtrigger.database.FlowTriggerLoader;
import azkaban.project.Project;
import azkaban.project.ProjectManager;
import com.google.common.base.Preconditions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TriggerProcessor {

  private static final Logger logger = LoggerFactory.getLogger(TriggerProcessor.class);
  private static final int THREAD_POOL_SIZE = 8;
  private final ProjectManager projectManager;
  private final ExecutorManager executorManager;
  private final ExecutorService executorService;
  private final FlowTriggerLoader dependencyLoader;

  @Inject
  public TriggerProcessor(final ProjectManager projectManager,
      final ExecutorManager executorManager, final FlowTriggerLoader dependencyLoader) {
    Preconditions.checkNotNull(projectManager);
    Preconditions.checkNotNull(executorManager);
    this.projectManager = projectManager;
    this.executorManager = executorManager;
    this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    this.dependencyLoader = dependencyLoader;
  }

  private void executeFlowAndUpdateExecID(final TriggerInstance triggerInst) {
    try {
      final Project project = FlowUtils.getProject(this.projectManager, triggerInst
          .getFlowTrigger().getProjectId());
      final Flow flow = FlowUtils.getFlow(project, triggerInst.getFlowTrigger().getFlowId());
      final ExecutableFlow executableFlow = FlowUtils.createExecutableFlow(project, flow);
      this.executorManager.submitExecutableFlow(executableFlow, triggerInst.getSubmitUser());

      triggerInst.setFlowExecId(executableFlow.getExecutionId());
      this.executorService.submit(() -> this.dependencyLoader.updateAssociatedFlowExecId
          (triggerInst));
    } catch (final Exception ex) {
      logger.error(ex.getMessage()); //todo chengren311: should we swallow the exception or
      // notify user
    }
  }

  private void processSucceed(final TriggerInstance triggerInst) {
    logger.debug("process succeed for " + triggerInst);
    // email and trigger a new flow
    this.executorService
        .submit(() -> executeFlowAndUpdateExecID(triggerInst));

  }

  private void processKilled(final TriggerInstance triggerInst) {
    logger.debug("process killed for " + triggerInst);
    // email
  }

  private void processTimeout(final TriggerInstance triggerInst) {
    logger.debug("process timeout for " + triggerInst);
    // email
  }

  private void processNewInstance(final TriggerInstance triggerInst) {
    logger.debug("process new instance for " + triggerInst);
    this.executorService
        .submit(() -> this.dependencyLoader.uploadTriggerInstance(triggerInst));
  }

  public void processStatusUpdate(final TriggerInstance updatedTriggerInst) {
    switch (updatedTriggerInst.getStatus()) {
      case RUNNING:
        processNewInstance(updatedTriggerInst);
        break;
      case SUCCEEDED:
        processSucceed(updatedTriggerInst);
        break;
      case KILLED:
        processKilled(updatedTriggerInst);
        break;
      case TIMEOUT:
        processTimeout(updatedTriggerInst);
        break;
      default:
        break;
    }
  }
}
