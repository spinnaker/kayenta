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

package com.netflix.kayenta.stackdriver.orca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.metrics.SynchronousQueryProcessor;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.stackdriver.canary.StackdriverCanaryScope;
import com.netflix.spinnaker.orca.api.pipeline.RetryableTask;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StackdriverFetchTask implements RetryableTask {

  private final ObjectMapper kayentaObjectMapper;
  private final AccountCredentialsRepository accountCredentialsRepository;
  private final SynchronousQueryProcessor synchronousQueryProcessor;

  @Autowired
  public StackdriverFetchTask(
      ObjectMapper kayentaObjectMapper,
      AccountCredentialsRepository accountCredentialsRepository,
      SynchronousQueryProcessor synchronousQueryProcessor) {
    this.kayentaObjectMapper = kayentaObjectMapper;
    this.accountCredentialsRepository = accountCredentialsRepository;
    this.synchronousQueryProcessor = synchronousQueryProcessor;
  }

  @Override
  public long getBackoffPeriod() {
    // TODO(duftler): Externalize this configuration.
    return Duration.ofSeconds(2).toMillis();
  }

  @Override
  public long getTimeout() {
    // TODO(duftler): Externalize this configuration.
    return Duration.ofMinutes(2).toMillis();
  }

  @Nonnull
  @Override
  public TaskResult execute(@Nonnull StageExecution stage) {
    Map<String, Object> context = stage.getContext();
    String metricsAccountName = (String) context.get("metricsAccountName");
    String storageAccountName = (String) context.get("storageAccountName");
    Map<String, Object> canaryConfigMap = (Map<String, Object>) context.get("canaryConfig");
    CanaryConfig canaryConfig =
        kayentaObjectMapper.convertValue(canaryConfigMap, CanaryConfig.class);
    int metricIndex = (Integer) stage.getContext().get("metricIndex");
    StackdriverCanaryScope stackdriverCanaryScope;
    try {
      stackdriverCanaryScope =
          kayentaObjectMapper.readValue(
              (String) stage.getContext().get("canaryScope"), StackdriverCanaryScope.class);
    } catch (IOException e) {
      log.warn("Unable to parse JSON scope", e);
      throw new RuntimeException(e);
    }
    String resolvedMetricsAccountName =
        accountCredentialsRepository
            .getRequiredOneBy(metricsAccountName, AccountCredentials.Type.METRICS_STORE)
            .getName();
    String resolvedStorageAccountName =
        accountCredentialsRepository
            .getRequiredOneBy(storageAccountName, AccountCredentials.Type.OBJECT_STORE)
            .getName();

    return synchronousQueryProcessor.executeQueryAndProduceTaskResult(
        resolvedMetricsAccountName,
        resolvedStorageAccountName,
        canaryConfig,
        metricIndex,
        stackdriverCanaryScope);
  }
}
