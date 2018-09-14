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

package com.netflix.kayenta.datadog.metrics;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.DatadogCanaryMetricSetQueryConfig;
import com.netflix.kayenta.datadog.security.DatadogCredentials;
import com.netflix.kayenta.datadog.security.DatadogNamedAccountCredentials;
import com.netflix.kayenta.datadog.service.DatadogRemoteService;
import com.netflix.kayenta.datadog.service.DatadogTimeSeries;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.spectator.api.Registry;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class DatadogMetricsService implements MetricsService {
  @NotNull
  @Singular
  @Getter
  private List<String> accountNames;

  @Autowired
  private final AccountCredentialsRepository accountCredentialsRepository;

  @Autowired
  private final Registry registry;

  @Override
  public String getType() {
    return "datadog";
  }

  @Override
  public boolean servicesAccount(String accountName) {
    return accountNames.contains(accountName);
  }

  @Override
  public List<MetricSet> queryMetrics(String accountName, CanaryConfig canaryConfig, CanaryMetricConfig canaryMetricConfig, CanaryScope canaryScope) {
    DatadogNamedAccountCredentials accountCredentials = (DatadogNamedAccountCredentials)accountCredentialsRepository
      .getOne(accountName)
      .orElseThrow(() -> new IllegalArgumentException("Unable to resolve account " + accountName + "."));

    DatadogCredentials credentials = accountCredentials.getCredentials();
    DatadogRemoteService remoteService = accountCredentials.getDatadogRemoteService();
    DatadogCanaryMetricSetQueryConfig queryConfig = (DatadogCanaryMetricSetQueryConfig)canaryMetricConfig.getQuery();

    DatadogTimeSeries timeSeries = remoteService.getTimeSeries(
      credentials.getApiKey(),
      credentials.getApplicationKey(),
      (int)canaryScope.getStart().getEpochSecond(),
      (int)canaryScope.getEnd().getEpochSecond(),
      queryConfig.getMetricName() + "{" + canaryScope.getScope() + "}"
    );

    List<MetricSet> ret = new ArrayList<MetricSet>();

    for (DatadogTimeSeries.DatadogSeriesEntry series : timeSeries.getSeries()) {
      ret.add(
        MetricSet.builder()
          .name(canaryMetricConfig.getName())
          .startTimeMillis(series.getStart())
          .startTimeIso(Instant.ofEpochMilli(series.getStart()).toString())
          .endTimeMillis(series.getEnd())
          .endTimeIso(Instant.ofEpochMilli(series.getEnd()).toString())
          .stepMillis(series.getInterval() * 1000)
          .values(series.getDataPoints().collect(Collectors.toList()))
          .build()
      );
    }

    return ret;
  }
}
