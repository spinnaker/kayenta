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

package com.netflix.kayenta.opentsdb.metrics;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.OpentsdbCanaryMetricSetQueryConfig;
import com.netflix.kayenta.opentsdb.model.OpentsdbResults;
import com.netflix.kayenta.opentsdb.security.OpentsdbCredentials;
import com.netflix.kayenta.opentsdb.security.OpentsdbNamedAccountCredentials;
import com.netflix.kayenta.opentsdb.service.OpentsdbRemoteService;
import com.netflix.kayenta.opentsdb.service.OpentsdbTimeSeries;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.security.CredentialsHelper;
import com.netflix.spectator.api.Registry;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.netflix.kayenta.opentsdb.canary.OpentsdbCanaryScope;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class OpentsdbMetricsService implements MetricsService {
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
    return "opentsdb";
  }

  @Override
  public boolean servicesAccount(String accountName) {
    return accountNames.contains(accountName);
  }

  @Override
  public String buildQuery(String metricsAccountName, CanaryConfig canaryConfig, CanaryMetricConfig canaryMetricConfig,
                           CanaryScope canaryScope)  {

    OpentsdbCanaryScope opentsdbCanaryScope = (OpentsdbCanaryScope) canaryScope;

    OpentsdbCanaryMetricSetQueryConfig queryConfig =
            (OpentsdbCanaryMetricSetQueryConfig) canaryMetricConfig.getQuery();

    // Example for a query produced by this class:
    // SELECT count(*) FROM Transaction TIMESERIES MAX SINCE 1540382125 UNTIL 1540392125
    // WHERE appName LIKE 'PROD - Service' AND httpResponseCode >= '500'

    // we expect the full select statement to be in the config
    StringBuilder query = new StringBuilder("sum:");
    query.append(queryConfig.getM());
    query.append("{");

    for (Map.Entry<String, String> extendedParam : opentsdbCanaryScope.getExtendedScopeParams().entrySet()) {
      if (extendedParam.getKey().startsWith("_")) {
        continue;
      }
      query.append(extendedParam.getKey());
      query.append("=");
      query.append(extendedParam.getValue());
      query.append(",");
    }

    query.append(opentsdbCanaryScope.getScopeKey());
    query.append("=");
    query.append(opentsdbCanaryScope.getScope());
    return query.toString();
  }
  @Override
  public List<MetricSet> queryMetrics(String accountName, CanaryConfig canaryConfig, CanaryMetricConfig canaryMetricConfig, CanaryScope canaryScope) {
    OpentsdbNamedAccountCredentials accountCredentials = (OpentsdbNamedAccountCredentials) accountCredentialsRepository
            .getOne(accountName)
            .orElseThrow(() -> new IllegalArgumentException("Unable to resolve account " + accountName + "."));

    OpentsdbCredentials credentials = accountCredentials.getCredentials();
    OpentsdbRemoteService remoteService = accountCredentials.getOpentsdbRemoteService();

    if (StringUtils.isEmpty(canaryScope.getStart())) {
      throw new IllegalArgumentException("Start time is required.");
    }

    if (StringUtils.isEmpty(canaryScope.getEnd())) {
      throw new IllegalArgumentException("End time is required.");
    }

    String query = buildQuery(accountName,
            canaryConfig,
            canaryMetricConfig,
            canaryScope);

    OpentsdbTimeSeries timeSeries = remoteService.fetch(
            query,
            canaryScope.getStart().getEpochSecond(),
            canaryScope.getEnd().getEpochSecond(),
            canaryScope.getStep()
    );

    List<MetricSet> ret = new ArrayList<MetricSet>();

    for (OpentsdbTimeSeries.OpentsdbSeriesEntry series : timeSeries.getSeries()) {
      ret.add(
              MetricSet.builder()
                      .name(canaryMetricConfig.getName())
                      .startTimeMillis(series.getStart())
                      .startTimeIso(Instant.ofEpochMilli(series.getStart()).toString())
                      .endTimeMillis(series.getEnd())
                      .endTimeIso(Instant.ofEpochMilli(series.getEnd()).toString())
                      .stepMillis(series.getInterval() * 1000)
                      .values(series.getDataPoints().collect(Collectors.toList()))
                      .attribute("query", query)
                      .build()
      );
    }

    return ret;
  }
}
