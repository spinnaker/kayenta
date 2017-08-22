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

package com.netflix.kayenta.prometheus.orca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.metrics.SynchronousQueryProcessor;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.CredentialsHelper;
import com.netflix.kayenta.prometheus.canary.PrometheusCanaryScope;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import com.netflix.kayenta.storage.StorageServiceRepository;
import com.netflix.spinnaker.orca.ExecutionStatus;
import com.netflix.spinnaker.orca.RetryableTask;
import com.netflix.spinnaker.orca.TaskResult;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class PrometheusFetchTask implements RetryableTask {

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  AccountCredentialsRepository accountCredentialsRepository;

  @Autowired
  StorageServiceRepository storageServiceRepository;

  @Autowired
  SynchronousQueryProcessor synchronousQueryProcessor;

  @Override
  public long getBackoffPeriod() {
    // TODO(duftler): Externalize this configuration.
    // TODO(ewiseblatt): Copied from stackdriver.
    return Duration.ofSeconds(2).toMillis();
  }

  @Override
  public long getTimeout() {
    // TODO(duftler): Externalize this configuration.
    // TODO(ewiseblatt): Copied from stackdriver.
    return Duration.ofMinutes(2).toMillis();
  }

  @Override
  public TaskResult execute(Stage stage) {
    Map<String, Object> context = stage.getContext();
    String metricsAccountName = (String)context.get("metricsAccountName");
    String storageAccountName = (String)context.get("storageAccountName");
    String canaryConfigId = (String)context.get("canaryConfigId");
    PrometheusCanaryScope prometheusCanaryScope =
      objectMapper.convertValue(stage.getContext().get("prometheusCanaryScope"), PrometheusCanaryScope.class);
    String resolvedMetricsAccountName = CredentialsHelper.resolveAccountByNameOrType(metricsAccountName,
                                                                                     AccountCredentials.Type.METRICS_STORE,
                                                                                     accountCredentialsRepository);
    String resolvedStorageAccountName = CredentialsHelper.resolveAccountByNameOrType(storageAccountName,
                                                                                     AccountCredentials.Type.OBJECT_STORE,
                                                                                     accountCredentialsRepository);
    StorageService storageService =
      storageServiceRepository
        .getOne(resolvedStorageAccountName)
        .orElseThrow(() -> new IllegalArgumentException("No storage service was configured; unable to load canary config."));

    try {
      CanaryConfig canaryConfig =
        storageService.loadObject(resolvedStorageAccountName, ObjectType.CANARY_CONFIG, canaryConfigId.toLowerCase());


      Instant startTimeInstant = Instant.parse(prometheusCanaryScope.getIntervalStartTimeIso());
      long startTimeMillis = startTimeInstant.toEpochMilli();
      Instant endTimeInstant = Instant.parse(prometheusCanaryScope.getIntervalEndTimeIso());
      long endTimeMillis = endTimeInstant.toEpochMilli();
      /*
      prometheusCanaryScope.setStart(startTimeMillis + "");
      prometheusCanaryScope.setEnd(endTimeMillis + "");
      */
      prometheusCanaryScope.setStart(prometheusCanaryScope.getIntervalStartTimeIso());
      prometheusCanaryScope.setEnd(prometheusCanaryScope.getIntervalEndTimeIso());

      List<String> metricSetListIds = synchronousQueryProcessor.processQuery(resolvedMetricsAccountName,
                                                                             resolvedStorageAccountName,
                                                                             canaryConfig.getMetrics(),
                                                                             prometheusCanaryScope);

      Map outputs = Collections.singletonMap("metricSetListIds", metricSetListIds);

      return new TaskResult(ExecutionStatus.SUCCEEDED, outputs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
