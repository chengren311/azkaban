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

package azkaban.flowtrigger.quartz;

import static java.util.Objects.requireNonNull;

import azkaban.flow.Flow;
import azkaban.flow.FlowUtils;
import azkaban.project.FlowConfigID;
import azkaban.project.FlowLoaderUtils;
import azkaban.project.FlowTrigger;
import azkaban.project.Project;
import azkaban.project.ProjectLoader;
import azkaban.scheduler.QuartzJobDescription;
import azkaban.scheduler.QuartzScheduler;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FlowTriggerScheduler {

  private static final Logger logger = LoggerFactory.getLogger(FlowTriggerScheduler.class);
  private final ProjectLoader projectLoader;
  private final QuartzScheduler scheduler;

  @Inject
  public FlowTriggerScheduler(final ProjectLoader projectLoader, final QuartzScheduler scheduler) {
    this.projectLoader = requireNonNull(projectLoader);
    this.scheduler = requireNonNull(scheduler);
  }

  /**
   * Schedule all possible flows in a project
   */
  public void scheduleAll(final Project project, final String submitUser)
      throws SchedulerException {
    //todo chengren311: schedule on uploading via CRT

    for (final Flow flow : project.getFlows()) {
      final String flowFileName = flow.getId() + ".flow";
      final int latestFlowVersion = this.projectLoader
          .getLatestFlowVersion(flow.getProjectId(), flow
              .getVersion(), flowFileName);
      if (latestFlowVersion > 0) {
        final File tempDir = Files.createTempDir();
        final File flowFile;
        try {
          flowFile = this.projectLoader
              .getUploadedFlowFile(project.getId(), project.getVersion(),
                  flowFileName, latestFlowVersion, tempDir);

          final FlowTrigger flowTrigger = FlowLoaderUtils.getFlowTriggerFromYamlFile(flowFile);

          if (flowTrigger != null) {
            final FlowConfigID flowConfigID = new FlowConfigID(project.getId(),
                project.getVersion(),
                flow.getId(), latestFlowVersion);
            final String projectJson = FlowUtils.toJson(project);
            final Map<String, Object> contextMap = ImmutableMap
                .of(FlowTriggerQuartzJob.SUBMIT_USER, submitUser,
                    FlowTriggerQuartzJob.FLOW_TRIGGER, flowTrigger, FlowConfigID.class.getName(),
                    flowConfigID, FlowTriggerQuartzJob.PROJECT, projectJson);
            logger.info("scheduling flow " + flow.getProjectId() + "." + flow.getId());
            this.scheduler
                .registerJob(flowTrigger.getSchedule().getCronExpression(), new QuartzJobDescription
                    (FlowTriggerQuartzJob.class, generateGroupName(flow), contextMap));
          }
        } catch (final IOException ex) {
          logger.error("error in getting flow file", ex);
        } finally {
          FlowLoaderUtils.cleanUpDir(tempDir);
        }
      }
    }
  }

  public List<ScheduledFlowTrigger> getScheduledFlowTriggerJobs() {
    final Scheduler quartzScheduler = this.scheduler.getScheduler();
    try {
      final List<String> groupNames = quartzScheduler.getJobGroupNames();

      final List<ScheduledFlowTrigger> flowTriggerJobDetails = new ArrayList<>();
      for (final String groupName : groupNames) {
        final JobKey jobKey = new JobKey(QuartzScheduler.DEFAULT_JOB_NAME, groupName);
        ScheduledFlowTrigger scheduledFlowTrigger = null;
        try {
          final JobDetail job = quartzScheduler.getJobDetail(jobKey);
          final JobDataMap jobDataMap = job.getJobDataMap();
          final FlowConfigID flowConfigID = (FlowConfigID) jobDataMap
              .get(FlowTriggerQuartzJob.FLOW_CONFIG_ID);
          final String projectJson = jobDataMap.getString(FlowTriggerQuartzJob.PROJECT);
          final Project project = FlowUtils.toProject(projectJson);
          final FlowTrigger flowTrigger = (FlowTrigger) jobDataMap
              .get(FlowTriggerQuartzJob.FLOW_TRIGGER);
          final String submitUser = jobDataMap.getString(FlowTriggerQuartzJob.SUBMIT_USER);
          final List<? extends Trigger> quartzTriggers = quartzScheduler.getTriggersOfJob(jobKey);
          scheduledFlowTrigger = new ScheduledFlowTrigger(
              project.getName(),
              flowConfigID.getFlowId(), flowTrigger, submitUser, quartzTriggers.isEmpty() ? null
              : quartzTriggers.get(0));
        } catch (final Exception ex) {
          logger
              .error(String.format("unable to get flow trigger by job key %s", jobKey, ex));
          scheduledFlowTrigger = null;
        }

        flowTriggerJobDetails.add(scheduledFlowTrigger);
      }
      return flowTriggerJobDetails;
    } catch (final SchedulerException ex) {
      logger.error("unable to get scheduled flow triggers", ex);
      return new ArrayList<>();
    }
  }

  /**
   * Unschedule all possible flows in a project
   */
  public void unscheduleAll(final Project project) throws SchedulerException {
    for (final Flow flow : project.getFlows()) {
      logger.info("unscheduling flow" + flow.getProjectId() + "." + flow.getId() + "if it has "
          + " schedule");
      this.scheduler.unregisterJob(generateGroupName(flow));
    }
  }

  private String generateGroupName(final Flow flow) {
    return String.valueOf(flow.getProjectId()) + "." + flow.getId();
  }

  public void start() {
    this.scheduler.start();
  }

  public void shutdown() {
    this.scheduler.shutdown();
  }

  public class ScheduledFlowTrigger {

    private final String projectName;
    private final String flowId;
    private final FlowTrigger flowTrigger;
    private final Trigger quartzTrigger;
    private final String submitUser;

    public ScheduledFlowTrigger(final String projectName, final String flowId,
        final FlowTrigger flowTrigger, final String submitUser,
        final Trigger quartzTrigger) {
      this.projectName = projectName;
      this.flowId = flowId;
      this.flowTrigger = flowTrigger;
      this.submitUser = submitUser;
      this.quartzTrigger = quartzTrigger;
    }

    public String getProjectName() {
      return this.projectName;
    }

    public String getFlowId() {
      return this.flowId;
    }

    public FlowTrigger getFlowTrigger() {
      return this.flowTrigger;
    }

    public Trigger getQuartzTrigger() {
      return this.quartzTrigger;
    }

    public String getSubmitUser() {
      return this.submitUser;
    }
  }
}