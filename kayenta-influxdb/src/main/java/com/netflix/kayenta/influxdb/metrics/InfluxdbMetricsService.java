/*
 * Copyright 2018 Joseph Motha
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.InfluxdbCanaryMetricSetQueryConfig;
import com.netflix.kayenta.influxdb.model.InfluxdbResult;
import com.netflix.kayenta.influxdb.security.InfluxdbNamedAccountCredentials;
import com.netflix.kayenta.influxdb.service.InfluxdbRemoteService;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricSet.MetricSetBuilder;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	  
    InfluxdbNamedAccountCredentials accountCredentials = (InfluxdbNamedAccountCredentials)accountCredentialsRepository
      .getOne(accountName)
      .orElseThrow(() -> new IllegalArgumentException("Unable to resolve account " + accountName + "."));

    InfluxdbRemoteService remoteService = accountCredentials.getInfluxdbRemoteService();
    InfluxdbCanaryMetricSetQueryConfig queryConfig = (InfluxdbCanaryMetricSetQueryConfig)canaryMetricConfig.getQuery();

    String query = queryBuilder.build(queryConfig, canaryScope);
    log.debug("query={}", query);
    
    String metricSetName = canaryMetricConfig.getName();
    List<InfluxdbResult> influxdbResults = queryInfluxdb(remoteService, metricSetName, query);
    
    return buildMetricSets(metricSetName, influxdbResults);
  }

  private List<InfluxdbResult> queryInfluxdb(InfluxdbRemoteService remoteService, String metricSetName, String query) {
    long startTime = registry.clock().monotonicTime();
    List<InfluxdbResult> influxdbResults; 
    
    try {
      influxdbResults = remoteService.query(metricSetName, query);
    } finally {
      long endTime = registry.clock().monotonicTime();
      Id influxdbFetchTimerId = registry.createId("influxdb.fetchTime");
      registry.timer(influxdbFetchTimerId).record(endTime - startTime, TimeUnit.NANOSECONDS);
    }
    return influxdbResults;
  }
  
  private List<MetricSet> buildMetricSets(String metricSetName, List<InfluxdbResult> influxdbResults) {
    List<MetricSet> metricSets = new ArrayList<MetricSet>();
    if (influxdbResults != null) {
      for (InfluxdbResult influxdbResult : influxdbResults) {
        MetricSetBuilder metricSetBuilder = MetricSet.builder()
            .name(metricSetName)
            .startTimeMillis(influxdbResult.getStartTimeMillis())
            .startTimeIso(Instant.ofEpochMilli(influxdbResult.getStartTimeMillis()).toString())
            .stepMillis(influxdbResult.getStepMillis())
            .values(influxdbResult.getValues())
            .tag("field", influxdbResult.getId());
        
        Map<String, String> tags = influxdbResult.getTags();
        if (tags != null) {
          metricSetBuilder.tags(tags);
        }
        
        metricSets.add(metricSetBuilder.build());
      }
    }
    return metricSets;
  }

}
