/*
 * Copyright 2018 Armory, Inc.
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

package com.netflix.kayenta.influxdb.metrics;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.InfluxdbCanaryMetricSetQueryConfig;
import com.netflix.kayenta.influxdb.canary.InfluxdbCanaryScope;
import com.netflix.kayenta.influxdb.model.InfluxdbResult;
import com.netflix.kayenta.influxdb.security.InfluxdbCredentials;
import com.netflix.kayenta.influxdb.security.InfluxdbNamedAccountCredentials;
import com.netflix.kayenta.influxdb.service.InfluxdbRemoteService;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.spectator.api.Registry;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Builder
public class InfluxdbMetricsService implements MetricsService {
  @NotNull
  @Singular
  @Getter
  private List<String> accountNames;

  @Autowired
  private final AccountCredentialsRepository accountCredentialsRepository;

  @Autowired
  private final Registry registry;
  
  @Autowired
  private final InfluxdbQueryBuilder queryBuilder;

  @Override
  public String getType() {
    return "influxdb";
  }

  @Override
  public boolean servicesAccount(String accountName) {
    return accountNames.contains(accountName);
  }

  @Override
  public List<MetricSet> queryMetrics(String accountName, CanaryConfig canaryConfig, CanaryMetricConfig canaryMetricConfig, CanaryScope canaryScope) throws IOException {
	  
    if (!(canaryScope instanceof InfluxdbCanaryScope)) {
      throw new IllegalArgumentException("Canary scope not instance of InfluxdbCanaryScope: " + canaryScope +
                                         ". One common cause is having multiple METRICS_STORE accounts configured but " +
                                         "neglecting to explicitly specify which account to use for a given request.");
    }
    InfluxdbNamedAccountCredentials accountCredentials = (InfluxdbNamedAccountCredentials)accountCredentialsRepository
      .getOne(accountName)
      .orElseThrow(() -> new IllegalArgumentException("Unable to resolve account " + accountName + "."));

    InfluxdbCredentials credentials = accountCredentials.getCredentials();
    InfluxdbRemoteService remoteService = accountCredentials.getInfluxdbRemoteService();
    InfluxdbCanaryMetricSetQueryConfig queryConfig = (InfluxdbCanaryMetricSetQueryConfig)canaryMetricConfig.getQuery();

    //TODO(joerajeev): do I need to support resource type
    List<InfluxdbResult> influxdbResults = remoteService.getTimeSeries(
      credentials.getDbName(),
      queryBuilder.build(queryConfig.getMetricName(), queryConfig.getFields(), canaryScope)
    );
    
    //TODO(joerajeev): Log retrieval time to registry?

    List<MetricSet> ret = new ArrayList<MetricSet>();

    for (InfluxdbResult influxdbResult : influxdbResults) {
      ret.add(
        MetricSet.builder()
          .name(canaryMetricConfig.getName())
          .startTimeMillis(influxdbResult.getStartTimeMillis())
          .startTimeIso(Instant.ofEpochMilli(influxdbResult.getStartTimeMillis()).toString())
          .stepMillis(influxdbResult.getStepMillis())
          .values(influxdbResult.getValues())
          .build()
      );
    }

    return ret;
  }

}
