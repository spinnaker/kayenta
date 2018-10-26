/*
 * Copyright 2018 Adobe
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

package com.netflix.kayenta.newrelic.metrics;

import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.NewrelicCanaryMetricSetQueryConfig;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.newrelic.security.NewrelicCredentials;
import com.netflix.kayenta.newrelic.security.NewrelicNamedAccountCredentials;
import com.netflix.kayenta.newrelic.service.NewrelicRemoteService;
import com.netflix.kayenta.newrelic.service.NewrelicTimeSeries;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.spectator.api.Registry;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

@Builder
@Slf4j
public class NewrelicMetricsService implements MetricsService {

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
    return "newrelic";
  }

  @Override
  public boolean servicesAccount(String accountName) {
    return accountNames.contains(accountName);
  }

  @Override
  public List<MetricSet> queryMetrics(String accountName,
      CanaryConfig canaryConfig,
      CanaryMetricConfig canaryMetricConfig, CanaryScope canaryScope)
      throws IOException {
    NewrelicNamedAccountCredentials accountCredentials =
        (NewrelicNamedAccountCredentials) accountCredentialsRepository
            .getOne(accountName)
            .orElseThrow(() -> new IllegalArgumentException(
                "Unable to resolve account " + accountName + "."));

    NewrelicCredentials credentials = accountCredentials.getCredentials();
    NewrelicRemoteService remoteService = accountCredentials.getNewrelicRemoteService();
    NewrelicCanaryMetricSetQueryConfig queryConfig =
        (NewrelicCanaryMetricSetQueryConfig) canaryMetricConfig.getQuery();

    // Example for a query produced by this class:
    // SELECT count(*) FROM Transaction TIMESERIES MAX SINCE 1540382125 UNTIL 1540392125
    // WHERE appName LIKE 'PROD - Service' AND httpResponseCode >= '500'
    StringBuilder query = new StringBuilder("SELECT ");

    query.append(queryConfig.getSelect());
    query.append(" FROM Transaction TIMESERIES MAX ");

    query.append(" SINCE ");
    query.append(canaryScope.getStart().getEpochSecond());
    query.append(" UNTIL ");
    query.append(canaryScope.getEnd().getEpochSecond());
    query.append(" WHERE ");
    if (!StringUtils.isEmpty(queryConfig.getQ())) {
      query.append(queryConfig.getQ());
      query.append(" AND ");
    }
    query.append(canaryScope.getScope());

    NewrelicTimeSeries timeSeries = remoteService.getTimeSeries(
        credentials.getApiKey(),
        credentials.getApplicationKey(),
        query.toString()
    );

    Instant begin =
        Instant.ofEpochMilli(timeSeries.getMetadata().getBeginTimeMillis());
    Instant end =
        Instant.ofEpochMilli(timeSeries.getMetadata().getEndTimeMillis());

    return Arrays.asList(
        MetricSet.builder()
            .name(canaryMetricConfig.getName())
            .startTimeMillis(begin.toEpochMilli())
            .startTimeIso(begin.toString())
            .endTimeMillis(end.toEpochMilli())
            .endTimeIso(end.toString())
            .values(timeSeries.getDataPoints().collect(Collectors.toList()))
            .build()
    );
  }
}
