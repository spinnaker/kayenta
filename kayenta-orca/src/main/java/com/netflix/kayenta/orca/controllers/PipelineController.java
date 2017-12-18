/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.kayenta.orca.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.StatusChangeEvent;
import com.netflix.spinnaker.kork.eureka.RemoteStatusChangedEvent;
import com.netflix.spinnaker.orca.log.ExecutionLogEntry;
import com.netflix.spinnaker.orca.log.ExecutionLogRepository;
import com.netflix.spinnaker.orca.pipeline.ExecutionLauncher;
import com.netflix.spinnaker.orca.pipeline.model.Execution;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.STARTING;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;

@RestController
@RequestMapping("/pipelines")
@Slf4j
public class PipelineController {

  private final ExecutionLauncher executionLauncher;
  private final ExecutionRepository executionRepository;
  private final ExecutionLogRepository executionLogRepository;
  private final ObjectMapper kayentaObjectMapper;
  private final ConfigurableApplicationContext context;
  private final HealthEndpoint healthEndpoint;
  private final ScheduledAnnotationBeanPostProcessor postProcessor;

  @Autowired
  public PipelineController(ExecutionLauncher executionLauncher, ExecutionRepository executionRepository, ExecutionLogRepository executionLogRepository, ObjectMapper kayentaObjectMapper, ConfigurableApplicationContext context, HealthEndpoint healthEndpoint, ScheduledAnnotationBeanPostProcessor postProcessor) {
    this.executionLauncher = executionLauncher;
    this.executionRepository = executionRepository;
    this.executionLogRepository = executionLogRepository;
    this.kayentaObjectMapper = kayentaObjectMapper;
    this.context = context;
    this.healthEndpoint = healthEndpoint;
    this.postProcessor = postProcessor;
  }

  // TODO(duftler): Expose /inservice and /outofservice endpoints.
  @Scheduled(initialDelay = 10000, fixedDelay = 5000)
  void startOrcaQueueProcessing() {
    Health health = healthEndpoint.invoke();

    if (health.getStatus() == Status.UP) {
      context.publishEvent(new RemoteStatusChangedEvent(new StatusChangeEvent(STARTING, UP)));

      // Cancel the scheduled task.
      postProcessor.postProcessBeforeDestruction(this, null);
    } else {
      log.info("Health indicators are still reporting DOWN; not starting orca queue processing yet: {}", health);
    }
  }

  @ApiOperation(value = "Initiate a pipeline execution")
  @RequestMapping(value = "/start", method = RequestMethod.POST)
  String start(@RequestBody Map map) throws Exception {
    return startPipeline(map);
  }

  @ApiOperation(value = "Retrieve a pipeline execution")
  @RequestMapping(value = "/{executionId}", method = RequestMethod.GET)
  Execution getPipeline(@PathVariable String executionId) {
    return executionRepository.retrieve(Execution.ExecutionType.PIPELINE, executionId);
  }

  @ApiOperation(value = "Retrieve pipeline execution logs")
  @RequestMapping(value = "/{executionId}/logs", method = RequestMethod.GET)
  List<ExecutionLogEntry> logs(@PathVariable String executionId) {
    if (executionLogRepository == null) {
      throw new FeatureNotEnabledException("Execution log not enabled");
    }
    return executionLogRepository.getAllByExecutionId(executionId);
  }

  @ApiOperation(value = "Cancel a pipeline execution")
  @RequestMapping(value = "/{executionId}/cancel", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.ACCEPTED)
  void cancel(@PathVariable String executionId) {
    log.info("Cancelling pipeline execution {}...", executionId);

    executionRepository.cancel(executionId);
  }

  private static class FeatureNotEnabledException extends RuntimeException {
    public FeatureNotEnabledException(String message) {
      super(message);
    }
  }

  private String startPipeline(Map config) throws Exception {
    String json = kayentaObjectMapper.writeValueAsString(config);

    log.info("Requested pipeline: {}", json);

    Execution pipeline = executionLauncher.start(Execution.ExecutionType.PIPELINE, json);

    return pipeline.getId();
  }
}
